"""Offline contract/failure tests; does not contact OSV or claim a dependency scan."""

import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("runtime_audit", Path(__file__).with_name("audit_runtime_dependencies.py"))
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class RuntimeAuditTests(unittest.TestCase):
    def test_inventory_excludes_local_projects_and_preserves_artifact_fingerprints(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "inventory.tsv"
            path.write_text("coordinate\tfile\tsha256\nproject :core:data\tdata.aar\t" + "a" * 64 +
                            "\ncom.example:library:1.0\tlibrary-1.0.aar\t" + "b" * 64 + "\n")
            packages, excluded, count, _ = audit.read_inventory(path)
            self.assertEqual(2, count)
            self.assertEqual(["com.example:library:1.0"], list(packages))
            self.assertEqual("b" * 64, packages["com.example:library:1.0"][0]["sha256"])
            self.assertEqual(1, len(excluded))

    def test_pagination_keeps_package_identity_and_deduplicates_ids(self):
        calls = []
        def send(queries):
            calls.append(queries)
            if len(calls) == 1:
                return {"results": [{"vulns": [{"id": "GHSA-example"}], "next_page_token": "next"}, {}]}
            self.assertEqual("g:a", queries[0]["package"]["name"])
            return {"results": [{"vulns": [{"id": "GHSA-example"}, {"id": "CVE-example"}]}]}
        records = audit.query_packages(["g:a:1", "g:b:2"], send)
        self.assertEqual(["CVE-example", "GHSA-example"], [v["id"] for v in records[0]["vulnerabilities"]])
        self.assertEqual([], records[1]["vulnerabilities"])

    def test_incomplete_or_invalid_api_response_fails_closed(self):
        for response in ({"results": []}, {"results": [{"error": "failure"}]},
                         {"results": [{"vulns": [{}]}]}):
            with self.subTest(response=response), self.assertRaises(audit.AuditError):
                audit.query_packages(["g:a:1"], lambda _: response)

    def test_repeated_pagination_token_is_an_error(self):
        with self.assertRaises(audit.AuditError):
            audit.query_packages(["g:a:1"], lambda _: {"results": [{"next_page_token": "same"}]})

    def test_unavailable_api_writes_error_report_and_returns_two(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "inventory.tsv"
            output = Path(directory) / "report.json"
            source.write_text("coordinate\tfile\tsha256\ng:a:1\ta.jar\t" + "a" * 64 + "\n")
            with patch.object(audit, "query_packages", side_effect=audit.AuditError("OSV unavailable")):
                result = audit.main([str(source), "--output", str(output)])
            import json
            self.assertEqual(2, result)
            self.assertEqual("error", json.loads(output.read_text())["status"])


if __name__ == "__main__":
    unittest.main()
