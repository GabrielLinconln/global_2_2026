/**
 * Lógica da tela Painel (index.html):
 * Carrega indicadores, casos críticos no topo, alertas ativos e situação territorial (módulo FloodGuard).
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
    lista.innerHTML = criticos.map(c => `
        <div class="critical-row">
            <div>
                <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                    <span class="tag-status ${c.status === 'CRITICO' ? 'badge-critico' : 'badge-ajuda'}">
                        ${c.status === 'CRITICO' ? 'CRÍTICO · SEM RESPOSTA' : 'PEDIDO DE AJUDA'}
                    </span>
                    <span class="risk-pill high">Risco ${c.pontuacaoRisco}/100</span>
                    <span class="small text-muted">${PlantaoApi.escaparHtml(c.nomeRegiao)}</span>
                </div>
                <div class="fw-semibold fs-6 text-dark">${PlantaoApi.escaparHtml(c.nomeIdoso)}</div>
                <div class="small text-secondary">${PlantaoApi.escaparHtml(c.enderecoIdoso)}</div>
                <div class="small text-muted mt-1">${PlantaoApi.escaparHtml(c.observacao || '')}</div>
            </div>
            <div class="d-flex flex-wrap gap-2 flex-shrink-0">
                <a href="${PlantaoApi.limparTelefoneHref(c.telefoneIdoso)}" class="btn btn-sm btn-danger mono">
                    Ligar ${PlantaoApi.escaparHtml(c.telefoneIdoso)}
                </a>
                <a href="alertas.html?alerta=${c.idAlerta}" class="btn btn-sm btn-paper">
                    Abrir na rodada &rarr;
                </a>
            </div>
        </div>
    `).join('');
}

function renderizarAlertas(alertas) {
    const container = document.getElementById('listaAlertasPainel');
    if (!alertas.length) {
        container.innerHTML = '<div class="p-4 text-muted text-center small">Nenhum alerta climático registrado.</div>';
        return;
    }
    container.innerHTML = alertas.slice(0, 4).map((a, idx) => {
        const badgeTipo = a.tipo === 'CALOR'
            ? '<span class="tag-status badge-calor">CALOR</span>'
            : '<span class="tag-status badge-chuva">CHUVA</span>';
        const badgeStatus = a.status === 'ATIVO'
            ? '<span class="tag-status badge-critico">ATIVO</span>'
            : '<span class="tag-status badge-pendente">ENCERRADO</span>';
        const borderClass = idx < Math.min(alertas.length, 4) - 1 ? 'border-bottom' : '';
        return `
            <div class="p-3 ${borderClass}">
                <div class="d-flex justify-content-between align-items-center gap-2 mb-1">
                    <div class="d-flex gap-1">${badgeTipo} ${badgeStatus}</div>
                    <span class="mono small text-muted">${PlantaoApi.escaparHtml(a.fonte)}</span>
                </div>
                <div class="fw-semibold text-dark">${PlantaoApi.escaparHtml(a.titulo)}</div>
                <div class="small text-muted mb-2">
                    ${PlantaoApi.escaparHtml(a.nomeRegiao)} · ${PlantaoApi.formatarDataHora(a.emitidoEm)}
                </div>
                <a href="alertas.html?alerta=${a.id}" class="btn btn-sm ${a.status === 'ATIVO' ? 'btn-ink' : 'btn-paper'}">
                    Abrir rodada
                </a>
            </div>
        `;
    }).join('');
}

function renderizarMapaRegioes(regioes) {
    const grid = document.getElementById('mapaRegioes');
    if (!regioes.length) {
        grid.innerHTML = '<div class="text-muted small">Nenhuma região monitorada.</div>';
        return;
    }
    grid.innerHTML = regioes.map(r => {
        const criticos = (r.casosCriticosAtivos || 0) + (r.casosAjudaAtivos || 0);
        const nivel = criticos > 0 ? 'level-critical'
            : (r.pendentesAtivos > 0 ? 'level-warning' : 'level-safe');
        const statusTxt = criticos > 0
            ? `<span class="mono fw-semibold text-danger">${criticos} crítico(s)</span>`
            : r.pendentesAtivos > 0
                ? `<span class="mono text-secondary">${r.pendentesAtivos} pend.</span>`
                : '<span class="mono text-muted">OK</span>';

        return `
            <div class="region-tile ${nivel}" onclick="abrirDetalheRegiao(${r.idRegiao})" role="button" tabindex="0">
                <div class="d-flex justify-content-between align-items-start gap-2 mb-1">
                    <div class="fw-semibold text-dark lh-sm">${PlantaoApi.escaparHtml(r.nomeRegiao)}</div>
                    ${statusTxt}
                </div>
                <div class="small text-muted mb-2">${PlantaoApi.escaparHtml(r.bairroZona)}</div>
                <div class="mono text-secondary" style="font-size:0.72rem;">
                    CALOR: <strong>${r.riscoCalor}</strong> · CHUVA: <strong>${r.riscoChuva}</strong>
                </div>
                <div class="small text-muted mt-2 pt-2 border-top">
                    ${r.totalIdosos} idoso(s) acompanhado(s)
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
        <p class="small text-secondary mb-3">
            <strong>UBS de referência:</strong> ${PlantaoApi.escaparHtml(r.referenciaUbs)}<br>
            Dados consolidados por território (sem identificação nominal).
        </p>
        <div class="row g-2 text-center mb-3">
            <div class="col-4"><div class="p-2 border rounded bg-light"><div class="mono h5 fw-bold mb-0">${r.totalIdosos}</div><small class="text-muted">Idosos</small></div></div>
            <div class="col-4"><div class="p-2 border rounded" style="background:#fef2f2;"><div class="mono h5 fw-bold text-danger mb-0">${r.casosCriticosAtivos + r.casosAjudaAtivos}</div><small class="text-danger">Críticos</small></div></div>
            <div class="col-4"><div class="p-2 border rounded bg-light"><div class="mono h5 fw-bold mb-0">${r.pendentesAtivos}</div><small class="text-muted">Pendentes</small></div></div>
        </div>
        <ul class="list-group list-group-flush small">
            <li class="list-group-item d-flex justify-content-between px-0"><span>Com doença crônica</span><strong class="mono">${r.comDoencaCronica}</strong></li>
            <li class="list-group-item d-flex justify-content-between px-0"><span>Casa sem ventilação</span><strong class="mono">${r.semVentilacao}</strong></li>
            <li class="list-group-item d-flex justify-content-between px-0"><span>Área de risco (enchente/encosta)</span><strong class="mono">${r.emAreaRisco}</strong></li>
            <li class="list-group-item d-flex justify-content-between px-0"><span>Mobilidade reduzida</span><strong class="mono">${r.comMobilidadeReduzida}</strong></li>
        </ul>
    `;
    new bootstrap.Modal(document.getElementById('modalRegiao')).show();
}
