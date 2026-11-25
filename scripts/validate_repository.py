#!/usr/bin/env python3
"""Offline repository/documentation checks; no Android SDK or third-party modules required."""

from __future__ import annotations

import re
import sys
import tomllib
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]


def anchors(text: str) -> set[str]:
    found = set(re.findall(r'<a\s+(?:id|name)="([^"]+)"', text))
    used: dict[str, int] = {}
    for heading in re.findall(r"^#{1,6}\s+(.+)$", text, re.MULTILINE):
        title = re.sub(r"\[([^]]+)\]\([^)]*\)", r"\1", heading).strip().lower()
        title = "".join(c for c in title if c in " -_" or unicodedata.category(c)[0] in "LN")
        slug = title.replace(" ", "-")
        count = used.get(slug, 0)
        used[slug] = count + 1
        found.add(f"{slug}-{count}" if count else slug)
    return found


def validate() -> list[str]:
    errors: list[str] = []
    markdown = [ROOT / "README.md", ROOT / "CONTRIBUTING.md", *sorted((ROOT / "docs").rglob("*.md"))]
    for path in markdown:
        if not path.is_file():
            errors.append(f"Missing document: {path.relative_to(ROOT)}")
            continue
        text = path.read_text(encoding="utf-8")
        outside_code = re.sub(r"```.*?```", "", text, flags=re.DOTALL)
        for href in re.findall(r"\[[^]\n]+\]\(([^\s)]+)\)", outside_code):
            parts = urlsplit(href.strip("<>"))
            if parts.scheme or parts.netloc:
                continue
            target = (path.parent / unquote(parts.path)).resolve() if parts.path else path
            if not target.is_relative_to(ROOT):
                errors.append(f"Link escapes repository: {path.relative_to(ROOT)} -> {href}")
            elif not target.exists():
                errors.append(f"Broken link: {path.relative_to(ROOT)} -> {href}")
            elif parts.fragment and target.suffix == ".md":
                if unquote(parts.fragment) not in anchors(target.read_text(encoding="utf-8")):
                    errors.append(f"Missing anchor: {path.relative_to(ROOT)} -> {href}")

    catalog = ROOT / "docs/research/issue-catalog.md"
    if catalog.exists():
        ids = re.findall(r'<a id="issue-(\d{2})"></a>', catalog.read_text(encoding="utf-8"))
        if ids != [f"{i:02d}" for i in range(1, 34)]:
            errors.append("Issue catalog must contain ordered unique anchors issue-01 through issue-33")

    for path in [*ROOT.glob("app/src/**/*.xml"), *ROOT.glob("core/data/src/**/*.xml")]:
        try:
            ET.parse(path)
        except ET.ParseError as exc:
            errors.append(f"Invalid XML: {path.relative_to(ROOT)}: {exc}")
    try:
        with (ROOT / "gradle/libs.versions.toml").open("rb") as file:
            tomllib.load(file)
    except (OSError, tomllib.TOMLDecodeError) as exc:
        errors.append(f"Invalid version catalog: {exc}")

    for module in ["app", "core/model", "core/domain", "core/data"]:
        if not (ROOT / module / "build.gradle.kts").is_file():
            errors.append(f"Missing Gradle module: {module}")
    for name in ["gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"]:
        if not (ROOT / name).is_file():
            errors.append(f"Missing Gradle Wrapper file: {name}")
    return errors


if __name__ == "__main__":
    problems = validate()
    if problems:
        print("Repository validation failed:", file=sys.stderr)
        for problem in problems:
            print(f"- {problem}", file=sys.stderr)
        sys.exit(1)
    print("Repository validation passed: Markdown links/anchors, 33 issue IDs, XML, TOML, modules, wrapper files.")
