"""Convenient local launcher; a stable operator token is generated once in .env."""
import os
from pathlib import Path
import secrets
import socket
import sys

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
sys.path.insert(0, str(ROOT))
config = ROOT / ".env"
if not config.exists():
    config.write_text("ALPI_ADMIN_TOKEN=" + secrets.token_urlsafe(32) + "\nALPI_PUBLIC_BASE_URL=\n", encoding="utf-8")
    config.chmod(0o600)
for line in config.read_text(encoding="utf-8").splitlines():
    if line.strip() and not line.startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        if key in ("ALPI_ADMIN_TOKEN", "ALPI_PUBLIC_BASE_URL", "ALPI_DATA") and value:
            os.environ.setdefault(key, value.strip())
port = int(os.getenv("PORT", "8000"))
try:
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.connect(("8.8.8.8", 80))
        address = s.getsockname()[0]
except OSError:
    address = "127.0.0.1"
base = os.environ.setdefault("ALPI_PUBLIC_BASE_URL", f"http://{address}:{port}")
print("\nAlpi · Server album")
print("Alamat server:", base)
print("Token operator ada pada berkas .env; masukkan ke Pengaturan Alpi.")
print("Sambungkan HP dan laptop ke jaringan Wi-Fi yang sama. Biarkan terminal ini terbuka.\n")
import uvicorn
uvicorn.run("server.app:app", host="0.0.0.0", port=port, proxy_headers=False)
