"""Servidor de demonstração LATAM Any Wear: estáticos + API de pré-reserva (sem pagamento, sem preços)."""
import json, os, re, threading, time, uuid
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from urllib.parse import parse_qs, urlparse

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "pre_reservas.jsonl"
LOCK = threading.Lock()
MAX_BODY = 4096
ESTACOES = {"primavera", "verao", "outono", "inverno"}
TAMANHOS = {"PP", "P", "M", "G", "GG"}
PECAS = {"jaquetas", "calcas", "botas", "toucas"}
EMAIL = re.compile(r"^[^@\s]{1,64}@[^@\s]{1,190}\.[^@\s]{2,}$")

def validar(f):
    g = lambda k: f.get(k, [""])[0].strip()[:80]
    pecas = [p for p in f.get("pecas", []) if p in PECAS]
    erros = [k for k, ok in {
        "email": bool(EMAIL.match(f.get("email", [""])[0].strip())),
        "destino": bool(g("destino")),
        "estacao": g("estacao") in ESTACOES,
        "tamanho": g("tamanho") in TAMANHOS,
        "pecas": bool(pecas),
        "consentimento": g("consentimento") == "on",
    }.items() if not ok]
    reg = {"id": uuid.uuid4().hex[:12], "em": time.strftime("%Y-%m-%dT%H:%M:%S"),
           "nome": g("nome"), "email": g("email"), "destino": g("destino"),
           "estacao": g("estacao"), "tamanho": g("tamanho"), "estilo": g("estilo"), "pecas": sorted(set(pecas))}
    return erros, reg

class Handler(SimpleHTTPRequestHandler):
    extensions_map = {**SimpleHTTPRequestHandler.extensions_map,
        ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8",
        ".js": "application/javascript; charset=utf-8", ".svg": "image/svg+xml"}

    def __init__(self, *a, **k):
        super().__init__(*a, directory=str(ROOT), **k)

    def end_headers(self):
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()

    def _json(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _privado(self):
        return urlparse(self.path).path.startswith(("/data", "/backend", "/."))

    def do_GET(self):
        p = urlparse(self.path).path
        if p == "/api/health": return self._json(200, {"ok": True})
        if self._privado(): return self._json(404, {"erro": "nao_encontrado"})
        super().do_GET()

    def do_HEAD(self):
        if self._privado(): return self._json(404, {"erro": "nao_encontrado"})
        super().do_HEAD()

    def do_POST(self):
        if urlparse(self.path).path != "/api/pre-reserva": return self._json(404, {"erro": "nao_encontrado"})
        n = int(self.headers.get("Content-Length") or 0)
        if n <= 0 or n > MAX_BODY: return self._json(413, {"erro": "tamanho"})
        form = parse_qs(self.rfile.read(n).decode("utf-8", "replace"))
        erros, reg = validar(form)
        if erros: return self._json(422, {"erro": "validacao", "campos": erros})
        with LOCK:
            DATA.parent.mkdir(exist_ok=True)
            with DATA.open("a", encoding="utf-8") as fh:
                fh.write(json.dumps(reg, ensure_ascii=False) + "\n")
        self._json(201, {"ok": True, "id": reg["id"]})

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    print(f"Servidor LATAM demo em http://localhost:{port}")
    ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
