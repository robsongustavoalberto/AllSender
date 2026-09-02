import os
import json
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import unquote
import network_utils

# Armazena o estado global dos arquivos compartilhados
SHARED_FILES = []
SERVER_INSTANCE = None
SERVER_THREAD = None

class FileShareHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        # Desativa logs padrão no console para melhorar a performance
        pass

    def do_GET(self):
        url_path = unquote(self.path)

        # Rota de API: Lista de arquivos disponíveis para o receptor
        if url_path == "/api/files":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            
            file_list = [
                {
                    "id": idx,
                    "name": os.path.basename(path),
                    "size": os.path.getsize(path) if os.path.exists(path) else 0
                }
                for idx, path in enumerate(SHARED_FILES) if os.path.exists(path)
            ]
            self.wfile.write(json.dumps(file_list).encode("utf-8"))
            return

        # Rota de Download: Download individual de arquivos via ID (/download/0)
        if url_path.startswith("/download/"):
            try:
                file_idx = int(url_path.split("/download/")[1])
                if 0 <= file_idx < len(SHARED_FILES):
                    file_path = SHARED_FILES[file_idx]
                    if os.path.exists(file_path):
                        filename = os.path.basename(file_path)
                        filesize = os.path.getsize(file_path)

                        self.send_response(200)
                        self.send_header("Content-Type", "application/octet-stream")
                        self.send_header("Content-Disposition", f'attachment; filename="{filename}"')
                        self.send_header("Content-Length", str(filesize))
                        self.send_header("Access-Control-Allow-Origin", "*")
                        self.end_headers()

                        # Envio em chunks para economizar memória RAM durante arquivos grandes
                        with open(file_path, "rb") as f:
                            while chunk := f.read(64 * 1024):
                                self.wfile.write(chunk)
                        return
            except (ValueError, IndexError):
                pass

            self.send_error(444, "Arquivo não encontrado")
            return

        # Fallback para rotas não mapeadas
        self.send_error(404, "Rota inválida")


def start_server(file_paths):
    """
    Inicia o servidor em uma thread separada para não bloquear a interface.
    """
    global SHARED_FILES, SERVER_INSTANCE, SERVER_THREAD

    stop_server() # Garante que instâncias anteriores sejam encerradas

    SHARED_FILES = file_paths
    ip = network_utils.get_local_ip()
    port = network_utils.find_free_port()

    SERVER_INSTANCE = HTTPServer((ip, port), FileShareHandler)
    SERVER_THREAD = threading.Thread(target=SERVER_INSTANCE.serve_forever)
    SERVER_THREAD.daemon = True
    SERVER_THREAD.start()

    return json.dumps({
        "status": "success",
        "ip": ip,
        "port": port,
        "url": f"http://{ip}:{port}"
    })


def stop_server():
    """
    Encerra a execução do servidor HTTP local.
    """
    global SERVER_INSTANCE, SERVER_THREAD
    if SERVER_INSTANCE:
        SERVER_INSTANCE.shutdown()
        SERVER_INSTANCE.server_close()
        SERVER_INSTANCE = None
        SERVER_THREAD = None
    return json.dumps({"status": "stopped"})