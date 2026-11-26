#!/usr/bin/env python3
"""Query official OSV for public Maven coordinates in the runtime artifact inventory.

Exit 0: no mapped OSV findings; 1: findings; 2: input/API/incomplete-scan failure.
Only public package coordinates/versions are sent, never artifact bytes or local paths.
API contract: https://google.github.io/osv.dev/post-v1-querybatch/
"""

import argparse
import csv
from datetime import datetime, timezone
import hashlib
import io
import json
from pathlib import Path
import re
import sys
from urllib import error, request

API_URL = "https://api.osv.dev/v1/querybatch"
BATCH_SIZE = 100
MAX_RESPONSE_BYTES = 8 * 1024 * 1024
COORDINATE = re.compile(r"([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([A-Za-z0-9_.+\-]+)")
SHA256 = re.compile(r"[a-f0-9]{64}")


class AuditError(Exception):
    """An incomplete scan must never be reported as no findings."""


def read_inventory(path):
    raw = path.read_bytes()
    try:
        rows = csv.DictReader(io.StringIO(raw.decode("utf-8")), delimiter="\t")
        if rows.fieldnames != ["coordinate", "file", "sha256"]:
            raise AuditError("Expected TSV columns: coordinate, file, sha256")
        packages = {}
        excluded = []
        count = 0
        for number, row in enumerate(rows, start=2):
            count += 1
            if None in row or any(row.get(key) is None for key in rows.fieldnames):
                raise AuditError(f"Malformed inventory row {number}")
            coordinate, filename, digest = (row[key] for key in rows.fieldnames)
            if not filename or "/" in filename or "\\" in filename or not SHA256.fullmatch(digest):
                raise AuditError(f"Invalid artifact metadata at row {number}")
            if coordinate.startswith("project "):
                excluded.append({"coordinate": coordinate, "reason": "Local project artifact; not queried"})
                continue
            if not COORDINATE.fullmatch(coordinate):
                raise AuditError(f"Unsupported/non-public coordinate format at row {number}")
            packages.setdefault(coordinate, []).append({"file": filename, "sha256": digest})
        if not packages:
            raise AuditError("Inventory contains no public Maven packages to audit")
    except (UnicodeError, csv.Error) as exc:
        raise AuditError("Inventory is not valid UTF-8 TSV") from exc
    return packages, excluded, count, hashlib.sha256(raw).hexdigest()


def post_batch(queries):
    payload = json.dumps({"queries": queries}).encode("utf-8")
    req = request.Request(API_URL, data=payload, method="POST", headers={
        "Content-Type": "application/json", "Accept": "application/json",
        "User-Agent": "TerminalWorkspace-RuntimeAudit/1",
    })
    try:
        with request.urlopen(req, timeout=30) as response:
            raw = response.read(MAX_RESPONSE_BYTES + 1)
            if len(raw) > MAX_RESPONSE_BYTES:
                raise AuditError("OSV response exceeded the audit's bounded response size")
        result = json.loads(raw)
    except error.HTTPError as exc:
        raise AuditError(f"OSV API returned HTTP {exc.code}; scan incomplete") from exc
    except (error.URLError, TimeoutError, OSError) as exc:
        raise AuditError("OSV API was unavailable; scan incomplete") from exc
    except (ValueError, UnicodeError) as exc:
        raise AuditError("OSV returned invalid JSON; scan incomplete") from exc
    if not isinstance(result, dict):
        raise AuditError("OSV returned an unexpected response shape")
    return result


def query_packages(coordinates, send=post_batch):
    records = []
    for start in range(0, len(coordinates), BATCH_SIZE):
        batch = coordinates[start:start + BATCH_SIZE]
        pending = []
        found = {coordinate: {} for coordinate in batch}
        seen_tokens = {coordinate: set() for coordinate in batch}
        for coordinate in batch:
            group, artifact, version = coordinate.split(":")
            pending.append((coordinate, {"package": {"ecosystem": "Maven", "name": f"{group}:{artifact}"},
                                         "version": version}))
        pages = 0
        while pending:
            pages += 1
            if pages > 100:
                raise AuditError("OSV pagination did not finish within the audit limit")
            response = send([query for _, query in pending])
            results = response.get("results")
            if not isinstance(results, list) or len(results) != len(pending):
                raise AuditError("OSV result count did not match the submitted queries")
            next_queries = []
            for (coordinate, query), result in zip(pending, results):
                if not isinstance(result, dict) or "error" in result:
                    raise AuditError("OSV reported a package-query failure")
                vulns = result.get("vulns", [])
                if not isinstance(vulns, list):
                    raise AuditError("OSV returned an invalid vulnerability list")
                for vuln in vulns:
                    if not isinstance(vuln, dict) or not isinstance(vuln.get("id"), str) or not vuln["id"]:
                        raise AuditError("OSV returned a vulnerability without an identifier")
                    found[coordinate][vuln["id"]] = {key: vuln[key] for key in ("id", "modified") if key in vuln}
                token = result.get("next_page_token")
                if token:
                    if not isinstance(token, str) or token in seen_tokens[coordinate]:
                        raise AuditError("OSV returned an invalid/repeated pagination token")
                    seen_tokens[coordinate].add(token)
                    next_queries.append((coordinate, {**query, "page_token": token}))
            pending = next_queries
        records.extend({"coordinate": coordinate, "query_status": "complete",
                        "vulnerabilities": sorted(found[coordinate].values(), key=lambda item: item["id"])}
                       for coordinate in batch)
    return records


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inventory", type=Path)
    parser.add_argument("--output", type=Path, default=Path("app/build/reports/runtime-osv-report.json"))
    args = parser.parse_args(argv)
    report = {
        "schema_version": 1, "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "api": API_URL, "status": "error", "inventory_file": args.inventory.name,
        "scope": "Public Maven coordinates in resolved debug runtime artifacts only",
        "limitations": [
            "No mapped OSV findings is not proof that the application or dependencies are secure.",
            "Vendored/native code, unmapped advisories, build/test tools and app integration need separate review.",
            "Only package coordinates/versions are queried; artifact hashes do not establish publisher authenticity.",
        ],
    }
    exit_code = 2
    try:
        packages, excluded, count, digest = read_inventory(args.inventory)
        report.update({"inventory_sha256": digest, "artifact_count": count,
                       "queried_package_count": len(packages), "excluded_components": excluded})
        records = query_packages(sorted(packages))
        for record in records:
            record["artifacts"] = packages[record["coordinate"]]
        findings = sum(len(record["vulnerabilities"]) for record in records)
        report.update({"packages": records, "finding_count": findings,
                       "status": "findings" if findings else "no_known_findings"})
        exit_code = 1 if findings else 0
    except (AuditError, OSError) as exc:
        report["error"] = str(exc) if isinstance(exc, AuditError) else "Inventory/report I/O failed; scan incomplete"
    try:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    except OSError:
        print("Could not write OSV audit report; scan incomplete", file=sys.stderr)
        return 2
    print(f"Runtime OSV audit: {report['status']} (exit {exit_code}). Report: {args.output}")
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
