#!/usr/bin/env python3
"""
Mock do SonarQube — simula os endpoints da REST API usados pelo SonarClient.java:

  GET /api/projects/search
  GET /api/measures/component
  GET /api/issues/search
  GET /api/measures/search_history

Não tem dependências externas (só a stdlib). Para rodar:

    python3 sonar_mock_server.py [porta]

Depois aponte o backend para ele em application.yml:

    integrations:
      sonar:
        url: http://localhost:9001
        token: mock-token
"""

import json
import random
import sys
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

random.seed(42)

# ---------------------------------------------------------------------------
# Geração determinística de projetos "fake", batendo com os números do
# dashboard de exemplo (148 projetos, 12,8M linhas, 82,4% cobertura, etc.)
# ---------------------------------------------------------------------------

LANGUAGE_PLAN = [
    # sonar_key, nome bonito, nº projetos, total ncloc (M), cobertura média, vulnerabilidades totais
    ("java", "Java", 62, 5.2, 84.2, 8),
    ("ts", "TypeScript", 31, 1.9, 81.7, 6),
    ("js", "JavaScript", 28, 2.8, 79.4, 4),
    ("py", "Python", 14, 0.8, 88.1, 2),
    ("sql", "SQL", 9, 1.1, 75.3, 3),
    ("go", "Outros", 4, 1.0, 80.0, 0),
]

PROJECTS = []  # cada item: dict com key, name, language, ncloc, coverage, vulnerabilities, bugs, code_smells, duplication, debt_minutes

_name_pool = [
    "payments", "checkout", "inventory", "auth", "notifications", "catalog",
    "orders", "shipping", "billing", "search", "recommendations", "reviews",
    "cart", "pricing", "loyalty", "reporting", "analytics", "gateway",
    "identity", "fraud-detection", "warehouse", "returns", "support",
    "onboarding", "settlements", "tax-engine", "promotions", "wishlist",
    "subscriptions", "invoices", "kyc", "geolocation", "notif-scheduler",
    "audit-log", "feature-flags", "config-service", "batch-jobs",
]

def _split_amount(total, n):
    """Divide um total em n partes positivas, com variação, somando exatamente ao total."""
    if n == 1:
        return [total]
    weights = [random.uniform(0.5, 1.5) for _ in range(n)]
    s = sum(weights)
    parts = [total * w / s for w in weights]
    # ajusta arredondamento para a soma bater certinho
    diff = total - sum(parts)
    parts[-1] += diff
    return parts

project_counter = 0
for lang_key, lang_name, n_projects, total_m, avg_coverage, total_vulns in LANGUAGE_PLAN:
    ncloc_parts = _split_amount(total_m * 1_000_000, n_projects)
    vuln_parts = _split_amount(max(total_vulns, 0.0001), n_projects) if total_vulns > 0 else [0] * n_projects
    for i in range(n_projects):
        project_counter += 1
        base_name = random.choice(_name_pool)
        suffix = lang_key if lang_key != "go" else "misc"
        name = f"{base_name}-{suffix}-{project_counter}"
        ncloc = round(ncloc_parts[i])
        coverage = round(max(40.0, min(99.0, random.gauss(avg_coverage, 6))), 1)
        duplication = round(max(0.0, random.gauss(2.1, 1.2)), 1)
        debt_minutes = int(ncloc / 1000 * random.uniform(0.8, 1.6))  # ~ minutos de debt por KLOC
        vulnerabilities = round(vuln_parts[i]) if total_vulns > 0 else 0
        bugs = random.randint(0, 6)
        code_smells = random.randint(5, 120)

        PROJECTS.append({
            "key": f"com.empresa:{name}",
            "name": name,
            "language": lang_key,
            "ncloc": ncloc,
            "coverage": coverage,
            "duplication": duplication,
            "debt_minutes": debt_minutes,
            "vulnerabilities": vulnerabilities,
            "bugs": bugs,
            "code_smells": code_smells,
        })

PROJECTS_BY_KEY = {p["key"]: p for p in PROJECTS}

TOTAL_VULNERABILITIES_CRITICAL = 23   # severities=BLOCKER,CRITICAL & types=VULNERABILITY
TOTAL_CRITICAL_ISSUES = 47            # severities=BLOCKER & types=BUG,VULNERABILITY,CODE_SMELL


