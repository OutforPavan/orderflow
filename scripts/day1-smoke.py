#!/usr/bin/env python3
"""Exercise the packaged application over real HTTP and across an app restart.

Uses only the dedicated orderflow_test database. Leaves its two labeled demo rows
available for inspection; does not delete data or stop any pre-existing process.
Run ./scripts/db start and ./dev verify first.
"""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / ".tools/test-database.properties"
JAR = ROOT / "target/orderflow-0.0.1-SNAPSHOT.jar"
LOCAL_JAVA = ROOT / ".tools/java21/Contents/Home/bin/java"


def request(base, method, path, body=None, expected=200):
    payload = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=payload, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        result = json.load(response)
        assert response.status == expected, (method, path, response.status, result)
        return result, response.headers


class Application:
    def __init__(self, label, arguments):
        self.log_path = ROOT / "target" / ("day1-smoke-" + label + ".log")
        self.arguments = arguments
        self.process = None

    def __enter__(self):
        environment = os.environ.copy()
        environment["SPRING_CONFIG_ADDITIONAL_LOCATION"] = CONFIG.as_uri()
        environment["LEARNING_MESSAGE"] = "From environment"
        self.log = self.log_path.open("w")
        java = str(LOCAL_JAVA) if LOCAL_JAVA.is_file() else "java"
        self.process = subprocess.Popen(
            [java, "-jar", str(JAR), "--server.address=127.0.0.1", "--server.port=0",
             "--spring.profiles.active=sql", *self.arguments],
            cwd=ROOT, env=environment, stdout=self.log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                match = re.search(r"Tomcat started on port (\d+)", self.log_path.read_text())
                if match:
                    return "http://127.0.0.1:" + match.group(1)
                if self.process.poll() is not None:
                    raise RuntimeError("Application failed; inspect " + str(self.log_path))
                time.sleep(0.2)
            raise TimeoutError("Startup exceeded 60 seconds; inspect " + str(self.log_path))
        except BaseException:
            self.__exit__(None, None, None)
            raise

    def __exit__(self, *_):
        if self.process is not None and self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=5)
        self.log.close()


def main():
    if not CONFIG.is_file() or not JAR.is_file():
        raise SystemExit("Run ./scripts/db start and ./dev verify first.")
    # No destructive cleanup, but still refuse the normal learning database.
    config = dict(line.split("=", 1) for line in CONFIG.read_text().splitlines()
                  if line.strip() and not line.lstrip().startswith("#") and "=" in line)
    assert re.search(r"/orderflow_test(?:\?|$)", config.get("spring.datasource.url", "")), \
        "This smoke check requires orderflow_test."
    # Environment-level datasource overrides would supersede the dedicated file.
    assert not any(key.startswith(("SPRING_DATASOURCE_", "SPRING_FLYWAY_")) for key in os.environ), \
        "Unset datasource/Flyway environment overrides before this isolated smoke check."

    with Application("first", []) as base:
        status, _ = request(base, "GET", "/api/learning/status")
        assert status["message"] == "From environment"
        product, headers = request(base, "POST", "/api/products",
                                   {"name": "Day 1 HTTP smoke product", "price": 1250, "stock": 10}, 201)
        product_path = "/api/products/" + str(product["id"])
        assert headers["Location"] == product_path
        invalid, _ = request(base, "POST", "/api/products", {"name": "", "price": -1, "stock": -1}, 400)
        assert len(invalid["fieldErrors"]) == 3
        request(base, "GET", "/api/products/9223372036854775807", expected=404)
        request(base, "PATCH", product_path + "/price", {"price": 1199})
        order, headers = request(base, "POST", "/api/orders",
                                 {"productId": product["id"], "quantity": 2}, 201)
        order_path = "/api/orders/" + str(order["id"])
        assert headers["Location"] == order_path and order["total"] == 2398
        request(base, "POST", "/api/orders", {"productId": product["id"], "quantity": 100}, 409)
        current, _ = request(base, "GET", product_path)
        assert current["stock"] == 8 and current["price"] == 1199
        print("PASS: configuration, create/read/update, validation, 404, order total, stock, 409")

    with Application("restart", ["--learning.message=From command line"]) as base:
        status, _ = request(base, "GET", "/api/learning/status")
        product, _ = request(base, "GET", product_path)
        persisted_order, _ = request(base, "GET", order_path)
        assert status["message"] == "From command line"
        assert product["stock"] == 8 and product["price"] == 1199
        assert persisted_order["total"] == 2398
        print("PASS: product and order survive application restart; CLI overrides environment")
    print("Stopped only the two temporary application processes. SQL logs: target/day1-smoke-*.log")
    print("Inspect test-database demo rows:", product_path, order_path)


if __name__ == "__main__":
    main()
