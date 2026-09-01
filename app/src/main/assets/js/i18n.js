const i18nData = {
  pt: {
    app_tagline: "Transferência de ficheiros simples, direta e FOSS",
    tab_send: "Enviar",
    tab_receive: "Receber",
    step1_title: "1. Selecionar Ficheiros",
    dropzone_text: "Clique ou arraste ficheiros para aqui",
    step2_title: "2. Iniciar Ponto de Partilha",
    step2_desc: "Gere um endereço local (Wi-Fi/Hotspot) ou insira um IP para longa distância.",
    btn_start_share: "Ativar Partilha",
    status_active: "Servidor Ativo",
    receive_title: "Conectar a um Emissor",
    receive_desc: "Insira o endereço IP fornecido pelo outro celular (local ou longa distância):",
    placeholder_ip: "Ex: 192.168.43.1:8080 ou IP Externo",
    btn_connect: "Aceder Ficheiros",
    remote_files_title: "Ficheiros Disponíveis",
    alert_no_files: "Por favor, selecione pelo menos um ficheiro antes de ativar a partilha.",
    alert_no_ip: "Por favor, insira o endereço IP do emissor."
  },
  en: {
    app_tagline: "Simple, direct, and FOSS file transfer",
    tab_send: "Send",
    tab_receive: "Receive",
    step1_title: "1. Select Files",
    dropzone_text: "Click or drag files here",
    step2_title: "2. Start Sharing Point",
    step2_desc: "Generate a local address (Wi-Fi/Hotspot) or enter an IP for long distance.",
    btn_start_share: "Start Sharing",
    status_active: "Server Active",
    receive_title: "Connect to Sender",
    receive_desc: "Enter the IP address provided by the other phone (local or long distance):",
    placeholder_ip: "e.g., 192.168.43.1:8080 or External IP",
    btn_connect: "Access Files",
    remote_files_title: "Available Files",
    alert_no_files: "Please select at least one file before activating sharing.",
    alert_no_ip: "Please enter the sender's IP address."
  },
  es: {
    app_tagline: "Transferencia de archivos simple, directa y FOSS",
    tab_send: "Enviar",
    tab_receive: "Recibir",
    step1_title: "1. Seleccionar Archivos",
    dropzone_text: "Haga clic o arrastre archivos aquí",
    step2_title: "2. Iniciar Punto de Compartición",
    step2_desc: "Genere una dirección local (Wi-Fi/Hotspot) o ingrese una IP para larga distancia.",
    btn_start_share: "Activar Compartición",
    status_active: "Servidor Activo",
    receive_title: "Conectarse a un Emisor",
    receive_desc: "Ingrese la dirección IP proporcionada por el otro teléfono:",
    placeholder_ip: "Ej: 192.168.43.1:8080 o IP Externa",
    btn_connect: "Acceder a Archivos",
    remote_files_title: "Archivos Disponibles",
    alert_no_files: "Por favor, seleccione al menos un archivo antes de activar la compartición.",
    alert_no_ip: "Por favor, ingrese la dirección IP del emisor."
  }
};

let currentLang = 'en';

function detectAndApplyLanguage() {
  const userLang = navigator.language || navigator.userLanguage;
  const langCode = userLang.substring(0, 2).toLowerCase();
  
  if (i18nData[langCode]) {
    currentLang = langCode;
  } else {
    currentLang = 'en'; // Idioma padrão caso o sistema esteja em outro idioma
  }

  applyTranslations();
}

function applyTranslations() {
  const lang = i18nData[currentLang];
  document.querySelectorAll('[data-i18n]').forEach(element => {
    const key = element.getAttribute('data-i18n');
    if (lang[key]) {
      element.textContent = lang[key];
    }
  });

  document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
    const key = element.getAttribute('data-i18n-placeholder');
    if (lang[key]) {
      element.placeholder = lang[key];
    }
  });
}

