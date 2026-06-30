#!/usr/bin/env python3
"""Parse Maven Surefire XML reports and print/generate a test results dashboard."""

from __future__ import annotations

import html
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


@dataclass
class TestResult:
    module: str
    test_type: str
    class_name: str
    method_name: str
    description: str
    status: str
    duration_sec: float
    message: str


def project_root() -> Path:
    return Path(__file__).resolve().parent.parent


def find_surefire_reports(root: Path) -> list[Path]:
    return sorted(root.glob("**/target/surefire-reports/TEST-*.xml"))


def short_class_name(classname: str) -> str:
    return classname.rsplit(".", 1)[-1]


def humanize_method_name(method_name: str) -> str:
    if not method_name:
        return method_name
    spaced = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", method_name)
    spaced = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1 \2", spaced)
    return spaced.replace("_", " ").strip().capitalize()


def testcase_status(case: ET.Element) -> tuple[str, str]:
    failure = case.find("failure")
    if failure is not None:
        return "FAIL", (failure.text or failure.get("message") or "").strip()
    error = case.find("error")
    if error is not None:
        return "ERROR", (error.text or error.get("message") or "").strip()
    skipped = case.find("skipped")
    if skipped is not None:
        return "SKIP", (skipped.get("message") or "").strip()
    return "PASS", ""


def parse_reports(root: Path) -> list[TestResult]:
    results: list[TestResult] = []
    for report in find_surefire_reports(root):
        module = report.parts[-4] if len(report.parts) >= 4 else "unknown"
        try:
            tree = ET.parse(report)
        except ET.ParseError as exc:
            print(f"Warning: could not parse {report}: {exc}", file=sys.stderr)
            continue

        for case in tree.getroot().findall("testcase"):
            classname = case.get("classname", "")
            method_name = case.get("name", "")
            if method_name in {"setUp", "tearDown", "seedAccount"}:
                continue

            test_type = "Integration" if ".integration." in classname else "Unit"
            status, message = testcase_status(case)
            display_name = case.get("display-name") or case.get("displayName")
            description = display_name.strip() if display_name else humanize_method_name(method_name)

            try:
                duration = float(case.get("time", "0") or 0)
            except ValueError:
                duration = 0.0

            results.append(
                TestResult(
                    module=module,
                    test_type=test_type,
                    class_name=short_class_name(classname),
                    method_name=method_name,
                    description=description,
                    status=status,
                    duration_sec=duration,
                    message=message,
                )
            )
    return results


def summarize(results: list[TestResult]) -> dict[str, dict[str, int]]:
    summary: dict[str, dict[str, int]] = {
        "Unit": {"total": 0, "passed": 0, "failed": 0},
        "Integration": {"total": 0, "passed": 0, "failed": 0},
        "All": {"total": 0, "passed": 0, "failed": 0},
    }
    for result in results:
        for key in (result.test_type, "All"):
            summary[key]["total"] += 1
            if result.status == "PASS":
                summary[key]["passed"] += 1
            else:
                summary[key]["failed"] += 1
    return summary


def overall_status(summary: dict[str, dict[str, int]]) -> str:
    return "SUCCESS" if summary["All"]["failed"] == 0 else "FAILURE"


def print_terminal_dashboard(results: list[TestResult], summary: dict[str, dict[str, int]]) -> None:
    width = 78
    line = "=" * width
    thin = "-" * width
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    status = overall_status(summary)

    print(line)
    print("TEST RESULTS DASHBOARD".center(width))
    print(line)
    print(f"Run: {now}    Overall: {status}")
    print()
    print("Summary")
    for test_type in ("Unit", "Integration", "All"):
        stats = summary[test_type]
        print(
            f"  {test_type:<14}: {stats['passed']:>2} passed, "
            f"{stats['failed']:>2} failed, {stats['total']:>2} total"
        )
    print()

    for test_type in ("Unit", "Integration"):
        type_results = [r for r in results if r.test_type == test_type]
        if not type_results:
            continue
        print(f"{test_type} scenarios")
        print(thin)
        for result in type_results:
            print(
                f"  {result.status:<5} {result.class_name}.{result.method_name} "
                f"({result.duration_sec:.2f}s)"
            )
            print(f"         {result.description}")
            if result.message:
                first_line = result.message.splitlines()[0][:100]
                print(f"         -> {first_line}")
        print()

    print("Reports")
    print(f"  HTML dashboard : target/test-dashboard.html")
    jacoco = project_root() / "transaction-service" / "target" / "site" / "jacoco" / "index.html"
    if jacoco.exists():
        print(f"  Coverage       : transaction-service/target/site/jacoco/index.html")
    print(line)


def status_badge_class(status: str) -> str:
    return {
        "PASS": "pass",
        "FAIL": "fail",
        "ERROR": "error",
        "SKIP": "skip",
    }.get(status, "unknown")


