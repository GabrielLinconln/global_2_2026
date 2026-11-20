// endereco da API: o proprio servidor, ou localhost:8080 se o index.html foi aberto direto do disco
const API_URL = location.protocol === 'file:' ? 'http://localhost:8080' : '';

const PAGINAS = [
  ['index.html', 'Painel do dia'],
  ['idosos.html', 'Idosos'],
  ['voluntarios.html', 'Voluntários'],
  ['alertas.html', 'Alertas'],
  ['relatorios.html', 'Relatórios'],
  ['como-funciona.html', 'Como funciona']
];

const ICONE_TEL = '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M6.62 10.79a15.1 15.1 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.02-.24c1.12.37 2.33.57 3.57.57a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1.02z"/></svg>';

// acessibilidade: tamanho da letra e alto contraste ficam salvos no navegador
const TAMANHOS = ['100%', '112.5%', '125%', '137.5%'];
function aplicarAjustes() {
  const raiz = document.documentElement;
  raiz.style.fontSize = TAMANHOS[+(localStorage.getItem('tamanho') || 0)];
  raiz.classList.toggle('contraste', localStorage.getItem('contraste') === 'S');
}
function mudarTamanho(passo) {
  const t = Math.min(TAMANHOS.length - 1, Math.max(0, +(localStorage.getItem('tamanho') || 0) + passo));
  localStorage.setItem('tamanho', t);
  aplicarAjustes();
}
function alternarContraste(botao) {
  const ligado = localStorage.getItem('contraste') !== 'S';
  localStorage.setItem('contraste', ligado ? 'S' : 'N');
  botao.setAttribute('aria-pressed', ligado);
  aplicarAjustes();
}
aplicarAjustes();

function montarMenu() {
  const principal = document.querySelector('main');
  if (principal) { principal.id = principal.id || 'principal'; principal.tabIndex = -1; }
  const atual = location.pathname.split('/').pop() || 'index.html';
  const itens = PAGINAS.map(([href, nome]) =>
    `<li class="nav-item"><a class="nav-link${href === atual ? ' active' : ''}" ${href === atual ? 'aria-current="page"' : ''} href="${href}">${nome}</a></li>`).join('');
  document.getElementById('menu').innerHTML = `
    <a class="pular" href="#${principal ? principal.id : 'menu'}">Pular para o conteúdo</a>
    <div class="ajustes no-print" role="group" aria-label="Acessibilidade">
      <div class="container">
        <span id="conexao" class="conexao" role="status">Conectando</span>
        <span>Acessibilidade</span>
        <button type="button" onclick="mudarTamanho(-1)" aria-label="Diminuir letra">A−</button>
        <button type="button" onclick="mudarTamanho(1)" aria-label="Aumentar letra">A+</button>
        <button type="button" onclick="alternarContraste(this)" aria-pressed="${localStorage.getItem('contraste') === 'S'}">Alto contraste</button>
      </div>
    </div>
    <nav class="navbar navbar-expand-lg topo no-print" aria-label="Principal">
      <div class="container">
        <a class="marca" href="index.html">
          <svg viewBox="0 0 34 34" aria-hidden="true"><rect width="34" height="34" rx="9" fill="#14304A"/><circle cx="25.5" cy="8.5" r="4" fill="#F6C83B"/>
            <g transform="translate(5 8) scale(.85)" fill="#fff"><path d="M6.62 10.79a15.1 15.1 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.02-.24c1.12.37 2.33.57 3.57.57a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1.02z"/></g></svg>
          Plantão Clima</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav" aria-controls="nav" aria-expanded="false" aria-label="Abrir menu">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="nav">
          <ul class="navbar-nav ms-auto">${itens}</ul>
        </div>
      </div>
    </nav>`;
}

// mostra no topo se o servidor Java + H2 esta respondendo
function marcarConexao(ok) {
  const el = document.getElementById('conexao');
  if (!el) return;
  el.className = 'conexao ' + (ok ? 'ok' : 'off');
  el.textContent = ok ? 'Servidor Java + H2 no ar' : 'Servidor desligado';
}

