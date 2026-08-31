/**
 * AllSender - Lógica da Interface Principal (Front-end)
 */

const AppState = {
  selectedFiles: [],
  isSharing: false,
  tutorialStep: 0
};

// Passos do Tutorial para novos utilizadores
const TutorialSteps = [
  {
    title: "Bem-vindo ao AllSender",
    desc: "Partilhe ficheiros em alta velocidade via Wi-Fi ou Ponto de Acesso sem gastar dados móveis.",
    highlight: null
  },
  {
    title: "1. Selecionar Ficheiros",
    desc: "Na aba 'Enviar', toque na área de seleção para escolher os ficheiros que deseja transferir.",
    highlight: "guide-step-select"
  },
  {
    title: "2. Ativar Partilha",
    desc: "Clique em 'Ativar Partilha'. O AllSender gerará um endereço IP local para o outro dispositivo aceder.",
    highlight: "guide-step-share"
  },
  {
    title: "3. Receber Ficheiros",
    desc: "No outro telemóvel ou PC, introduza o IP fornecido na aba 'Receber' para descarregar os ficheiros.",
    highlight: "guide-step-receive"
  }
];

// 1. Inicialização Imediata e Otimizada
(function initApp() {
  if (typeof detectAndApplyLanguage === 'function') {
    detectAndApplyLanguage();
  }

  window.addEventListener('load', () => {
    setTimeout(() => {
      const splash = document.getElementById("splash-screen");
      const mainApp = document.getElementById("mainApp");

      if (splash && mainApp) {
        splash.classList.add("hidden");
        mainApp.classList.add("visible");
        
        setTimeout(() => {
          splash.style.display = "none";
          checkFirstRunTutorial();
        }, 300);
      }
    }, 600);
  });
})();

// 2. Gestão do Tutorial de Primeiro Acesso
function checkFirstRunTutorial() {
  const isFirstRun = !localStorage.getItem("allsender_tutorial_seen");
  if (isFirstRun) {
    showTutorialStep(0);
  }
}

function showTutorialStep(stepIndex) {
  const modal = document.getElementById("tutorial-modal");
  const titleEl = document.getElementById("tutorial-title");
  const bodyEl = document.getElementById("tutorial-body");
  const btnNext = document.getElementById("tutorial-btn-next");

  if (!modal || stepIndex >= TutorialSteps.length) {
    if (modal) modal.style.display = "none";
    localStorage.setItem("allsender_tutorial_seen", "true");
    return;
  }

  const step = TutorialSteps[stepIndex];
  AppState.tutorialStep = stepIndex;

  titleEl.textContent = step.title;
  bodyEl.textContent = step.desc;
  btnNext.textContent = (stepIndex === TutorialSteps.length - 1) ? "Entendido!" : "Próximo";

  modal.style.display = "flex";
}

function nextTutorialStep() {
  showTutorialStep(AppState.tutorialStep + 1);
}

// 3. Alternância de Abas
function switchTab(tabId) {
  const contents = document.querySelectorAll('.tab-content');
  const buttons = document.querySelectorAll('.tab-btn');

  contents.forEach(tab => tab.classList.remove('active'));
  buttons.forEach(btn => btn.classList.remove('active'));

  const targetTab = document.getElementById(tabId);
  if (targetTab) {
    targetTab.classList.add('active');
  }

  const activeBtn = Array.from(buttons).find(btn => 
    btn.getAttribute('onclick') && btn.getAttribute('onclick').includes(tabId)
  );
  if (activeBtn) {
    activeBtn.classList.add('active');
  }
}

// 4. Formatação de Tamanho de Ficheiro
function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 5. Seleção de Ficheiros
function handleFileSelection(event) {
  const files = Array.from(event.target.files);
  AppState.selectedFiles = files;
  
  const fileListElement = document.getElementById('selectedFilesList');
  if (!fileListElement) return;

  fileListElement.innerHTML = '';

  files.forEach(file => {
    const li = document.createElement('li');
    li.className = 'file-item';

    const nameSpan = document.createElement('span');
    nameSpan.textContent = file.name;

    const sizeSpan = document.createElement('span');
    sizeSpan.style.color = 'var(--text-muted)';
    sizeSpan.style.fontSize = '12px';
    sizeSpan.textContent = formatFileSize(file.size);

    li.appendChild(nameSpan);
    li.appendChild(sizeSpan);
    fileListElement.appendChild(li);
  });
}

