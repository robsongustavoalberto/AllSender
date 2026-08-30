import socket

def get_local_ip():
    """
    Obtém o endereço IP local do dispositivo.
    Tenta abrir um socket UDP temporário para identificar a interface de rede ativa.
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Não estabelece conexão real, apenas identifica a rota de saída
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip

def find_free_port(default_port=8080):
    """
    Verifica se a porta padrão está livre; caso contrário, busca uma disponível.
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            s.bind(("", default_port))
            return default_port
        except OSError:
            # Se a porta padrão estiver ocupada, solicita ao SO uma porta livre
            s.bind(("", 0))
            return s.getsockname()[1]