# ---------------------------------------------------------------------------
# Handlers
# ---------------------------------------------------------------------------

def handle_projects_search(qs):
    page = int(qs.get("p", ["1"])[0])
    page_size = int(qs.get("ps", ["100"])[0])
    start = (page - 1) * page_size
    end = start + page_size
    page_items = PROJECTS[start:end]
    return {
        "paging": {"pageIndex": page, "pageSize": page_size, "total": len(PROJECTS)},
        "components": [{"key": p["key"], "name": p["name"], "qualifier": "TRK"} for p in page_items],
    }


def handle_measures_component(qs):
    component_key = qs.get("component", [""])[0]
    metric_keys = qs.get("metricKeys", [""])[0].split(",")
    project = PROJECTS_BY_KEY.get(component_key)

    if project is None:
        return {"component": {"key": component_key, "name": component_key, "measures": []}}

    metric_values = {
        "coverage": str(project["coverage"]),
        "ncloc": str(project["ncloc"]),
        "ncloc_language_distribution": f"{project['language']}={project['ncloc']}",
        "duplicated_lines_density": str(project["duplication"]),
        "sqale_index": str(project["debt_minutes"]),
        "vulnerabilities": str(project["vulnerabilities"]),
        "bugs": str(project["bugs"]),
        "code_smells": str(project["code_smells"]),
    }

    measures = [
        {"metric": key, "value": metric_values.get(key, "0")}
        for key in metric_keys if key
    ]

    return {
        "component": {
            "key": project["key"],
            "name": project["name"],
            "measures": measures,
        }
    }


def handle_issues_search(qs):
    severities = qs.get("severities", [""])[0]
    types = qs.get("types", [""])[0]

    if "CRITICAL" in severities and "VULNERABILITY" in types:
        total = TOTAL_VULNERABILITIES_CRITICAL
    elif severities == "BLOCKER" and "CODE_SMELL" in types:
        total = TOTAL_CRITICAL_ISSUES
    else:
        total = random.randint(5, 60)

    return {
        "total": total,
        "p": 1,
        "ps": int(qs.get("ps", ["100"])[0]),
        "issues": [],
    }


def handle_measures_history(qs):
    metric = qs.get("metrics", ["ncloc"])[0]
    from_date = qs.get("from", ["2024-06-01"])[0]

    try:
        start = datetime.strptime(from_date, "%Y-%m-%d")
    except ValueError:
        start = datetime(2024, 6, 1)

    # 12 pontos mensais crescendo de ~8.2M até 12.8M (mesma curva do mock visual)
    curve = [8.2, 8.6, 9.0, 9.3, 9.8, 10.2, 10.6, 11.2, 11.6, 12.0, 12.4, 12.8]
    history = []
    for i, value_m in enumerate(curve):
        date = start + timedelta(days=30 * i)
        history.append({
            "date": date.strftime("%Y-%m-%dT00:00:00+0000"),
            "value": str(round(value_m * 1_000_000)),
        })

    return {
        "paging": {"pageIndex": 1, "pageSize": 100, "total": len(history)},
        "measures": [{"metric": metric, "history": history}],
    }


ROUTES = {
    "/api/projects/search": handle_projects_search,
    "/api/measures/component": handle_measures_component,
    "/api/issues/search": handle_issues_search,
    "/api/measures/search_history": handle_measures_history,
}


class SonarMockHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        handler = ROUTES.get(parsed.path)

        if handler is None:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(json.dumps({"error": f"rota não mockada: {parsed.path}"}).encode())
            return

        qs = parse_qs(parsed.query)
        body = handler(qs)

        payload = json.dumps(body).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        print(f"[sonar-mock] {self.address_string()} - {format % args}")


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9001
    server = ThreadingHTTPServer(("0.0.0.0", port), SonarMockHandler)
    server.request_queue_size = 200
    print(f"Sonar mock rodando em http://localhost:{port}")
    print(f"Total de projetos gerados: {len(PROJECTS)}")
    print(f"Total ncloc: {sum(p['ncloc'] for p in PROJECTS):,}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()


if __name__ == "__main__":
    main()
