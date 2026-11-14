#!/usr/bin/env python3
"""Run the real two-tab test on an isolated, disposable local backend/database."""

import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

import bcrypt


WORKTREE = Path(__file__).resolve().parents[2]
FRONTEND = WORKTREE / "frontend"
DB = "gym-auth-tabs-test-db"
API = "gym-auth-tabs-test-api"
NETWORK = "gym-auth-tabs-test"
LABEL = "gym.auth-tabs.run"


def command(*args, **kwargs):
    kwargs.setdefault("timeout", 30)
    return subprocess.run(args, check=True, capture_output=True, **kwargs)


def stop_process(process):
    """Terminate only the process group created by this runner."""
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except ProcessLookupError:
        return
    try:
        process.wait(timeout=10)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait(timeout=5)


def wait_until(check, seconds, description):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if check():
            return
        time.sleep(1)
    raise RuntimeError(f"Hết thời gian chờ: {description}")


def healthy():
    try:
        with urllib.request.urlopen("http://127.0.0.1:8081/actuator/health", timeout=2) as response:
            return json.load(response).get("status") == "UP"
    except (OSError, urllib.error.URLError):
        return False


def vite_ready():
    try:
        with urllib.request.urlopen("http://localhost:5173/login", timeout=2) as response:
            return response.status == 200
    except (OSError, urllib.error.URLError):
        return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-local", action="store_true")
    options = parser.parse_args()
    for name in ("docker", "node", "npm"):
        if not shutil.which(name):
            raise RuntimeError(f"Thiếu lệnh: {name}")
    for file in (WORKTREE / "backend/Dockerfile", FRONTEND / "e2e/multi-tab-session.mjs"):
        if not file.is_file():
            raise RuntimeError(f"Thiếu tệp: {file}")
    head = command("git", "-C", str(WORKTREE), "rev-parse", "HEAD").stdout.decode().strip()
    if options.check_local:
        print(f"Kiểm tra cục bộ đạt; head {head}; chưa tạo container, tiến trình hoặc gọi API.")
        return

    if command("git", "-C", str(WORKTREE), "status", "--porcelain").stdout.strip():
        raise RuntimeError("Worktree còn thay đổi chưa commit; chốt nguồn trước khi ghi bằng chứng theo head.")

    command("docker", "info")
    for port in (5173, 8081):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
            try:
                probe.bind(("127.0.0.1", port))
            except OSError as error:
                raise RuntimeError(f"Cổng {port} đang bận hoặc bị chặn; chưa khởi động bản sao mới.") from error
    for kind, name in (("container", DB), ("container", API), ("network", NETWORK)):
        existing = subprocess.run(("docker", kind, "inspect", name), capture_output=True, timeout=15)
        if existing.returncode == 0:
            raise RuntimeError(f"Tài nguyên {name} đã tồn tại; không tự dừng/xóa tài nguyên phiên khác.")

    artifacts = Path(tempfile.mkdtemp(prefix="gym-auth-tabs-"))
    os.chmod(artifacts, 0o700)
    run_id = secrets.token_hex(10)
    password = "T" + secrets.token_urlsafe(24) + "7"
    database_password = secrets.token_urlsafe(32)
    jwt_secret = secrets.token_hex(32)
    seed_hash = bcrypt.hashpw(secrets.token_bytes(24), bcrypt.gensalt()).decode()
    private_values = (password, database_password, jwt_secret, seed_hash)

    def save_log(name, data):
        text = data.decode(errors="replace") if isinstance(data, bytes) else data
        for value in private_values:
            text = text.replace(value, "[redacted]")
        text = re.sub(r"eyJ[\w-]+\.[\w-]+\.[\w-]+", "[redacted-jwt]", text)
        path = artifacts / name
        path.write_text(text)
        os.chmod(path, 0o600)

    def env_file(name, entries):
        path = artifacts / name
        path.write_text("".join(f"{key}={value}\n" for key, value in entries.items()))
        os.chmod(path, 0o600)
        return path

    def logged_process(args, log_name, timeout, **kwargs):
        process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   start_new_session=True, **kwargs)
        deadline = time.monotonic() + timeout
        try:
            while True:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise subprocess.TimeoutExpired(args, timeout)
                try:
                    output, _ = process.communicate(timeout=min(30, remaining))
                    save_log(log_name, output)
                    if process.returncode:
                        raise RuntimeError(f"{log_name}: lệnh thất bại ({process.returncode}); xem log đã che bí mật.")
                    return
                except subprocess.TimeoutExpired:
                    print(f"Đang chạy {log_name}; còn tối đa {max(0, int(deadline - time.monotonic()))} giây.", flush=True)
        finally:
            stop_process(process)
            output, _ = process.communicate()
            save_log(log_name, output)

    def backup(name):
        path = artifacts / name
        with path.open("wb") as output:
            subprocess.run(("docker", "exec", DB, "pg_dump", "-U", "gym_test", "-d", "gym_test", "-Fc"),
                           stdout=output, stderr=subprocess.PIPE, check=True, timeout=30)
        os.chmod(path, 0o600)
        if path.stat().st_size == 0:
            raise RuntimeError("Bản sao lưu rỗng; chưa chạy migration/test.")
        return path

    db_env = env_file("database.env", {
        "POSTGRES_DB": "gym_test", "POSTGRES_USER": "gym_test", "POSTGRES_PASSWORD": database_password,
    })
    api_env = env_file("backend.env", {
        "SPRING_PROFILES_ACTIVE": "local", "DB_HOST": DB, "DB_PORT": "5432",
        "DB_NAME": "gym_test", "DB_USERNAME": "gym_test", "DB_PASSWORD": database_password,
        "JWT_SECRET": jwt_secret, "SEED_PASSWORD_HASH": seed_hash,
        "CORS_ALLOWED_ORIGINS": "http://localhost:5173",
    })
    vite = None
    print(f"Artifact/log/backup: {artifacts}", flush=True)
    try:
        browser = os.environ.get("GYM_BROWSER_EXECUTABLE")
        if not browser:
            browser = command("node", "--input-type=module", "-e",
                              "import {chromium} from 'playwright'; console.log(chromium.executablePath())",
                              cwd=FRONTEND).stdout.decode().strip()
            if not Path(browser).is_file():
                candidates = sorted((Path.home() / ".cache/ms-playwright").glob("chromium-*/chrome-linux*/chrome"))
                browser = str(candidates[-1]) if candidates else browser
        if not Path(browser).is_file() or not os.access(browser, os.X_OK):
            raise RuntimeError("Thiếu Chromium. Chạy npx playwright install chromium trong frontend, hoặc đặt GYM_BROWSER_EXECUTABLE.")
        print("Đóng gói backend thật từ worktree; có thể mất vài phút.", flush=True)
        image = f"gym-auth-tabs-backend:{head[:12]}"
        logged_process(("docker", "build", "-t", image, str(WORKTREE / "backend")), "backend-build.log", 1200)
        command("docker", "network", "create", "--label", f"{LABEL}={run_id}", NETWORK)
        command("docker", "run", "-d", "--name", DB, "--label", f"{LABEL}={run_id}",
                "--network", NETWORK, "--env-file", str(db_env), "postgres:16-alpine", timeout=180)
        wait_until(lambda: subprocess.run(("docker", "exec", DB, "pg_isready", "-U", "gym_test"),
                                         capture_output=True, timeout=5).returncode == 0, 60, "PostgreSQL")
        backup("before-migrations.dump")
        print("Đã backup DB mới trước migration; khởi động backend.", flush=True)
        command("docker", "run", "-d", "--name", API, "--label", f"{LABEL}={run_id}",
                "--network", NETWORK, "--env-file", str(api_env), "-p", "127.0.0.1:8081:8080", image)
        wait_until(healthy, 180, "backend health UP")
        baseline = backup("before-session-test.dump")
        email = f"tabs-{run_id}@example.com"
        data = json.dumps({"fullName": "Kiem thu phien nhieu tab", "email": email,
                           "phone": "09" + f"{secrets.randbelow(10**8):08d}", "password": password}).encode()
        request = urllib.request.Request("http://127.0.0.1:8081/api/v1/auth/register", data=data,
                                         headers={"Content-Type": "application/json", "Origin": "http://localhost:5173"})
        with urllib.request.urlopen(request, timeout=20) as response:
            if response.status != 201:
                raise RuntimeError("Đăng ký tài khoản kiểm thử không trả 201.")
        environment = dict(os.environ, VITE_API_URL="/api/v1", GYM_TEMP_DIR=str(artifacts),
                           GYM_E2E_EMAIL=email, GYM_E2E_PASSWORD=password, GYM_E2E_DB_BACKUP=str(baseline),
                           GYM_BROWSER_EXECUTABLE=browser)
        code = """import {createServer} from 'vite';
const server=await createServer({envDir:process.env.GYM_TEMP_DIR,server:{host:'localhost',port:5173,strictPort:true,proxy:{'/api':'http://127.0.0.1:8081'}}});
await server.listen();"""
        vite = subprocess.Popen(("node", "--input-type=module", "-e", code), cwd=FRONTEND,
                                env=environment, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                                start_new_session=True)
        print(f"Vite PID {vite.pid}, port 5173, worktree {WORKTREE}", flush=True)
        wait_until(vite_ready, 30, "Vite")
        print("Chạy Playwright hai tab với API/cookie thật.", flush=True)
        logged_process(("npm", "run", "test:auth:tabs"), "playwright.log", 150, cwd=FRONTEND, env=environment)
        (artifacts / "result.json").write_text(json.dumps({
            "head": head, "result": "PASS", "backend": "worktree Dockerfile", "database": "postgres:16-alpine",
            "browser": browser,
            "checks": ["cross-tab lock before dispatch", "real cookie rotation", "concurrent reload",
                       "same identity", "HttpOnly", "empty Web Storage", "replayed token rejected"],
        }, indent=2))
        print("PASS: hai tab giữ phiên; token cũ vẫn bị từ chối. Kết quả ở result.json.", flush=True)
    finally:
        cleanup_errors = []
        if vite is not None:
            try:
                stop_process(vite)
            except (OSError, subprocess.SubprocessError) as error:
                cleanup_errors.append(f"Vite PID {vite.pid}: {error}")
        for kind, name in (("container", API), ("container", DB), ("network", NETWORK)):
            try:
                owner = subprocess.run(("docker", kind, "inspect", "--format",
                                        '{{index .Labels "gym.auth-tabs.run"}}' if kind == "network" else
                                        '{{index .Config.Labels "gym.auth-tabs.run"}}', name),
                                       capture_output=True, timeout=15)
                if owner.returncode != 0:
                    if b"No such" not in owner.stderr:
                        cleanup_errors.append(f"Không kiểm tra được chủ sở hữu {name}.")
                    continue
                if owner.stdout.decode().strip() != run_id:
                    continue
                if kind == "container":
                    try:
                        logs = subprocess.run(("docker", "logs", name), capture_output=True, timeout=15)
                        save_log(f"{name}.log", logs.stdout + logs.stderr)
                    except (OSError, subprocess.SubprocessError) as error:
                        print(f"Không lấy được log {name}: {type(error).__name__}", flush=True)
                    command("docker", "stop", "--time", "10", name)
                command("docker", kind, "rm", name)
            except (OSError, subprocess.SubprocessError) as error:
                cleanup_errors.append(f"{name}: {error}")
        for path in (db_env, api_env):
            try:
                path.unlink(missing_ok=True)
            except OSError as error:
                cleanup_errors.append(f"Xóa tệp môi trường riêng: {error}")
        if cleanup_errors:
            raise RuntimeError("Chưa dọn hết tài nguyên: " + "; ".join(cleanup_errors))
        print("Đã dọn tiến trình/tài nguyên thuộc lần thử; giữ log đã che bí mật và backup.", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.SubprocessError) as error:
        print(f"Không hoàn tất kiểm thử: {type(error).__name__}: {error}")
        raise SystemExit(1)
