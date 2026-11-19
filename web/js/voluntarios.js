/**
 * Logica da tela de Voluntarios e Vinculos de Cuidado (voluntarios.html).
 */
let volRegioes = [];
let volIdosos = [];
let volLista = [];
let volVinculos = [];

document.addEventListener('DOMContentLoaded', async () => {
    await carregarTudo();
    document.getElementById('formVoluntario').addEventListener('submit', salvarVoluntario);
    document.getElementById('btnCancelarVol').addEventListener('click', limparFormVol);
    document.getElementById('formVinculo').addEventListener('submit', salvarVinculo);
});

async function carregarTudo() {
    try {
        const [regioes, idosos, voluntarios, vinculos] = await Promise.all([
            PlantaoApi.get('/api/regioes'),
            PlantaoApi.get('/api/idosos'),
            PlantaoApi.get('/api/voluntarios'),
            PlantaoApi.get('/api/vinculos')
        ]);
        volRegioes = regioes;
        volIdosos = idosos;
        volLista = voluntarios;
        volVinculos = vinculos;

        preencherSelects();
        renderizarRedePorIdoso();
        renderizarTabelaVoluntarios();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', 'Erro ao carregar dados: ' + erro.message, 'danger');
    }
}

function preencherSelects() {
    document.getElementById('volRegiao').innerHTML = '<option value="">Selecione...</option>' +
        volRegioes.map(r => `<option value="${r.id}">${PlantaoApi.escaparHtml(r.nome)} (${PlantaoApi.escaparHtml(r.bairroZona)})</option>`).join('');

    document.getElementById('vincIdoso').innerHTML = '<option value="">Selecione o idoso...</option>' +
        volIdosos.map(i => `<option value="${i.id}">${PlantaoApi.escaparHtml(i.nome)} — ${PlantaoApi.escaparHtml(i.nomeRegiao)}</option>`).join('');

    document.getElementById('vincVoluntario').innerHTML = '<option value="">Selecione o voluntario...</option>' +
        volLista.map(v => `<option value="${v.id}">${PlantaoApi.escaparHtml(v.nome)} (${PlantaoApi.escaparHtml(v.relacaoComunidade)})</option>`).join('');
}

function renderizarRedePorIdoso() {
    const tbody = document.getElementById('tabelaRedeIdosos');
    if (!volIdosos.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Nenhum idoso cadastrado.</td></tr>';
        return;
    }

    tbody.innerHTML = volIdosos.map(idoso => {
        const titular = volVinculos.find(v => v.idIdoso === idoso.id && v.papel === 'TITULAR');
        const reserva = volVinculos.find(v => v.idIdoso === idoso.id && v.papel === 'RESERVA');
        const completa = titular && reserva;

        return `
            <tr>
                <td>
                    <div class="fw-bold">${PlantaoApi.escaparHtml(idoso.nome)}</div>
                    <small class="text-muted">${PlantaoApi.escaparHtml(idoso.nomeRegiao)}</small>
                </td>
                <td>${renderCelulaVinculo(titular)}</td>
                <td>${renderCelulaVinculo(reserva)}</td>
                <td>
                    ${completa
                        ? '<span class="badge badge-ok"><i class="fas fa-check me-1"></i>Completa</span>'
                        : '<span class="badge badge-atencao"><i class="fas fa-exclamation-triangle me-1"></i>Incompleta</span>'}
                </td>
            </tr>
        `;
    }).join('');
}

function renderCelulaVinculo(vinc) {
    if (!vinc) {
        return '<span class="text-danger small fst-italic"><i class="fas fa-user-slash me-1"></i>Nao definido</span>';
    }
    return `
        <div class="d-flex justify-content-between align-items-center gap-2">
            <div>
                <div class="fw-semibold small">${PlantaoApi.escaparHtml(vinc.nomeVoluntario)}</div>
                <div class="text-muted" style="font-size:0.75rem;">${PlantaoApi.escaparHtml(vinc.relacaoVoluntario || '')} · ${PlantaoApi.escaparHtml(vinc.telefoneVoluntario || '')}</div>
            </div>
            <button type="button" class="btn btn-sm btn-link text-danger p-0" onclick="removerVinculo(${vinc.id})" title="Remover vinculo">
                <i class="fas fa-times-circle"></i>
            </button>
        </div>
    `;
}

