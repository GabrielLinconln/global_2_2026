/**
 * Logica da tela de Idosos (idosos.html): CRUD completo com validacao em JS.
 */
let idososCache = [];
let regioesCache = [];

document.addEventListener('DOMContentLoaded', async () => {
    await carregarRegioes();
    await carregarIdosos();

    document.getElementById('formIdoso').addEventListener('submit', salvarIdoso);
    document.getElementById('btnCancelarEdicao').addEventListener('click', limparFormulario);
    document.getElementById('filtroBusca').addEventListener('input', renderizarTabela);
    document.getElementById('filtroRegiao').addEventListener('change', renderizarTabela);
});

async function carregarRegioes() {
    try {
        regioesCache = await PlantaoApi.get('/api/regioes');
        const selForm = document.getElementById('idRegiao');
        const selFiltro = document.getElementById('filtroRegiao');
        const opts = regioesCache.map(r => `<option value="${r.id}">${PlantaoApi.escaparHtml(r.nome)} (${PlantaoApi.escaparHtml(r.bairroZona)})</option>`).join('');
        selForm.innerHTML = '<option value="">Selecione a regiao...</option>' + opts;
        selFiltro.innerHTML = '<option value="">Todas as regioes</option>' + opts;
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackIdosos', 'Erro ao carregar regioes: ' + erro.message, 'danger');
    }
}

async function carregarIdosos() {
    try {
        idososCache = await PlantaoApi.get('/api/idosos');
        renderizarTabela();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackIdosos', 'Erro ao carregar idosos: ' + erro.message, 'danger');
    }
}