// GET quando nao ha dados; POST com formulario (sem preflight de CORS)
// um POST por vez: o duplo clique nao grava duas vezes
let enviando = false;
async function api(caminho, dados) {
  if (dados && enviando) throw new Error('Aguarde: o envio anterior ainda não terminou.');
  if (dados) enviando = true;
  try {
    const opcoes = dados ? { method: 'POST', body: new URLSearchParams(dados) } : {};
    let resp, json;
    try {
      resp = await fetch(API_URL + '/api/' + caminho, opcoes);
      json = await resp.json(); // resposta sem JSON (ex.: site estático) = servidor Java fora do ar
    } catch (e) {
      marcarConexao(false);
      avisar('Servidor desligado. Inicie o sistema (App.java) e recarregue a página.', 'danger');
      throw new Error('Failed to fetch');
    }
    marcarConexao(true);
    if (!resp.ok) throw new Error(json.erro || 'Erro no servidor');
    return json;
  } finally {
    if (dados) enviando = false;
  }
}

function avisar(msg, tipo = 'success') {
  const area = document.getElementById('avisos');
  area.innerHTML = `<div class="alert alert-${tipo} alert-dismissible" role="alert">${esc(msg)}
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button></div>`;
  area.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// executa uma acao e mostra o erro do servidor sem quebrar a pagina
async function tentar(acao, msgOk) {
  try {
    const r = await acao();
    if (msgOk) avisar(msgOk);
    return r;
  } catch (e) {
    if (e.message !== 'Failed to fetch') avisar(e.message, 'danger');
  }
}

function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function hoje() {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function dataBr(iso) {
  if (!iso) return '';
  const [d, h] = String(iso).split('T');
  const [a, m, dia] = d.split('-');
  return `${dia}/${m}/${a}` + (h ? ' ' + h.slice(0, 5) : '');
}

function soDigitos(v) {
  return String(v || '').replace(/\D/g, '');
}

// dados do formulario; checkbox vira S/N
function dadosForm(form) {
  const dados = {};
  for (const el of form.elements) {
    if (!el.name) continue;
    dados[el.name] = el.type === 'checkbox' ? (el.checked ? 'S' : 'N') : el.value.trim();
  }
  return dados;
}

function preencherForm(form, obj) {
  for (const el of form.elements) {
    if (!el.name || !(el.name in obj)) continue;
    if (el.type === 'checkbox') el.checked = obj[el.name] === true;
    else el.value = obj[el.name] ?? '';
  }
}

// marca o campo invalido com a mensagem embaixo dele
function erroCampo(form, nome, msg) {
  const el = form.elements[nome];
  el.classList.add('is-invalid');
  let fb = el.parentElement.querySelector('.invalid-feedback');
  if (!fb) {
    fb = document.createElement('div');
    fb.className = 'invalid-feedback';
    el.parentElement.appendChild(fb);
  }
  fb.textContent = msg;
  return false;
}

function limparErros(form) {
  form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
}

function opcoes(select, lista, valor, texto, vazio) {
  select.innerHTML = (vazio ? `<option value="">${esc(vazio)}</option>` : '') +
    lista.map(o => `<option value="${esc(o[valor])}">${esc(texto(o))}</option>`).join('');
}

// tabela simples a partir de linhas (relatorios)
function tabela(el, linhas, colunas) {
  if (!linhas.length) {
    el.innerHTML = '<p class="text-body-secondary">Nada para mostrar.</p>';
    return;
  }
  el.innerHTML = `<div class="table-responsive"><table class="table table-striped align-middle">
    <thead><tr>${colunas.map(c => `<th scope="col">${esc(c[1])}</th>`).join('')}</tr></thead>
    <tbody>${linhas.map(l => `<tr>${colunas.map(c => `<td>${esc(c[2] ? c[2](l[c[0]], l) : l[c[0]] ?? '-')}</td>`).join('')}</tr>`).join('')}</tbody>
  </table></div>`;
}

const ROTULO_RISCO = { ALTO: 'Risco alto', MEDIO: 'Risco médio', BAIXO: 'Risco baixo' };
const COR_RISCO = { ALTO: 'danger', MEDIO: 'warning', BAIXO: 'secondary' };

function badgeRisco(r) {
  return `<span class="badge text-bg-${COR_RISCO[r]}">${ROTULO_RISCO[r]}</span>`;
}

document.addEventListener('DOMContentLoaded', montarMenu);
