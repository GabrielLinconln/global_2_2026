/**
 * Logica da tela de Alertas e Rodada de Ligacoes (alertas.html).
 * Permite registrar alertas (CALOR/CHUVA), gerar a fila ordenada por risco,
 * preencher checklists de orientacao, acionar 192/193/199 e executar escalonamento automatico.
 */
let alertasCache = [];
let alertaSelecionadoId = null;
let rodadaCache = [];
let filtroStatusAtual = 'TODOS';

const CHECKLIST_CALOR = [
    'Orientado a beber agua fresca com frequencia',
    'Casa muito abafada / sem ventilacao adequada',
    'Ventilador ou janela aberta verificados',
    'Relatou tontura, fraqueza ou dor de cabeca',
    'Medicacoes de uso continuo em dia'
];

const CHECKLIST_CHUVA = [
    'Rua ou acesso com risco de alagamento',
    'Agua entrando na residencia ou goteira grave',
    'Documentos e remedios separados em local seco',
    'Orientado sobre ponto seguro mais proximo',
    'Energia eletrica ou telefone estaveis'
];

document.addEventListener('DOMContentLoaded', async () => {
    await carregarRegioesSelect();
    await carregarAlertas();

    document.getElementById('formAlerta').addEventListener('submit', criarAlerta);
    document.getElementById('formChecklist').addEventListener('submit', confirmarChecklist);
    document.getElementById('formAjuda').addEventListener('submit', confirmarAjuda);

    document.querySelectorAll('[data-filtro]').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('[data-filtro]').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            filtroStatusAtual = btn.dataset.filtro;
            renderizarRodada();
        });
    });
});

async function carregarRegioesSelect() {
    try {
        const regioes = await PlantaoApi.get('/api/regioes');
        document.getElementById('alertaRegiao').innerHTML = '<option value="">Todas as regioes</option>' +
            regioes.map(r => `<option value="${r.id}">${PlantaoApi.escaparHtml(r.nome)}</option>`).join('');
    } catch (_) {
        // silencioso
    }
}

async function carregarAlertas(forcarId = null) {
    try {
        alertasCache = await PlantaoApi.get('/api/alertas');
        const params = new URLSearchParams(window.location.search);
        const idQuery = params.get('alerta') ? Number(params.get('alerta')) : null;

        if (forcarId) {
            alertaSelecionadoId = forcarId;
        } else if (idQuery && alertasCache.some(a => a.id === idQuery)) {
            alertaSelecionadoId = idQuery;
        } else if (!alertaSelecionadoId || !alertasCache.some(a => a.id === alertaSelecionadoId)) {
            alertaSelecionadoId = alertasCache.length ? alertasCache[0].id : null;
        }

        renderizarListaAlertas();
        if (alertaSelecionadoId) {
            await carregarRodada(alertaSelecionadoId);
        } else {
            document.getElementById('listaRodada').innerHTML =
                '<div class="pc-card p-4 text-center text-muted">Emita um alerta ao lado para gerar automaticamente a rodada de ligacoes.</div>';
        }
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', 'Erro ao carregar alertas: ' + erro.message, 'danger');
    }
}

function renderizarListaAlertas() {
    const el = document.getElementById('listaAlertas');
    if (!alertasCache.length) {
        el.innerHTML = '<div class="text-muted text-center py-3">Nenhum alerta registrado.</div>';
        return;
    }

    el.innerHTML = alertasCache.map(a => {
        const ativo = a.id === alertaSelecionadoId;
        const badgeTipo = a.tipo === 'CALOR'
            ? '<span class="badge badge-calor"><i class="fas fa-sun me-1"></i>CALOR</span>'
            : '<span class="badge badge-chuva"><i class="fas fa-cloud-showers-heavy me-1"></i>CHUVA</span>';
        return `
            <div class="border rounded-3 p-2 ${ativo ? 'border-primary border-2 bg-primary-subtle' : 'bg-white'}" role="button" onclick="selecionarAlerta(${a.id})">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <div class="d-flex gap-1">${badgeTipo}
                        <span class="badge ${a.status === 'ATIVO' ? 'bg-danger' : 'bg-secondary'}">${a.status}</span>
                    </div>
                    <small class="text-muted">${PlantaoApi.escaparHtml(a.severidade)}</small>
                </div>
                <div class="fw-bold small">${PlantaoApi.escaparHtml(a.titulo)}</div>
                <div class="text-muted" style="font-size:0.75rem;">${PlantaoApi.escaparHtml(a.fonte)} · ${PlantaoApi.escaparHtml(a.nomeRegiao)}</div>
            </div>
        `;
    }).join('');
}