function renderizarTabelaVoluntarios() {
    document.getElementById('totalVolLista').textContent = volLista.length;
    const tbody = document.getElementById('tabelaVoluntarios');
    if (!volLista.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Nenhum voluntario cadastrado.</td></tr>';
        return;
    }

    tbody.innerHTML = volLista.map(v => `
        <tr>
            <td>
                <div class="fw-bold">${PlantaoApi.escaparHtml(v.nome)}</div>
                <div class="small text-muted"><i class="fas fa-phone me-1"></i>${PlantaoApi.escaparHtml(v.telefone)}</div>
            </td>
            <td>
                <div class="small fw-semibold">${PlantaoApi.escaparHtml(v.relacaoComunidade)}</div>
                <div class="small text-muted">${PlantaoApi.escaparHtml(v.disponibilidade)}</div>
            </td>
            <td><span class="badge bg-light text-dark border">${PlantaoApi.escaparHtml(v.nomeRegiao)}</span></td>
            <td class="text-end text-nowrap">
                <button type="button" class="btn btn-sm btn-outline-primary me-1" onclick="editarVoluntario(${v.id})" title="Editar">
                    <i class="fas fa-pen"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="excluirVoluntario(${v.id}, '${PlantaoApi.escaparHtml(v.nome)}')" title="Excluir">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

async function salvarVinculo(e) {
    e.preventDefault();
    const idIdoso = document.getElementById('vincIdoso');
    const idVol = document.getElementById('vincVoluntario');
    const papel = document.getElementById('vincPapel');

    idIdoso.classList.toggle('is-invalid', !idIdoso.value);
    idVol.classList.toggle('is-invalid', !idVol.value);
    if (!idIdoso.value || !idVol.value) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', 'Selecione o idoso e o voluntario para criar o vinculo.', 'warning');
        return;
    }

    try {
        await PlantaoApi.post('/api/vinculos', {
            idIdoso: Number(idIdoso.value),
            idVoluntario: Number(idVol.value),
            papel: papel.value
        });
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', `Vinculo de ${papel.value} salvo no banco com sucesso!`, 'success');
        document.getElementById('formVinculo').reset();
        await carregarTudo();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', erro.message, 'danger');
    }
}

async function removerVinculo(id) {
    if (!window.confirm('Remover este vinculo de cuidado?')) return;
    try {
        await PlantaoApi.del(`/api/vinculos/${id}`);
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', 'Vinculo removido.', 'info');
        await carregarTudo();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', erro.message, 'danger');
    }
}

async function salvarVoluntario(e) {
    e.preventDefault();
    const nome = document.getElementById('volNome');
    const tel = document.getElementById('volTelefone');
    const regiao = document.getElementById('volRegiao');

    const okNome = nome.value.trim().length >= 3;
    const okTel = tel.value.replace(/\D/g, '').length >= 8;
    const okReg = regiao.value !== '';
    nome.classList.toggle('is-invalid', !okNome);
    tel.classList.toggle('is-invalid', !okTel);
    regiao.classList.toggle('is-invalid', !okReg);

    if (!okNome || !okTel || !okReg) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', 'Corrija os campos obrigatorios do voluntario.', 'warning');
        return;
    }

    const id = document.getElementById('volId').value;
    const payload = {
        nome: nome.value.trim(),
        telefone: tel.value.trim(),
        relacaoComunidade: document.getElementById('volRelacao').value,
        disponibilidade: document.getElementById('volDisponibilidade').value,
        idRegiao: Number(regiao.value),
        ativo: true
    };

    try {
        if (id) {
            await PlantaoApi.put(`/api/voluntarios/${id}`, payload);
            PlantaoApi.mostrarFeedback('feedbackVoluntarios', 'Voluntario atualizado com sucesso!', 'success');
        } else {
            await PlantaoApi.post('/api/voluntarios', payload);
            PlantaoApi.mostrarFeedback('feedbackVoluntarios', 'Voluntario cadastrado com sucesso!', 'success');
        }
        limparFormVol();
        await carregarTudo();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', erro.message, 'danger');
    }
}

function editarVoluntario(id) {
    const v = volLista.find(x => x.id === id);
    if (!v) return;
    document.getElementById('volId').value = v.id;
    document.getElementById('volNome').value = v.nome;
    document.getElementById('volTelefone').value = v.telefone;
    document.getElementById('volRelacao').value = v.relacaoComunidade;
    document.getElementById('volDisponibilidade').value = v.disponibilidade;
    document.getElementById('volRegiao').value = v.idRegiao;
    document.getElementById('formVolTitulo').innerHTML = '<i class="fas fa-pen text-warning me-2"></i>Editando Voluntario #' + v.id;
    document.getElementById('btnVolTexto').textContent = 'Atualizar Voluntario';
    document.getElementById('btnCancelarVol').classList.remove('d-none');
}

function limparFormVol() {
    const form = document.getElementById('formVoluntario');
    form.reset();
    document.getElementById('volId').value = '';
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    document.getElementById('formVolTitulo').innerHTML = '<i class="fas fa-user-plus text-success me-2"></i>Cadastrar Voluntario';
    document.getElementById('btnVolTexto').textContent = 'Salvar Voluntario';
    document.getElementById('btnCancelarVol').classList.add('d-none');
}

async function excluirVoluntario(id, nome) {
    if (!window.confirm(`Excluir o voluntario ${nome}?`)) return;
    try {
        await PlantaoApi.del(`/api/voluntarios/${id}`);
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', `Voluntario ${nome} excluido.`, 'info');
        await carregarTudo();
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackVoluntarios', erro.message, 'danger');
    }
}
