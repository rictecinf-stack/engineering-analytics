#!/usr/bin/env python3
"""
Mock do Jenkins — simula os endpoints da REST API usados pelo JenkinsClient.java:

  GET /api/json                      (lista jobs + builds)
  GET /job/{jobName}/api/json        (builds de um job específico)
  GET /queue/api/json                (fila de builds em andamento)

Não tem dependências externas (só a stdlib). Para rodar:

    python3 jenkins_mock_server.py [porta]

Depois aponte o backend para ele em application-mock.yml:

    integrations:
      jenkins:
        url: http://localhost:9002
        user: mock
        api-token: mock-token
"""

import json
import random
import sys
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

random.seed(7)

JOB_NAMES = [
    "payments-api", "checkout-web", "inventory-service", "auth-service",
    "notifications-worker", "catalog-api", "search-service", "fraud-detection-service",
]

# Mesma curva mensal usada nos previews (Jun/24 -> Mai/25)
MONTHLY_TARGETS = [98, 112, 105, 120, 132, 148, 150, 152, 161, 170, 165, 126]
START_YEAR, START_MONTH = 2024, 6

TOTAL_FAILURES = 7
TOTAL_UNSTABLE = 5


def _month_add(year, month, delta):
    idx = (month - 1) + delta
    return year + idx // 12, idx % 12 + 1


def _random_timestamp_in_month(year, month):
    day = random.randint(1, 27)
    hour = random.randint(0, 23)
    minute = random.randint(0, 59)
    dt = datetime(year, month, day, hour, minute)
    return int(dt.timestamp() * 1000)


def _generate_builds():
    """Gera todos os builds, um job aleatório por build, respeitando a curva mensal."""
    builds_by_job = {name: [] for name in JOB_NAMES}
    next_number = {name: 1 for name in JOB_NAMES}
    all_builds_flat = []

    for i, target in enumerate(MONTHLY_TARGETS):
        year, month = _month_add(START_YEAR, START_MONTH, i)
        for _ in range(target):
            job = random.choice(JOB_NAMES)
            number = next_number[job]
            next_number[job] += 1
            timestamp = _random_timestamp_in_month(year, month)
            # duração simula "lead time" (commit -> deploy), entre ~0.5 e 4 dias em ms
            duration_ms = int(random.uniform(0.5, 4.0) * 24 * 60 * 60 * 1000)
            build = {
                "job": job,
                "number": number,
                "timestamp": timestamp,
                "duration": duration_ms,
                "result": "SUCCESS",
                "building": False,
            }
            builds_by_job[job].append(build)
            all_builds_flat.append(build)

    # marca falhas e instabilidades aleatoriamente entre os builds gerados
    sample_failures = random.sample(all_builds_flat, min(TOTAL_FAILURES, len(all_builds_flat)))
    for b in sample_failures:
        b["result"] = "FAILURE"

    remaining = [b for b in all_builds_flat if b["result"] == "SUCCESS"]
    sample_unstable = random.sample(remaining, min(TOTAL_UNSTABLE, len(remaining)))
    for b in sample_unstable:
        b["result"] = "UNSTABLE"

    for job in builds_by_job:
        builds_by_job[job].sort(key=lambda b: b["timestamp"], reverse=True)

    return builds_by_job


BUILDS_BY_JOB = _generate_builds()


def _job_color(job_name):
    last = BUILDS_BY_JOB[job_name][0] if BUILDS_BY_JOB[job_name] else None
    if last is None:
        return "notbuilt"
    return {"SUCCESS": "blue", "FAILURE": "red", "UNSTABLE": "yellow"}.get(last["result"], "grey")


def handle_root_api(qs):
    jobs = []
    for name in JOB_NAMES:
        builds = BUILDS_BY_JOB[name]
        last_build = builds[0] if builds else None
        jobs.append({
            "name": name,
            "url": f"http://localhost:9002/job/{name}/",
            "color": _job_color(name),
            "lastBuild": {
                "number": last_build["number"],
                "timestamp": last_build["timestamp"],
                "result": last_build["result"],
                "duration": last_build["duration"],
            } if last_build else None,
            "builds": [
                {
                    "number": b["number"],
                    "timestamp": b["timestamp"],
                    "result": b["result"],
                    "duration": b["duration"],
                }
                for b in builds
            ],
        })
    return {"jobs": jobs}


def handle_job_api(job_name):
    builds = BUILDS_BY_JOB.get(job_name, [])
    return {
        "builds": [
            {
                "number": b["number"],
                "timestamp": b["timestamp"],
                "result": b["result"],
                "duration": b["duration"],
                "building": b["building"],
            }
            for b in builds
        ]
    }


def handle_queue():
    return {"items": []}


class JenkinsMockHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/api/json":
            body = handle_root_api(parsed.query)
        elif path.startswith("/job/") and path.endswith("/api/json"):
            job_name = path.split("/")[2]
            body = handle_job_api(job_name)
        elif path == "/queue/api/json":
            body = handle_queue()
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(json.dumps({"error": f"rota não mockada: {path}"}).encode())
            return

        payload = json.dumps(body).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        print(f"[jenkins-mock] {self.address_string()} - {format % args}")


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9002
    total_builds = sum(len(b) for b in BUILDS_BY_JOB.values())
    server = ThreadingHTTPServer(("0.0.0.0", port), JenkinsMockHandler)
    server.request_queue_size = 200
    print(f"Jenkins mock rodando em http://localhost:{port}")
    print(f"Jobs: {len(JOB_NAMES)} | Total de builds gerados: {total_builds}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()


if __name__ == "__main__":
    main()