async function selecionarAlerta(id) {
    alertaSelecionadoId = id;
    renderizarListaAlertas();
    await carregarRodada(id);
}

async function carregarRodada(idAlerta) {
    try {
        rodadaCache = await PlantaoApi.get(`/api/alertas/${idAlerta}/rodada`);
        renderizarCabecalhoRodada();
        renderizarRodada();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', 'Erro ao carregar rodada: ' + erro.message, 'danger');
    }
}

function renderizarCabecalhoRodada() {
    const a = alertasCache.find(x => x.id === alertaSelecionadoId);
    const el = document.getElementById('cabecalhoRodada');
    if (!a) return;

    const criterio = a.tipo === 'CALOR'
        ? 'Prioridade de CALOR: pesam doenca cronica (+35) e casa sem ventilacao (+30).'
        : 'Prioridade de CHUVA: pesam area de risco (+35) e mobilidade reduzida (+30).';

    el.innerHTML = `
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-2">
            <div>
                <div class="d-flex flex-wrap gap-1 mb-1">
                    <span class="badge ${a.tipo === 'CALOR' ? 'badge-calor' : 'badge-chuva'}">${a.tipo}</span>
                    <span class="badge bg-dark">${a.severidade}</span>
                    <span class="badge ${a.status === 'ATIVO' ? 'bg-danger' : 'bg-secondary'}">${a.status}</span>
                    <span class="badge bg-light text-dark border">${PlantaoApi.escaparHtml(a.fonte)}</span>
                </div>
                <h2 class="h5 fw-bold mb-1">${PlantaoApi.escaparHtml(a.titulo)}</h2>
                <p class="small text-muted mb-1">${PlantaoApi.escaparHtml(a.descricao)}</p>
                <div class="small text-primary fw-semibold"><i class="fas fa-sliders me-1"></i>${criterio}</div>
            </div>
            <div class="d-flex gap-2 no-print">
                ${a.status === 'ATIVO'
                    ? `<button type="button" class="btn btn-sm btn-outline-secondary" onclick="encerrarAlerta(${a.id})"><i class="fas fa-check-double me-1"></i>Encerrar Alerta</button>`
                    : ''}
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="excluirAlerta(${a.id})" title="Excluir alerta">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `;
}