function renderizarTabela() {
    const busca = document.getElementById('filtroBusca').value.trim().toLowerCase();
    const regiaoId = document.getElementById('filtroRegiao').value;

    const filtrados = idososCache.filter(i => {
        const matchTexto = !busca
            || i.nome.toLowerCase().includes(busca)
            || (i.enderecoResumo || '').toLowerCase().includes(busca)
            || (i.telefone || '').toLowerCase().includes(busca);
        const matchRegiao = !regiaoId || String(i.idRegiao) === regiaoId;
        return matchTexto && matchRegiao;
    });

    document.getElementById('totalIdososLista').textContent = filtrados.length;
    const tbody = document.getElementById('tabelaIdosos');

    if (!filtrados.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted">Nenhum idoso encontrado com os filtros aplicados.</td></tr>';
        return;
    }

    tbody.innerHTML = filtrados.map(i => {
        const tags = [];
        if (i.doencaCronica) tags.push('<span class="vuln-tag"><i class="fas fa-heart-pulse text-danger me-1"></i>Cronica</span>');
        if (i.casaSemVentilacao) tags.push('<span class="vuln-tag"><i class="fas fa-fan text-warning me-1"></i>Sem ventilacao</span>');
        if (i.areaRisco) tags.push('<span class="vuln-tag"><i class="fas fa-house-flood-water text-primary me-1"></i>Area risco</span>');
        if (i.mobilidadeReduzida) tags.push('<span class="vuln-tag"><i class="fas fa-wheelchair text-info me-1"></i>Mob. reduzida</span>');

        return `
            <tr>
                <td>
                    <div class="fw-bold">${PlantaoApi.escaparHtml(i.nome)}</div>
                    <div class="small text-muted">
                        Nasc. ${i.anoNascimento} (~${i.idadeEstimada} anos) ·
                        <a href="${PlantaoApi.limparTelefoneHref(i.telefone)}" class="text-decoration-none">${PlantaoApi.escaparHtml(i.telefone)}</a>
                    </div>
                    <div class="small text-muted">${PlantaoApi.escaparHtml(i.enderecoResumo)}</div>
                </td>
                <td>
                    <div class="fw-semibold small mb-1">${PlantaoApi.escaparHtml(i.nomeRegiao)}</div>
                    <div>${tags.length ? tags.join('') : '<span class="small text-muted">Sem fatores adicionais</span>'}</div>
                </td>
                <td class="text-center">${renderPillRisco(i.pontuacaoCalor)}</td>
                <td class="text-center">${renderPillRisco(i.pontuacaoChuva)}</td>
                <td class="text-end text-nowrap">
                    <button type="button" class="btn btn-sm btn-outline-primary me-1" onclick="editarIdoso(${i.id})" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="excluirIdoso(${i.id}, '${PlantaoApi.escaparHtml(i.nome)}')" title="Excluir">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderPillRisco(score) {
    const cls = score >= 75 ? 'high' : score >= 45 ? 'medium' : 'low';
    return `<span class="risk-pill ${cls}">${score}/100</span>`;
}

function validarFormulario() {
    const form = document.getElementById('formIdoso');
    const nome = document.getElementById('nome');
    const tel = document.getElementById('telefone');
    const ano = document.getElementById('anoNascimento');
    const regiao = document.getElementById('idRegiao');
    const end = document.getElementById('enderecoResumo');
    const lgpd = document.getElementById('consentimentoLgpd');

    let valido = true;
    const marcar = (el, ok) => {
        el.classList.toggle('is-invalid', !ok);
        el.classList.toggle('is-valid', ok);
        if (!ok) valido = false;
    };

    marcar(nome, nome.value.trim().length >= 3);
    marcar(tel, tel.value.replace(/\D/g, '').length >= 8);
    const anoNum = Number(ano.value);
    marcar(ano, anoNum >= 1900 && anoNum <= 1975);
    marcar(regiao, regiao.value !== '');
    marcar(end, end.value.trim().length >= 5);
    marcar(lgpd, lgpd.checked);

    form.classList.add('was-validated-custom');
    return valido;
}

async function salvarIdoso(e) {
    e.preventDefault();
    if (!validarFormulario()) {
        PlantaoApi.mostrarFeedback('feedbackIdosos', 'Verifique os campos destacados em vermelho antes de salvar.', 'warning');
        return;
    }

    const id = document.getElementById('idosoId').value;
    const payload = {
        nome: document.getElementById('nome').value.trim(),
        telefone: document.getElementById('telefone').value.trim(),
        anoNascimento: Number(document.getElementById('anoNascimento').value),
        idRegiao: Number(document.getElementById('idRegiao').value),
        enderecoResumo: document.getElementById('enderecoResumo').value.trim(),
        doencaCronica: document.getElementById('doencaCronica').checked,
        casaSemVentilacao: document.getElementById('casaSemVentilacao').checked,
        areaRisco: document.getElementById('areaRisco').checked,
        mobilidadeReduzida: document.getElementById('mobilidadeReduzida').checked,
        consentimentoLgpd: document.getElementById('consentimentoLgpd').checked,
        observacoes: document.getElementById('observacoes').value.trim(),
        ativo: true
    };

    try {
        if (id) {
            await PlantaoApi.put(`/api/idosos/${id}`, payload);
            PlantaoApi.mostrarFeedback('feedbackIdosos', 'Dados do idoso atualizados no banco com sucesso!', 'success');
        } else {
            await PlantaoApi.post('/api/idosos', payload);
            PlantaoApi.mostrarFeedback('feedbackIdosos', 'Idoso cadastrado com sucesso! Agora vincule um Titular e um Reserva na aba Voluntarios.', 'success');
        }
        limparFormulario();
        await carregarIdosos();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackIdosos', erro.message, 'danger');
    }
}

function editarIdoso(id) {
    const i = idososCache.find(x => x.id === id);
    if (!i) return;
    document.getElementById('idosoId').value = i.id;
    document.getElementById('nome').value = i.nome;
    document.getElementById('telefone').value = i.telefone;
    document.getElementById('anoNascimento').value = i.anoNascimento;
    document.getElementById('idRegiao').value = i.idRegiao;
    document.getElementById('enderecoResumo').value = i.enderecoResumo;
    document.getElementById('doencaCronica').checked = !!i.doencaCronica;
    document.getElementById('casaSemVentilacao').checked = !!i.casaSemVentilacao;
    document.getElementById('areaRisco').checked = !!i.areaRisco;
    document.getElementById('mobilidadeReduzida').checked = !!i.mobilidadeReduzida;
    document.getElementById('consentimentoLgpd').checked = !!i.consentimentoLgpd;
    document.getElementById('observacoes').value = i.observacoes || '';

    document.getElementById('formIdosoTitulo').innerHTML = '<i class="fas fa-pen text-warning me-2"></i>Editando Idoso #' + i.id;
    document.getElementById('btnSalvarTexto').textContent = 'Atualizar Idoso';
    document.getElementById('btnCancelarEdicao').classList.remove('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function limparFormulario() {
    const form = document.getElementById('formIdoso');
    form.reset();
    document.getElementById('idosoId').value = '';
    form.querySelectorAll('.is-valid, .is-invalid').forEach(el => el.classList.remove('is-valid', 'is-invalid'));
    document.getElementById('formIdosoTitulo').innerHTML = '<i class="fas fa-user-plus text-primary me-2"></i>Cadastrar Novo Idoso';
    document.getElementById('btnSalvarTexto').textContent = 'Salvar Idoso';
    document.getElementById('btnCancelarEdicao').classList.add('d-none');
}

async function excluirIdoso(id, nome) {
    if (!window.confirm(`Deseja realmente excluir o cadastro de ${nome}?`)) return;
    try {
        await PlantaoApi.del(`/api/idosos/${id}`);
        PlantaoApi.mostrarFeedback('feedbackIdosos', `Cadastro de ${nome} removido com sucesso.`, 'info');
        await carregarIdosos();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackIdosos', erro.message, 'danger');
    }
}
