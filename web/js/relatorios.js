/**
 * Logica da tela de Relatorios SQL (relatorios.html).
 */
document.addEventListener('DOMContentLoaded', () => {
    carregarRelatorios();
});

async function carregarRelatorios() {
    try {
        const dados = await PlantaoApi.get('/api/relatorios');
        renderCobertura4h(dados.cobertura4h || []);
        renderRedeIncompleta(dados.redeIncompleta || []);
        renderTaxaNaoAtendeu(dados.taxaNaoAtendeu || []);
        renderAgregadoUbs(dados.agregadoRegioes || []);
    } catch (erro) {
        PlantaoApi.mostrarFeedback('feedbackRelatorios', 'Erro ao carregar relatorios SQL: ' + erro.message, 'danger');
    }
}

function renderCobertura4h(lista) {
    const tbody = document.getElementById('tbodyCobertura4h');
    if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Nenhum alerta registrado.</td></tr>';
        return;
    }
    tbody.innerHTML = lista.map(item => {
        const barColor = item.percentualAte4h >= 75 ? 'bg-success' : item.percentualAte4h >= 40 ? 'bg-warning' : 'bg-danger';
        return `
            <tr>
                <td class="fw-bold">${PlantaoApi.escaparHtml(item.titulo)}</td>
                <td>
                    <span class="badge ${item.tipo === 'CALOR' ? 'badge-calor' : 'badge-chuva'} me-1">${item.tipo}</span>
                    <span class="badge ${item.status === 'ATIVO' ? 'bg-danger' : 'bg-secondary'}">${item.status}</span>
                </td>
                <td class="text-center fw-semibold">${item.totalIdososRodada}</td>
                <td class="text-center fw-semibold text-success">${item.checadosAte4h}</td>
                <td class="text-center fw-bold ${item.criticos > 0 ? 'text-danger' : 'text-muted'}">${item.criticos}</td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <div class="progress flex-grow-1" style="height: 10px;">
                            <div class="progress-bar ${barColor}" style="width: ${item.percentualAte4h}%"></div>
                        </div>
                        <span class="fw-bold small">${item.percentualAte4h}%</span>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderRedeIncompleta(lista) {
    const tbody = document.getElementById('tbodyRedeIncompleta');
    if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-success fw-semibold"><i class="fas fa-check-circle me-1"></i>Todos os idosos possuem Titular e Reserva!</td></tr>';
        return;
    }
    tbody.innerHTML = lista.map(item => `
        <tr>
            <td>
                <div class="fw-bold">${PlantaoApi.escaparHtml(item.nomeIdoso)}</div>
                <small class="text-muted">Nasc. ${item.anoNascimento}</small>
            </td>
            <td>${PlantaoApi.escaparHtml(item.nomeRegiao)}</td>
            <td><span class="badge badge-atencao">${PlantaoApi.escaparHtml(item.diagnostico)}</span></td>
            <td class="text-end no-print">
                <a href="voluntarios.html" class="btn btn-sm btn-outline-primary">Vincular</a>
            </td>
        </tr>
    `).join('');
}

function renderTaxaNaoAtendeu(lista) {
    const tbody = document.getElementById('tbodyTaxaNaoAtendeu');
    if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Nenhum voluntario ativo.</td></tr>';
        return;
    }
    tbody.innerHTML = lista.map(item => `
        <tr>
            <td>
                <div class="fw-bold">${PlantaoApi.escaparHtml(item.nomeVoluntario)}</div>
                <small class="text-muted">${PlantaoApi.escaparHtml(item.relacaoComunidade)} · ${PlantaoApi.escaparHtml(item.nomeRegiao)}</small>
            </td>
            <td class="text-center">${item.totalLigacoes}</td>
            <td class="text-center fw-semibold ${item.totalNaoAtendeu > 0 ? 'text-danger' : 'text-muted'}">${item.totalNaoAtendeu}</td>
            <td>
                <span class="badge ${item.taxaNaoAtendeuPct >= 50 ? 'bg-danger' : item.taxaNaoAtendeuPct > 0 ? 'bg-warning text-dark' : 'bg-success'}">
                    ${item.taxaNaoAtendeuPct}%
                </span>
            </td>
        </tr>
    `).join('');
}

function renderAgregadoUbs(lista) {
    const tbody = document.getElementById('tbodyAgregadoUbs');
    if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">Nenhuma regiao ativa.</td></tr>';
        return;
    }
    tbody.innerHTML = lista.map(r => `
        <tr>
            <td>
                <div class="fw-bold">${PlantaoApi.escaparHtml(r.nomeRegiao)} <span class="text-muted fw-normal">(${PlantaoApi.escaparHtml(r.bairroZona)})</span></div>
                <small class="text-muted"><i class="fas fa-hospital me-1"></i>${PlantaoApi.escaparHtml(r.referenciaUbs)}</small>
            </td>
            <td class="text-center fw-bold">${r.totalIdosos}</td>
            <td class="text-center">${r.comDoencaCronica}</td>
            <td class="text-center">${r.semVentilacao}</td>
            <td class="text-center">${r.emAreaRisco}</td>
            <td class="text-center">${r.comMobilidadeReduzida}</td>
            <td class="text-center">
                <span class="badge ${(r.casosCriticosAtivos + r.casosAjudaAtivos) > 0 ? 'bg-danger' : 'bg-success'}">
                    ${r.casosCriticosAtivos} crit. / ${r.casosAjudaAtivos} ajuda
                </span>
            </td>
        </tr>
    `).join('');
}