function renderizarRodada() {
    const container = document.getElementById('listaRodada');
    const filtrados = rodadaCache.filter(c => {
        if (filtroStatusAtual === 'TODOS') return true;
        if (filtroStatusAtual === 'CRITICOS') return c.status === 'CRITICO' || c.status === 'PRECISA_AJUDA';
        return c.status === filtroStatusAtual;
    });

    if (!filtrados.length) {
        container.innerHTML = '<div class="pc-card p-4 text-center text-muted">Nenhuma ligacao neste filtro.</div>';
        return;
    }

    container.innerHTML = filtrados.map(c => {
        const badgeStatus = formatarBadgeStatus(c.status);
        const badgePapel = c.papelAtual === 'RESERVA'
            ? '<span class="badge bg-warning text-dark"><i class="fas fa-rotate me-1"></i>Escalonado p/ RESERVA</span>'
            : c.papelAtual === 'COORDENACAO'
                ? '<span class="badge bg-danger"><i class="fas fa-user-shield me-1"></i>COORDENACAO</span>'
                : '<span class="badge bg-primary-subtle text-primary">TITULAR</span>';

        const responsavel = c.nomeVoluntario
            ? `${PlantaoApi.escaparHtml(c.nomeVoluntario)} (${PlantaoApi.escaparHtml(c.telefoneVoluntario || '')})`
            : 'Coordenador comunitario (sem voluntario vinculado)';

        const fatores = [];
        if (c.doencaCronica) fatores.push('Doenca cronica');
        if (c.casaSemVentilacao) fatores.push('Casa sem ventilacao');
        if (c.areaRisco) fatores.push('Area de risco');
        if (c.mobilidadeReduzida) fatores.push('Mobilidade reduzida');

        return `
            <article class="call-card status-${c.status}">
                <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
                    <div>
                        <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                            <span class="risk-pill ${c.pontuacaoRisco >= 75 ? 'high' : c.pontuacaoRisco >= 45 ? 'medium' : 'low'}">
                                Risco ${c.pontuacaoRisco}/100
                            </span>
                            ${badgeStatus}
                            ${badgePapel}
                        </div>
                        <h3 class="h5 fw-bold mb-0">${PlantaoApi.escaparHtml(c.nomeIdoso)}</h3>
                        <div class="small text-muted">
                            Nasc. ${c.anoNascimentoIdoso} · ${PlantaoApi.escaparHtml(c.nomeRegiao)} · ${PlantaoApi.escaparHtml(c.enderecoIdoso)}
                        </div>
                    </div>
                    <div class="text-md-end">
                        <a href="${PlantaoApi.limparTelefoneHref(c.telefoneIdoso)}" class="btn btn-primary fw-bold btn-sm mb-1">
                            <i class="fas fa-phone me-1"></i>Ligar: ${PlantaoApi.escaparHtml(c.telefoneIdoso)}
                        </a>
                        <div class="small text-muted">Responsavel: <strong>${responsavel}</strong></div>
                    </div>
                </div>

                ${fatores.length ? `<div class="mb-2">${fatores.map(f => `<span class="vuln-tag">${f}</span>`).join('')}</div>` : ''}

                ${c.checklistResumo ? `<div class="alert alert-warning py-1 px-2 small mb-2"><strong>Checklist:</strong> ${PlantaoApi.escaparHtml(c.checklistResumo)}</div>` : ''}
                ${c.observacao ? `<div class="small text-muted mb-2"><i class="fas fa-comment-dots me-1"></i>${PlantaoApi.escaparHtml(c.observacao)}</div>` : ''}

                ${c.status === 'CRITICO' ? `
                    <div class="alert alert-danger py-2 px-3 small mb-2">
                        <strong><i class="fas fa-triangle-exclamation me-1"></i>ACAO IMEDIATA:</strong>
                        Enviar alguem ate o endereco (${PlantaoApi.escaparHtml(c.enderecoIdoso)}) e acionar
                        <a href="tel:193" class="fw-bold text-danger">193</a>,
                        <a href="tel:192" class="fw-bold text-danger">192</a> ou
                        <a href="tel:199" class="fw-bold text-danger">199</a>.
                    </div>
                ` : ''}

                <!-- Caixas de marcacao manual exclusivas para impressao offline -->
                <div class="print-only small border-top pt-2 mt-2">
                    [ ] Esta bem &nbsp;&nbsp; [ ] Precisa de atencao &nbsp;&nbsp; [ ] Precisa de ajuda (192) &nbsp;&nbsp; [ ] Nao atendeu (Passar p/ reserva)
                </div>

                <div class="call-action-grid mt-2 pt-2 border-top no-print">
                    <button type="button" class="btn btn-sm btn-outline-success fw-semibold" onclick="registrarDireto(${c.id}, 'ESTA_BEM')">
                        <i class="fas fa-check me-1"></i>Esta bem
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-warning fw-semibold text-dark" onclick="abrirModalChecklist(${c.id})">
                        <i class="fas fa-clipboard-list me-1"></i>Precisa atencao
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger fw-semibold" onclick="abrirModalAjuda(${c.id})">
                        <i class="fas fa-kit-medical me-1"></i>Precisa ajuda
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-dark fw-semibold" onclick="registrarDireto(${c.id}, 'NAO_ATENDEU')">
                        <i class="fas fa-phone-slash me-1"></i>Nao atendeu
                    </button>
                </div>
            </article>
        `;
    }).join('');
}