def generate_html(results: list[TestResult], summary: dict[str, dict[str, int]], output: Path) -> None:
    status = overall_status(summary)
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    jacoco_rel = "../transaction-service/target/site/jacoco/index.html"
    jacoco_exists = (project_root() / "transaction-service" / "target" / "site" / "jacoco" / "index.html").exists()

    def render_rows(test_type: str) -> str:
        rows = []
        for index, result in enumerate([r for r in results if r.test_type == test_type], start=1):
            message_html = (
                f'<div class="message">{html.escape(result.message)}</div>' if result.message else ""
            )
            rows.append(
                f"""
                <tr>
                  <td>{index}</td>
                  <td><code>{html.escape(result.class_name)}.{html.escape(result.method_name)}</code></td>
                  <td>{html.escape(result.description)}</td>
                  <td><span class="badge {status_badge_class(result.status)}">{result.status}</span></td>
                  <td>{result.duration_sec:.2f}s</td>
                </tr>
                {f'<tr class="detail"><td colspan="5">{message_html}</td></tr>' if message_html else ''}
                """
            )
        return "\n".join(rows) if rows else '<tr><td colspan="5">No tests found</td></tr>'

    doc = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Test Results Dashboard</title>
  <style>
    :root {{
      --bg: #0f172a;
      --card: #1e293b;
      --text: #e2e8f0;
      --muted: #94a3b8;
      --pass: #22c55e;
      --fail: #ef4444;
      --error: #f97316;
      --skip: #eab308;
      --accent: #38bdf8;
      --border: #334155;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: Inter,Segoe UI,Roboto,Arial,sans-serif;
      background: linear-gradient(180deg, #0b1220, var(--bg));
      color: var(--text);
      padding: 24px;
    }}
    .container {{ max-width: 1200px; margin: 0 auto; }}
    h1 {{ margin: 0 0 8px; font-size: 1.8rem; }}
    .meta {{ color: var(--muted); margin-bottom: 24px; }}
    .cards {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-bottom: 28px;
    }}
    .card {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
    }}
    .card .label {{ color: var(--muted); font-size: 0.85rem; }}
    .card .value {{ font-size: 1.6rem; font-weight: 700; margin-top: 6px; }}
    .overall {{ color: {'var(--pass)' if status == 'SUCCESS' else 'var(--fail)'}; }}
    section {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 24px;
    }}
    h2 {{ margin-top: 0; font-size: 1.2rem; }}
    table {{ width: 100%; border-collapse: collapse; }}
    th, td {{
      padding: 10px 12px;
      border-bottom: 1px solid var(--border);
      text-align: left;
      vertical-align: top;
    }}
    th {{ color: var(--muted); font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.04em; }}
    tr.detail td {{ padding-top: 0; color: #fca5a5; font-size: 0.85rem; }}
    .badge {{
      display: inline-block;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 700;
    }}
    .badge.pass {{ background: rgba(34,197,94,.15); color: var(--pass); }}
    .badge.fail {{ background: rgba(239,68,68,.15); color: var(--fail); }}
    .badge.error {{ background: rgba(249,115,22,.15); color: var(--error); }}
    .badge.skip {{ background: rgba(234,179,8,.15); color: var(--skip); }}
    .links a {{
      color: var(--accent);
      margin-right: 16px;
      text-decoration: none;
    }}
    .links a:hover {{ text-decoration: underline; }}
    code {{ color: #cbd5e1; }}
    .message {{ white-space: pre-wrap; }}
  </style>
</head>
<body>
  <div class="container">
    <h1>Test Results Dashboard</h1>
    <div class="meta">Generated: {html.escape(now)}</div>

    <div class="cards">
      <div class="card"><div class="label">Overall</div><div class="value overall">{status}</div></div>
      <div class="card"><div class="label">Total Passed</div><div class="value">{summary['All']['passed']}</div></div>
      <div class="card"><div class="label">Total Failed</div><div class="value">{summary['All']['failed']}</div></div>
      <div class="card"><div class="label">Unit Tests</div><div class="value">{summary['Unit']['passed']}/{summary['Unit']['total']}</div></div>
      <div class="card"><div class="label">Integration Tests</div><div class="value">{summary['Integration']['passed']}/{summary['Integration']['total']}</div></div>
    </div>

    <section>
      <h2>Unit Scenarios</h2>
      <table>
        <thead>
          <tr><th>#</th><th>Scenario</th><th>Description</th><th>Status</th><th>Time</th></tr>
        </thead>
        <tbody>
          {render_rows("Unit")}
        </tbody>
      </table>
    </section>

    <section>
      <h2>Integration Scenarios</h2>
      <table>
        <thead>
          <tr><th>#</th><th>Scenario</th><th>Description</th><th>Status</th><th>Time</th></tr>
        </thead>
        <tbody>
          {render_rows("Integration")}
        </tbody>
      </table>
    </section>

    <section class="links">
      <h2>Reports</h2>
      <a href="test-dashboard.html">Dashboard</a>
      {"<a href='" + jacoco_rel + "'>JaCoCo Coverage</a>" if jacoco_exists else "<span style='color:var(--muted)'>JaCoCo report not generated</span>"}
    </section>
  </div>
</body>
</html>
"""
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(doc, encoding="utf-8")


def main() -> int:
    root = project_root()
    results = parse_reports(root)
    if not results:
        print("No Surefire reports found. Run tests first: ./scripts/run-tests.sh", file=sys.stderr)
        return 1

    summary = summarize(results)
    print_terminal_dashboard(results, summary)

    output = root / "target" / "test-dashboard.html"
    generate_html(results, summary, output)
    print(f"Dashboard written to: {output}")
    return 0 if overall_status(summary) == "SUCCESS" else 1


if __name__ == "__main__":
    sys.exit(main())
