/**
 * Lógica da tela Painel (index.html):
 * Carrega KPIs, casos críticos no topo, alertas ativos e Mapa Territorial (módulo FloodGuard).
 */
document.addEventListener('DOMContentLoaded', () => {
    carregarPainel();
});

let regioesMapaCache = [];

async function carregarPainel() {
    try {
        const dados = await PlantaoApi.get('/api/painel');
        renderizarKpis(dados.resumo);
        renderizarCriticos(dados.casosCriticos || []);
        renderizarAlertas(dados.alertas || []);
        regioesMapaCache = dados.mapaRegioes || [];
        renderizarMapaRegioes(regioesMapaCache);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackPainel', 'Erro ao carregar painel: ' + erro.message, 'danger');
    }
}

function renderizarKpis(r) {
    document.getElementById('kpiIdosos').textContent = r.totalIdosos ?? 0;
    document.getElementById('kpiVoluntarios').textContent = r.totalVoluntarios ?? 0;
    document.getElementById('kpiCobertura4h').textContent = `${r.percentualChecados4h ?? 0}%`;
    document.getElementById('kpiPendentes').textContent = r.pendentes ?? 0;
    document.getElementById('kpiCriticos').textContent = (r.casosCriticos ?? 0) + (r.precisamAjuda ?? 0);
    document.getElementById('kpiRedeIncompleta').textContent = r.idososRedeIncompleta ?? 0;
}

function renderizarCriticos(criticos) {
    const sec = document.getElementById('secCriticos');
    const lista = document.getElementById('listaCriticos');
    if (!criticos.length) {
        sec.classList.add('d-none');
        return;
    }
    sec.classList.remove('d-none');
    const colClass = criticos.length === 1 ? 'col-12' : 'col-lg-6';
    lista.innerHTML = criticos.map(c => `
        <div class="${colClass}">
            <div class="critical-case-row d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                <div>
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <span class="badge ${c.status === 'CRITICO' ? 'badge-critico' : 'badge-ajuda'}">
                            ${c.status === 'CRITICO' ? 'CRÍTICO · SEM RESPOSTA' : 'PRECISA DE AJUDA'}
                        </span>
                        <span class="risk-pill high">Risco ${c.pontuacaoRisco}/100</span>
                        <span class="small text-muted fw-semibold"><i class="fas fa-map-pin me-1"></i>${PlantaoApi.escaparHtml(c.nomeRegiao)}</span>
                    </div>
                    <div class="fw-bold fs-5 text-dark">${PlantaoApi.escaparHtml(c.nomeIdoso)}</div>
                    <div class="small text-secondary mb-1">
                        <i class="fas fa-location-dot text-danger me-1"></i><strong>${PlantaoApi.escaparHtml(c.enderecoIdoso)}</strong>
                    </div>
                    <div class="small text-muted">${PlantaoApi.escaparHtml(c.observacao || '')}</div>
                </div>
                <div class="d-flex flex-wrap gap-2 flex-shrink-0">
                    <a href="${PlantaoApi.limparTelefoneHref(c.telefoneIdoso)}" class="btn btn-danger fw-semibold btn-sm px-3">
                        <i class="fas fa-phone me-1"></i>${PlantaoApi.escaparHtml(c.telefoneIdoso)}
                    </a>
                    <a href="alertas.html?alerta=${c.idAlerta}" class="btn btn-outline-danger fw-semibold btn-sm px-3">
                        Abrir na Rodada <i class="fas fa-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>
        </div>
    `).join('');
}

function renderizarAlertas(alertas) {
    const container = document.getElementById('listaAlertasPainel');
    if (!alertas.length) {
        container.innerHTML = '<div class="text-muted text-center py-3">Nenhum alerta climático registrado.</div>';
        return;
    }
    container.innerHTML = alertas.slice(0, 4).map(a => {
        const badgeTipo = a.tipo === 'CALOR'
            ? '<span class="badge badge-calor"><i class="fas fa-sun me-1"></i>CALOR</span>'
            : '<span class="badge badge-chuva"><i class="fas fa-cloud-showers-heavy me-1"></i>CHUVA</span>';
        const badgeStatus = a.status === 'ATIVO'
            ? '<span class="badge bg-danger">ATIVO</span>'
            : '<span class="badge bg-secondary">ENCERRADO</span>';
        return `
            <div class="border rounded-3 p-3 mb-2 ${a.status === 'ATIVO' ? 'bg-light border-danger-subtle' : 'bg-white'}">
                <div class="d-flex justify-content-between align-items-center gap-2 mb-1">
                    <div class="d-flex gap-1">${badgeTipo} ${badgeStatus}</div>
                    <small class="text-muted fw-medium">${PlantaoApi.escaparHtml(a.fonte)}</small>
                </div>
                <div class="fw-bold text-dark">${PlantaoApi.escaparHtml(a.titulo)}</div>
                <div class="small text-muted mb-2">
                    <i class="fas fa-map-pin me-1"></i>${PlantaoApi.escaparHtml(a.nomeRegiao)} · Emitido em ${PlantaoApi.formatarDataHora(a.emitidoEm)}
                </div>
                <a href="alertas.html?alerta=${a.id}" class="btn btn-sm ${a.status === 'ATIVO' ? 'btn-primary' : 'btn-outline-secondary'} rounded-pill px-3">
                    <i class="fas fa-list-check me-1"></i>Ver Rodada de Ligações
                </a>
            </div>
        `;
    }).join('');
}