function formatarBadgeStatus(status) {
    switch (status) {
        case 'CRITICO': return '<span class="badge badge-critico"><i class="fas fa-triangle-exclamation me-1"></i>CRITICO</span>';
        case 'PRECISA_AJUDA': return '<span class="badge badge-ajuda"><i class="fas fa-ambulance me-1"></i>PRECISA AJUDA (192)</span>';
        case 'PRECISA_ATENCAO': return '<span class="badge badge-atencao"><i class="fas fa-eye me-1"></i>PRECISA ATENCAO</span>';
        case 'ESTA_BEM': return '<span class="badge badge-ok"><i class="fas fa-check-circle me-1"></i>ESTA BEM</span>';
        default: return '<span class="badge badge-pendente"><i class="fas fa-hourglass-half me-1"></i>PENDENTE</span>';
    }
}

async function criarAlerta(e) {
    e.preventDefault();
    const titulo = document.getElementById('alertaTitulo');
    const desc = document.getElementById('alertaDescricao');

    const okT = titulo.value.trim().length >= 5;
    const okD = desc.value.trim().length >= 10;
    titulo.classList.toggle('is-invalid', !okT);
    desc.classList.toggle('is-invalid', !okD);
    if (!okT || !okD) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', 'Preencha corretamente o titulo e a descricao do alerta.', 'warning');
        return;
    }

    const idReg = document.getElementById('alertaRegiao').value;
    try {
        const resp = await PlantaoApi.post('/api/alertas', {
            titulo: titulo.value.trim(),
            tipo: document.getElementById('alertaTipo').value,
            severidade: document.getElementById('alertaSeveridade').value,
            fonte: document.getElementById('alertaFonte').value,
            descricao: desc.value.trim(),
            idRegiao: idReg ? Number(idReg) : null
        });
        document.getElementById('formAlerta').reset();
        PlantaoApi.mostrarFeedback(
            'feedbackAlertas',
            `Alerta emitido! Rodada de ligacoes gerada automaticamente com ${resp.rodada.length} idoso(s) ordenados por risco.`,
            'success'
        );
        await carregarAlertas(resp.alerta.id);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', erro.message, 'danger');
    }
}

async function registrarDireto(idChecagem, status) {
    try {
        const obs = status === 'ESTA_BEM'
            ? 'Ligacao realizada com sucesso: idoso confirmou estar bem.'
            : 'Sem resposta na ligacao telefonica.';
        const resp = await PlantaoApi.post(`/api/checagens/${idChecagem}/registrar`, {
            status,
            observacao: obs
        });
        const tipoMsg = resp.checagem?.status === 'CRITICO' ? 'danger' : resp.escalonado ? 'warning' : 'success';
        PlantaoApi.mostrarFeedback('feedbackAlertas', resp.mensagem, tipoMsg);
        await carregarRodada(alertaSelecionadoId);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', erro.message, 'danger');
    }
}