// 6. Iniciar Servidor de Partilha (Emissão)
function startSharingServer() {
  if (AppState.selectedFiles.length === 0) {
    const msg = (typeof i18nData !== 'undefined' && typeof currentLang !== 'undefined' && i18nData[currentLang]) 
      ? i18nData[currentLang].alert_no_files 
      : "Selecione pelo menos um ficheiro.";
    alert(msg);
    return;
  }

  const serverDetails = document.getElementById('serverDetails');
  const displayIp = document.getElementById('displayIp');

  if (window.pythonBridge && typeof window.pythonBridge.startServer === 'function') {
    try {
      const filePaths = AppState.selectedFiles.map(f => f.path || f.name);
      const resultJson = window.pythonBridge.startServer(JSON.stringify(filePaths));
      const response = JSON.parse(resultJson);

      if (response.status === "success") {
        displayIp.textContent = `IP: ${response.url}`;
        serverDetails.style.display = 'block';
        AppState.isSharing = true;
      } else {
        alert("Erro ao iniciar servidor de partilha.");
      }
    } catch (err) {
      console.error("Erro na ponte Python:", err);
    }
  } else {
    displayIp.textContent = "IP: http://192.168.43.1:8080";
    serverDetails.style.display = 'block';
    AppState.isSharing = true;
  }
}

// 7. Conectar ao Emissor (Recepção)
async function connectToSender() {
  const input = document.getElementById('targetIpInput');
  const targetIp = input ? input.value.trim() : '';

  if (!targetIp) {
    const msg = (typeof i18nData !== 'undefined' && typeof currentLang !== 'undefined' && i18nData[currentLang]) 
      ? i18nData[currentLang].alert_no_ip 
      : "Insira o IP do emissor.";
    alert(msg);
    return;
  }

  const baseUrl = targetIp.startsWith('http') ? targetIp : `http://${targetIp}`;
  const remoteCard = document.getElementById('remoteFilesCard');
  const remoteList = document.getElementById('remoteFilesList');
  
  if (!remoteCard || !remoteList) return;

  remoteCard.style.display = 'block';
  remoteList.innerHTML = '<li class="file-item" style="color: var(--text-muted);">A ligar e a obter ficheiros...</li>';

  try {
    const response = await fetch(`${baseUrl}/api/files`, { mode: 'cors' });
    if (!response.ok) throw new Error("Servidor indisponível");

    const files = await response.json();
    remoteList.innerHTML = '';

    if (!files || files.length === 0) {
      remoteList.innerHTML = '<li class="file-item">Nenhum ficheiro disponível no momento.</li>';
      return;
    }

    files.forEach(file => {
      const li = document.createElement('li');
      li.className = 'file-item';

      const infoDiv = document.createElement('div');
      const nameStrong = document.createElement('strong');
      nameStrong.textContent = file.name;
      
      const sizeDiv = document.createElement('div');
      sizeDiv.style.color = 'var(--text-muted)';
      sizeDiv.style.fontSize = '12px';
      sizeDiv.textContent = formatFileSize(file.size);

      infoDiv.appendChild(nameStrong);
      infoDiv.appendChild(sizeDiv);

      const downloadBtn = document.createElement('a');
      downloadBtn.href = `${baseUrl}/download/${file.id}`;
      downloadBtn.setAttribute('download', file.name);
      downloadBtn.className = 'btn';
      downloadBtn.style.width = 'auto';
      downloadBtn.style.padding = '6px 14px';
      downloadBtn.style.textDecoration = 'none';
      downloadBtn.style.fontSize = '13px';
      downloadBtn.textContent = (typeof i18nData !== 'undefined' && typeof currentLang !== 'undefined' && i18nData[currentLang] && i18nData[currentLang].btn_download) 
        ? i18nData[currentLang].btn_download 
        : "Baixar";

      li.appendChild(infoDiv);
      li.appendChild(downloadBtn);
      remoteList.appendChild(li);
    });

  } catch (error) {
    remoteList.innerHTML = '<li class="file-item" style="color: #ef4444;">Erro ao conectar ao endereço fornecido. Verifique a ligação Wi-Fi/IP.</li>';
  }
}