function renderizarMapaRegioes(regioes) {
    const grid = document.getElementById('mapaRegioes');
    if (!regioes.length) {
        grid.innerHTML = '<div class="text-muted">Nenhuma região monitorada.</div>';
        return;
    }
    grid.innerHTML = regioes.map(r => {
        const criticos = (r.casosCriticosAtivos || 0) + (r.casosAjudaAtivos || 0);
        const nivel = criticos > 0 ? 'level-critical'
            : (r.pendentesAtivos > 0 ? 'level-warning' : 'level-safe');
        const iconeNivel = criticos > 0 ? 'fa-triangle-exclamation text-danger'
            : (r.pendentesAtivos > 0 ? 'fa-clock text-warning' : 'fa-circle-check text-success');
        return `
            <div class="region-tile ${nivel}" onclick="abrirDetalheRegiao(${r.idRegiao})" role="button" tabindex="0">
                <div class="d-flex justify-content-between align-items-start mb-1">
                    <strong class="lh-sm text-dark">${PlantaoApi.escaparHtml(r.nomeRegiao)}</strong>
                    <i class="fas ${iconeNivel} fs-6"></i>
                </div>
                <div class="small text-muted mb-2">${PlantaoApi.escaparHtml(r.bairroZona)}</div>
                <div class="d-flex flex-wrap gap-1 mb-2">
                    <span class="badge badge-calor">Calor: ${r.riscoCalor}</span>
                    <span class="badge badge-chuva">Chuva: ${r.riscoChuva}</span>
                </div>
                <div class="d-flex justify-content-between small fw-semibold border-top pt-2 mt-1">
                    <span class="text-secondary"><i class="fas fa-users me-1"></i>${r.totalIdosos} idoso(s)</span>
                    <span class="${criticos > 0 ? 'text-danger' : 'text-muted'}">
                        ${criticos > 0 ? `${criticos} crítico(s)` : `${r.pendentesAtivos} pend.`}
                    </span>
                </div>
            </div>
        `;
    }).join('');
}

function abrirDetalheRegiao(idRegiao) {
    const r = regioesMapaCache.find(x => x.idRegiao === idRegiao);
    if (!r) return;
    document.getElementById('modalRegiaoTitulo').textContent = `${r.nomeRegiao} — ${r.bairroZona}`;
    document.getElementById('modalRegiaoCorpo').innerHTML = `
        <p class="small text-muted mb-3">
            <strong>UBS de referência:</strong> ${PlantaoApi.escaparHtml(r.referenciaUbs)}<br>
            <em>Dados agregados e anonimizados (módulo territorial herdado do FloodGuard).</em>
        </p>
        <div class="row g-2 text-center mb-3">
            <div class="col-4"><div class="p-2 bg-light rounded-3 border"><div class="h5 fw-bold mb-0">${r.totalIdosos}</div><small class="text-muted">Idosos</small></div></div>
            <div class="col-4"><div class="p-2 bg-danger-subtle rounded-3 border"><div class="h5 fw-bold text-danger mb-0">${r.casosCriticosAtivos + r.casosAjudaAtivos}</div><small class="text-danger">Críticos/Ajuda</small></div></div>
            <div class="col-4"><div class="p-2 bg-warning-subtle rounded-3 border"><div class="h5 fw-bold text-warning mb-0">${r.pendentesAtivos}</div><small class="text-warning">Pendentes</small></div></div>
        </div>
        <ul class="list-group list-group-flush small">
            <li class="list-group-item d-flex justify-content-between"><span>Com doença crônica</span><strong>${r.comDoencaCronica}</strong></li>
            <li class="list-group-item d-flex justify-content-between"><span>Casa sem ventilação</span><strong>${r.semVentilacao}</strong></li>
            <li class="list-group-item d-flex justify-content-between"><span>Casa em área de risco (enchente/encosta)</span><strong>${r.emAreaRisco}</strong></li>
            <li class="list-group-item d-flex justify-content-between"><span>Mobilidade reduzida</span><strong>${r.comMobilidadeReduzida}</strong></li>
        </ul>
    `;
    new bootstrap.Modal(document.getElementById('modalRegiao')).show();
}