function abrirModalChecklist(idChecagem) {
    const c = rodadaCache.find(x => x.id === idChecagem);
    if (!c) return;
    document.getElementById('chkIdChecagem').value = c.id;
    const isCalor = c.tipoAlerta === 'CALOR';
    document.getElementById('modalChecklistTitulo').innerHTML =
        `<i class="fas ${isCalor ? 'fa-sun text-warning' : 'fa-cloud-showers-heavy text-primary'} me-2"></i>Checklist de ${isCalor ? 'Calor Extremo' : 'Chuva Forte'}`;
    document.getElementById('chkSubtitulo').textContent = `Idoso: ${c.nomeIdoso} (${c.telefoneIdoso})`;

    const itens = isCalor ? CHECKLIST_CALOR : CHECKLIST_CHUVA;
    document.getElementById('chkItensContainer').innerHTML = itens.map((item, idx) => `
        <div class="form-check mb-2">
            <input class="form-check-input chk-item-box" type="checkbox" value="${PlantaoApi.escaparHtml(item)}" id="chkItem${idx}">
            <label class="form-check-label" for="chkItem${idx}">${PlantaoApi.escaparHtml(item)}</label>
        </div>
    `).join('');
    document.getElementById('chkObs').value = '';
    new bootstrap.Modal(document.getElementById('modalChecklist')).show();
}

async function confirmarChecklist(e) {
    e.preventDefault();
    const id = Number(document.getElementById('chkIdChecagem').value);
    const marcados = [...document.querySelectorAll('.chk-item-box:checked')].map(el => el.value);
    const obs = document.getElementById('chkObs').value.trim();

    try {
        const resp = await PlantaoApi.post(`/api/checagens/${id}/registrar`, {
            status: 'PRECISA_ATENCAO',
            checklistResumo: marcados.join('; ') || 'Orientacoes gerais repassadas',
            observacao: obs || 'Acompanhamento prioritario agendado.'
        });
        bootstrap.Modal.getInstance(document.getElementById('modalChecklist')).hide();
        PlantaoApi.mostrarFeedback('feedbackAlertas', resp.mensagem, 'warning');
        await carregarRodada(alertaSelecionadoId);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', erro.message, 'danger');
    }
}

function abrirModalAjuda(idChecagem) {
    const c = rodadaCache.find(x => x.id === idChecagem);
    if (!c) return;
    document.getElementById('ajudaIdChecagem').value = c.id;
    document.getElementById('ajudaEnderecoResumo').innerHTML =
        `<strong>Idoso:</strong> ${PlantaoApi.escaparHtml(c.nomeIdoso)} · <strong>Endereco:</strong> ${PlantaoApi.escaparHtml(c.enderecoIdoso)}`;
    document.getElementById('ajudaObs').value = '';
    new bootstrap.Modal(document.getElementById('modalAjuda')).show();
}

async function confirmarAjuda(e) {
    e.preventDefault();
    const id = Number(document.getElementById('ajudaIdChecagem').value);
    const obs = document.getElementById('ajudaObs').value.trim();
    if (!obs) return;

    try {
        const resp = await PlantaoApi.post(`/api/checagens/${id}/registrar`, {
            status: 'PRECISA_AJUDA',
            observacao: obs
        });
        bootstrap.Modal.getInstance(document.getElementById('modalAjuda')).hide();
        PlantaoApi.mostrarFeedback('feedbackAlertas', resp.mensagem, 'danger');
        await carregarRodada(alertaSelecionadoId);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', erro.message, 'danger');
    }
}

async function encerrarAlerta(id) {
    if (!window.confirm('Marcar este alerta climatico como ENCERRADO?')) return;
    try {
        await PlantaoApi.post(`/api/alertas/${id}/encerrar`, {});
        PlantaoApi.mostrarFeedback('feedbackAlertas', 'Alerta encerrado com sucesso.', 'info');
        await carregarAlertas(id);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', erro.message, 'danger');
    }
}

async function excluirAlerta(id) {
    if (!window.confirm('Excluir este alerta e todas as checagens associadas?')) return;
    try {
        await PlantaoApi.del(`/api/alertas/${id}`);
        PlantaoApi.mostrarFeedback('feedbackAlertas', 'Alerta excluido.', 'info');
        alertaSelecionadoId = null;
        await carregarAlertas();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackAlertas', erro.message, 'danger');
    }
}
