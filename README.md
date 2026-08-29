# AllSender 🚀

**AllSender** é uma aplicação Android leve, moderna e *open source* para partilha e transferência local de ficheiros entre dispositivos via Wi-Fi e Ponto de Acesso (Hotspot), sem necessidade de ligação à Internet.

A arquitetura combina a simplicidade de uma interface web progressiva (HTML5/CSS3/JavaScript) com o desempenho de um servidor local em **Python** embarcado nativamente através do **Chaquopy**.

---

## 🌟 Principais Recursos

- 🔒 **100% Offline e Privado**: Nenhuns dados passam por servidores na nuvem ou serviços de terceiros.
- ⚡ **Transferência em Alta Velocidade**: Utiliza a largura de banda máxima da sua rede Wi-Fi local.
- 🌍 **Interface Multilíngue**: Suporte dinâmico para múltiplos idiomas com deteção automática do sistema.
- 🌐 **Compatibilidade Multiplataforma**: O dispositivo emissor gera um servidor HTTP acessível por qualquer navegador web (Android, iOS, Windows, Linux, Mac).
- 🎨 **Design Moderno e Responsivo**: Interface limpa em modo escuro (Dark Mode) otimizada para navegação móvel.
- 📦 **Sem Dependências Proprietárias**: Totalmente livre de rastreadores, telemetria ou SDKs fechados.

---

## 🛠️ Arquitetura do Projeto

O projeto adota uma arquitetura híbrida e descentralizada:

```text
[ Front-end (WebCode / Assets) ] 
         │ (JavaScript Bridge)
         ▼
[ Native Wrapper (Android / Java) ]
         │ (Chaquopy Engine)
         ▼
[ Back-end (Python / HTTP Server) ]
