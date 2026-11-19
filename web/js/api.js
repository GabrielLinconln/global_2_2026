/**
 * Cliente de API do Plantao Clima.
 * Conecta ao backend Java + JDBC + H2 (http://localhost:8080/api).
 * Caso o index.html seja aberto diretamente via file:// sem o servidor Java ativo,
 * ativa automaticamente o modo demonstracao local (localStorage) com os mesmos dados iniciais.
 */
const PlantaoApi = (() => {
    const STORAGE_KEY = 'plantao_clima_offline_db_v1';
    let apiBaseUrl = '';
    let modoOnline = false;
    let inicializado = false;

    function getBaseCandidates() {
        if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
            return [window.location.origin];
        }
        return ['http://localhost:8080'];
    }

    async function detectarBackend() {
        if (inicializado) {
            return modoOnline;
        }
        for (const base of getBaseCandidates()) {
            try {
                const controller = new AbortController();
                const timer = setTimeout(() => controller.abort(), 1400);
                const resp = await fetch(`${base}/api/health`, { signal: controller.signal });
                clearTimeout(timer);
                const ct = resp.headers.get('content-type') || '';
                if (resp.ok && ct.includes('application/json')) {
                    apiBaseUrl = base;
                    modoOnline = true;
                    inicializado = true;
                    atualizarIndicadorConexao();
                    return true;
                }
            } catch (_) {
                // tenta proximo candidato ou cai no modo demonstracao offline
            }
        }
        modoOnline = false;
        inicializado = true;
        garantirDadosOffline();
        atualizarIndicadorConexao();
        return false;
    }

    function atualizarIndicadorConexao() {
        const pill = document.getElementById('connectionStatus');
        if (!pill) return;
        if (modoOnline) {
            pill.className = 'connection-pill online';
            pill.textContent = 'H2 Online';
        } else {
            pill.className = 'connection-pill offline';
            pill.textContent = 'Demo Web (Local)';
        }
    }

    async function request(method, path, body = null) {
        await detectarBackend();
        if (modoOnline) {
            const options = {
                method,
                headers: { 'Content-Type': 'application/json' }
            };
            if (body !== null) {
                options.body = JSON.stringify(body);
            }
            const resp = await fetch(`${apiBaseUrl}${path}`, options);
            const data = await resp.json().catch(() => ({}));
            if (!resp.ok) {
                throw new Error(data.mensagem || `Erro HTTP ${resp.status}`);
            }
            return data;
        }
        return executarOffline(method, path, body);
    }

    // =========================================================================
    // MODO FALLBACK LOCAL (usado apenas se o servidor Java nao estiver rodando)
    // =========================================================================
    function calcularRisco(idoso, tipoAlerta) {
        const idade = Math.max(0, new Date().getFullYear() - Number(idoso.anoNascimento || 1950));
        let pontos = idade >= 80 ? 25 : idade >= 70 ? 15 : 10;
        if (tipoAlerta === 'CALOR') {
            if (idoso.doencaCronica) pontos += 35;
            if (idoso.casaSemVentilacao) pontos += 30;
            if (idoso.mobilidadeReduzida) pontos += 10;
            if (idoso.areaRisco) pontos += 5;
        } else {
            if (idoso.areaRisco) pontos += 35;
            if (idoso.mobilidadeReduzida) pontos += 30;
            if (idoso.doencaCronica) pontos += 10;
            if (idoso.casaSemVentilacao) pontos += 5;
        }
        return Math.min(100, pontos);
    }

    function criarSeedOffline() {
        const agora = Date.now();
        const iso = (msOffset) => new Date(agora + msOffset).toISOString().slice(0, 19);
        return {
            seq: { regiao: 6, idoso: 9, voluntario: 7, vinculo: 14, alerta: 3, checagem: 14 },
            regioes: [
                { id: 1, nome: 'Vila Esperanca', bairroZona: 'Zona Leste', riscoCalor: 'ALTO', riscoChuva: 'MODERADO', referenciaUbs: 'UBS Vila Esperanca - Rua das Acacias, 120', ativo: true },
                { id: 2, nome: 'Jardim Ribeirinho', bairroZona: 'Zona Sul', riscoCalor: 'MODERADO', riscoChuva: 'ALTO', referenciaUbs: 'UBS Ribeirinho - Av. Beira Rio, 45', ativo: true },
                { id: 3, nome: 'Centro Historico', bairroZona: 'Regiao Central', riscoCalor: 'ALTO', riscoChuva: 'BAIXO', referenciaUbs: 'UBS Se / Centro - Praca da Republica, 88', ativo: true },
                { id: 4, nome: 'Parque da Serra', bairroZona: 'Zona Norte', riscoCalor: 'MODERADO', riscoChuva: 'ALTO', referenciaUbs: 'UBS Serra Verde - Estrada Velha, 310', ativo: true },
                { id: 5, nome: 'Bairro das Flores', bairroZona: 'Zona Oeste', riscoCalor: 'BAIXO', riscoChuva: 'MODERADO', referenciaUbs: 'UBS Jardim Flores - Rua Magnolia, 210', ativo: true }
            ],
            idosos: [
                { id: 1, nome: 'Dona Maria Aparecida S.', telefone: '(11) 98123-4401', anoNascimento: 1941, enderecoResumo: 'Rua das Acacias, 84 - Casa fundos', doencaCronica: true, mobilidadeReduzida: true, casaSemVentilacao: true, areaRisco: false, consentimentoLgpd: true, observacoes: 'Hipertensa, usa bengala, telhado de zinco sem ventilador.', idRegiao: 1, ativo: true },
                { id: 2, nome: 'Seu Jose Carlos M.', telefone: '(11) 98123-4402', anoNascimento: 1945, enderecoResumo: 'Av. Beira Rio, 112 - Terreo', doencaCronica: true, mobilidadeReduzida: true, casaSemVentilacao: false, areaRisco: true, consentimentoLgpd: true, observacoes: 'Diabetico, rua alaga com chuvas fortes.', idRegiao: 2, ativo: true },
                { id: 3, nome: 'Dona Helena Ferreira', telefone: '(11) 98123-4403', anoNascimento: 1948, enderecoResumo: 'Rua Conselheiro Crispiniano, 55 - Apto 42', doencaCronica: true, mobilidadeReduzida: false, casaSemVentilacao: true, areaRisco: false, consentimentoLgpd: true, observacoes: 'Cardiopata, apartamento face oeste muito quente.', idRegiao: 3, ativo: true },
                { id: 4, nome: 'Seu Antonio Batista', telefone: '(11) 98123-4404', anoNascimento: 1939, enderecoResumo: 'Estrada Velha, 290 - Encosta', doencaCronica: false, mobilidadeReduzida: true, casaSemVentilacao: false, areaRisco: true, consentimentoLgpd: true, observacoes: 'Dificuldade para subir escadas, casa proxima a talude.', idRegiao: 4, ativo: true },
                { id: 5, nome: 'Dona Nair Oliveira', telefone: '(11) 98123-4405', anoNascimento: 1953, enderecoResumo: 'Rua das Acacias, 205 - Sobrado', doencaCronica: true, mobilidadeReduzida: false, casaSemVentilacao: true, areaRisco: false, consentimentoLgpd: true, observacoes: 'Asma leve.', idRegiao: 1, ativo: true },
                { id: 6, nome: 'Seu Pedro Henrique R.', telefone: '(11) 98123-4406', anoNascimento: 1956, enderecoResumo: 'Rua Magnolia, 78 - Casa 2', doencaCronica: false, mobilidadeReduzida: false, casaSemVentilacao: false, areaRisco: false, consentimentoLgpd: true, observacoes: 'Independente.', idRegiao: 5, ativo: true },
                { id: 7, nome: 'Dona Tereza Lima', telefone: '(11) 98123-4407', anoNascimento: 1943, enderecoResumo: 'Viela do Corrego, 19 - Casa 1', doencaCronica: true, mobilidadeReduzida: true, casaSemVentilacao: true, areaRisco: true, consentimentoLgpd: true, observacoes: 'Sem voluntario reserva.', idRegiao: 2, ativo: true },
                { id: 8, nome: 'Seu Oswaldo Mendes', telefone: '(11) 98123-4408', anoNascimento: 1950, enderecoResumo: 'Rua Nova, 314 - Apto 11', doencaCronica: true, mobilidadeReduzida: false, casaSemVentilacao: false, areaRisco: false, consentimentoLgpd: true, observacoes: 'Aguardando vinculacao.', idRegiao: 3, ativo: true }
            ],
            voluntarios: [
                { id: 1, nome: 'Carla Souza', telefone: '(11) 97200-1001', relacaoComunidade: 'Vizinha', disponibilidade: 'Integral', idRegiao: 1, ativo: true },
                { id: 2, nome: 'Marcos Vinicius', telefone: '(11) 97200-1002', relacaoComunidade: 'Igreja Comunitaria', disponibilidade: 'Manha e Tarde', idRegiao: 1, ativo: true },
                { id: 3, nome: 'Fernanda Lima', telefone: '(11) 97200-1003', relacaoComunidade: 'Agente de Saude', disponibilidade: 'Integral', idRegiao: 2, ativo: true },
                { id: 4, nome: 'Roberto Almeida', telefone: '(11) 97200-1004', relacaoComunidade: 'Sindico', disponibilidade: 'Noite e Fins de Semana', idRegiao: 3, ativo: true },
                { id: 5, nome: 'Juliana Rocha', telefone: '(11) 97200-1005', relacaoComunidade: 'Familiar / Sobrinha', disponibilidade: 'Integral', idRegiao: 4, ativo: true },
                { id: 6, nome: 'Paulo Cesar', telefone: '(11) 97200-1006', relacaoComunidade: 'Associacao de Bairro', disponibilidade: 'Tarde e Noite', idRegiao: 5, ativo: true }
            ],
            vinculos: [
                { id: 1, idIdoso: 1, idVoluntario: 1, papel: 'TITULAR', prioridade: 1 },
                { id: 2, idIdoso: 1, idVoluntario: 2, papel: 'RESERVA', prioridade: 2 },
                { id: 3, idIdoso: 2, idVoluntario: 3, papel: 'TITULAR', prioridade: 1 },
                { id: 4, idIdoso: 2, idVoluntario: 6, papel: 'RESERVA', prioridade: 2 },
                { id: 5, idIdoso: 3, idVoluntario: 4, papel: 'TITULAR', prioridade: 1 },
                { id: 6, idIdoso: 3, idVoluntario: 1, papel: 'RESERVA', prioridade: 2 },
                { id: 7, idIdoso: 4, idVoluntario: 5, papel: 'TITULAR', prioridade: 1 },
                { id: 8, idIdoso: 4, idVoluntario: 2, papel: 'RESERVA', prioridade: 2 },
                { id: 9, idIdoso: 5, idVoluntario: 2, papel: 'TITULAR', prioridade: 1 },
                { id: 10, idIdoso: 5, idVoluntario: 1, papel: 'RESERVA', prioridade: 2 },
                { id: 11, idIdoso: 6, idVoluntario: 6, papel: 'TITULAR', prioridade: 1 },
                { id: 12, idIdoso: 6, idVoluntario: 5, papel: 'RESERVA', prioridade: 2 },
                { id: 13, idIdoso: 7, idVoluntario: 3, papel: 'TITULAR', prioridade: 1 }
            ],
            alertas: [
                { id: 1, titulo: 'Onda de Calor Extremo - El Nino (39C)', tipo: 'CALOR', severidade: 'VERMELHO', fonte: 'INMET', descricao: 'Temperatura 5C acima da media por 3 dias consecutivos com baixa umidade.', status: 'ATIVO', idRegiao: null, emitidoEm: iso(-7200000), encerradoEm: null },
                { id: 2, titulo: 'Chuva Intensa e Risco de Alagamento - Beira Rio', tipo: 'CHUVA', severidade: 'LARANJA', fonte: 'Defesa Civil / FloodGuard', descricao: 'Acumulado previsto de 70mm/h na Zona Sul.', status: 'ENCERRADO', idRegiao: 2, emitidoEm: iso(-93600000), encerradoEm: iso(-72000000) }
            ],
            checagens: [
                { id: 1, idAlerta: 1, idIdoso: 1, idVoluntario: 1, papelAtual: 'TITULAR', pontuacaoRisco: 100, status: 'NAO_ATENDEU', tentativas: 2, observacao: 'Telefone chamou ate caixa postal.', criadoEm: iso(-7200000), atualizadoEm: iso(-5700000) },
                { id: 2, idAlerta: 1, idIdoso: 1, idVoluntario: 2, papelAtual: 'RESERVA', pontuacaoRisco: 100, status: 'NAO_ATENDEU', tentativas: 2, observacao: 'Tambem sem resposta.', criadoEm: iso(-5700000), atualizadoEm: iso(-3000000) },
                { id: 3, idAlerta: 1, idIdoso: 1, idVoluntario: null, papelAtual: 'COORDENACAO', pontuacaoRisco: 100, status: 'CRITICO', tentativas: 0, observacao: 'Titular e reserva nao conseguiram contato. Enviar alguem a casa e ligar 192/193/199.', criadoEm: iso(-3000000), atualizadoEm: iso(-3000000) },
                { id: 4, idAlerta: 1, idIdoso: 7, idVoluntario: 3, papelAtual: 'TITULAR', pontuacaoRisco: 100, status: 'PRECISA_ATENCAO', tentativas: 1, checklistResumo: 'Casa muito abafada; Ventilador desligado', observacao: 'Relatou leve tontura pelo calor.', criadoEm: iso(-7200000), atualizadoEm: iso(-4800000) },
                { id: 5, idAlerta: 1, idIdoso: 3, idVoluntario: 4, papelAtual: 'TITULAR', pontuacaoRisco: 80, status: 'NAO_ATENDEU', tentativas: 1, observacao: 'Passado para reserva.', criadoEm: iso(-7200000), atualizadoEm: iso(-4200000) },
                { id: 6, idAlerta: 1, idIdoso: 3, idVoluntario: 1, papelAtual: 'RESERVA', pontuacaoRisco: 80, status: 'PENDENTE', tentativas: 0, observacao: 'Aguardando ligacao do reserva.', criadoEm: iso(-4200000), atualizadoEm: null },
                { id: 7, idAlerta: 1, idIdoso: 5, idVoluntario: 2, papelAtual: 'TITULAR', pontuacaoRisco: 80, status: 'ESTA_BEM', tentativas: 1, observacao: 'Hidratada e bem.', criadoEm: iso(-7200000), atualizadoEm: iso(-6000000) },
                { id: 8, idAlerta: 1, idIdoso: 2, idVoluntario: 3, papelAtual: 'TITULAR', pontuacaoRisco: 75, status: 'ESTA_BEM', tentativas: 1, observacao: 'Acompanhado por vizinha.', criadoEm: iso(-7200000), atualizadoEm: iso(-5400000) },
                { id: 9, idAlerta: 1, idIdoso: 8, idVoluntario: null, papelAtual: 'COORDENACAO', pontuacaoRisco: 50, status: 'PENDENTE', tentativas: 0, observacao: 'Sem voluntario vinculado.', criadoEm: iso(-7200000), atualizadoEm: null },
                { id: 10, idAlerta: 1, idIdoso: 4, idVoluntario: 5, papelAtual: 'TITULAR', pontuacaoRisco: 40, status: 'PENDENTE', tentativas: 0, observacao: 'Aguardando titular.', criadoEm: iso(-7200000), atualizadoEm: null },
                { id: 11, idAlerta: 1, idIdoso: 6, idVoluntario: 6, papelAtual: 'TITULAR', pontuacaoRisco: 15, status: 'ESTA_BEM', tentativas: 1, observacao: 'Tudo tranquilo.', criadoEm: iso(-7200000), atualizadoEm: iso(-3900000) },
                { id: 12, idAlerta: 2, idIdoso: 2, idVoluntario: 3, papelAtual: 'TITULAR', pontuacaoRisco: 95, status: 'ESTA_BEM', tentativas: 1, observacao: 'Sem alagamento na casa.', criadoEm: iso(-93600000), atualizadoEm: iso(-86400000) },
                { id: 13, idAlerta: 2, idIdoso: 7, idVoluntario: 3, papelAtual: 'TITULAR', pontuacaoRisco: 100, status: 'PRECISA_ATENCAO', tentativas: 1, checklistResumo: 'Agua na rua; Documentos separados', observacao: 'Acompanhada por vizinho.', criadoEm: iso(-93600000), atualizadoEm: iso(-90000000) }
            ]
        };
    }

    function garantirDadosOffline() {
        if (!localStorage.getItem(STORAGE_KEY)) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(criarSeedOffline()));
        }
        return JSON.parse(localStorage.getItem(STORAGE_KEY));
    }

    function salvarOffline(db) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
    }

    function enriquecerIdoso(db, i) {
        const reg = db.regioes.find(r => r.id === Number(i.idRegiao));
        const pCalor = calcularRisco(i, 'CALOR');
        const pChuva = calcularRisco(i, 'CHUVA');
        return {
            ...i,
            idadeEstimada: Math.max(0, new Date().getFullYear() - Number(i.anoNascimento)),
            nomeRegiao: reg ? reg.nome : '-',
            pontuacaoCalor: pCalor,
            pontuacaoChuva: pChuva,
            nivelCalor: pCalor >= 75 ? 'MUITO_ALTO' : pCalor >= 45 ? 'ALTO' : 'MODERADO',
            nivelChuva: pChuva >= 75 ? 'MUITO_ALTO' : pChuva >= 45 ? 'ALTO' : 'MODERADO'
        };
    }

    function enriquecerChecagem(db, c) {
        const a = db.alertas.find(x => x.id === c.idAlerta) || {};
        const i = db.idosos.find(x => x.id === c.idIdoso) || {};
        const r = db.regioes.find(x => x.id === i.idRegiao) || {};
        const v = db.voluntarios.find(x => x.id === c.idVoluntario) || {};
        return {
            ...c,
            tituloAlerta: a.titulo || '',
            tipoAlerta: a.tipo || 'CALOR',
            nomeIdoso: i.nome || '',
            telefoneIdoso: i.telefone || '',
            anoNascimentoIdoso: i.anoNascimento || 1950,
            enderecoIdoso: i.enderecoResumo || '',
            nomeRegiao: r.nome || '',
            doencaCronica: !!i.doencaCronica,
            mobilidadeReduzida: !!i.mobilidadeReduzida,
            casaSemVentilacao: !!i.casaSemVentilacao,
            areaRisco: !!i.areaRisco,
            nomeVoluntario: v.nome || null,
            telefoneVoluntario: v.telefone || null
        };
    }

    function obterRodadaAtualOffline(db, idAlerta) {
        const doAlerta = db.checagens.filter(c => c.idAlerta === idAlerta);
        const latestByIdoso = new Map();
        for (const c of doAlerta) {
            const prev = latestByIdoso.get(c.idIdoso);
            if (!prev || c.id > prev.id) latestByIdoso.set(c.idIdoso, c);
        }
        const ordem = { CRITICO: 1, PRECISA_AJUDA: 2, PENDENTE: 3, PRECISA_ATENCAO: 4, NAO_ATENDEU: 5, ESTA_BEM: 6 };
        return [...latestByIdoso.values()]
            .map(c => enriquecerChecagem(db, c))
            .sort((a, b) => (ordem[a.status] - ordem[b.status]) || (b.pontuacaoRisco - a.pontuacaoRisco));
    }

    function montarRelatoriosOffline(db) {
        const cobertura4h = db.alertas.map(a => {
            const lista = db.checagens.filter(c => c.idAlerta === a.id);
            const idososSet = new Set(lista.map(c => c.idIdoso));
            const emitMs = new Date(a.emitidoEm).getTime();
            const checados4hSet = new Set();
            const totalChecadosSet = new Set();
            for (const c of lista) {
                if (['ESTA_BEM', 'PRECISA_ATENCAO', 'PRECISA_AJUDA'].includes(c.status)) {
                    totalChecadosSet.add(c.idIdoso);
                    if (c.atualizadoEm && (new Date(c.atualizadoEm).getTime() - emitMs) <= 4 * 3600000) {
                        checados4hSet.add(c.idIdoso);
                    }
                }
            }
            const rodada = obterRodadaAtualOffline(db, a.id);
            const criticos = rodada.filter(c => c.status === 'CRITICO').length;
            const total = idososSet.size;
            return {
                idAlerta: a.id, titulo: a.titulo, tipo: a.tipo, status: a.status,
                totalIdososRodada: total, checadosAte4h: checados4hSet.size,
                totalChecados: totalChecadosSet.size, criticos,
                percentualAte4h: total ? Math.round((checados4hSet.size * 1000) / total) / 10 : 0
            };
        });

        const redeIncompleta = db.idosos.filter(i => i.ativo).map(i => {
            const v = db.vinculos.filter(x => x.idIdoso === i.id);
            const qtdTitular = v.filter(x => x.papel === 'TITULAR').length;
            const qtdReserva = v.filter(x => x.papel === 'RESERVA').length;
            const reg = db.regioes.find(r => r.id === i.idRegiao);
            let diagnostico = '';
            if (!qtdTitular && !qtdReserva) diagnostico = 'Sem titular e sem reserva';
            else if (!qtdTitular) diagnostico = 'Sem voluntario titular';
            else if (!qtdReserva) diagnostico = 'Sem voluntario reserva';
            return { idIdoso: i.id, nomeIdoso: i.nome, nomeRegiao: reg ? reg.nome : '-', anoNascimento: i.anoNascimento, qtdTitular, qtdReserva, diagnostico };
        }).filter(x => x.diagnostico);

        const taxaNaoAtendeu = db.voluntarios.filter(v => v.ativo).map(v => {
            const reg = db.regioes.find(r => r.id === v.idRegiao);
            const calls = db.checagens.filter(c => c.idVoluntario === v.id && c.status !== 'PENDENTE');
            const na = calls.filter(c => c.status === 'NAO_ATENDEU').length;
            return {
                idVoluntario: v.id, nomeVoluntario: v.nome, relacaoComunidade: v.relacaoComunidade,
                nomeRegiao: reg ? reg.nome : '-', totalLigacoes: calls.length, totalNaoAtendeu: na,
                taxaNaoAtendeuPct: calls.length ? Math.round((na * 1000) / calls.length) / 10 : 0
            };
        });

        const alertasAtivos = db.alertas.filter(a => a.status === 'ATIVO');
        const rodadasAtivas = alertasAtivos.flatMap(a => obterRodadaAtualOffline(db, a.id));

        const agregadoRegioes = db.regioes.filter(r => r.ativo).map(r => {
            const ids = db.idosos.filter(i => i.ativo && i.idRegiao === r.id);
            const idSet = new Set(ids.map(i => i.id));
            const rCalls = rodadasAtivas.filter(c => idSet.has(c.idIdoso));
            return {
                idRegiao: r.id, nomeRegiao: r.nome, bairroZona: r.bairroZona,
                riscoCalor: r.riscoCalor, riscoChuva: r.riscoChuva, referenciaUbs: r.referenciaUbs,
                totalIdosos: ids.length,
                comDoencaCronica: ids.filter(i => i.doencaCronica).length,
                comMobilidadeReduzida: ids.filter(i => i.mobilidadeReduzida).length,
                semVentilacao: ids.filter(i => i.casaSemVentilacao).length,
                emAreaRisco: ids.filter(i => i.areaRisco).length,
                alertasAtivos: alertasAtivos.filter(a => a.idRegiao === null || a.idRegiao === r.id).length,
                pendentesAtivos: rCalls.filter(c => c.status === 'PENDENTE').length,
                casosCriticosAtivos: rCalls.filter(c => c.status === 'CRITICO').length,
                casosAjudaAtivos: rCalls.filter(c => c.status === 'PRECISA_AJUDA').length
            };
        });

        return { cobertura4h, redeIncompleta, taxaNaoAtendeu, agregadoRegioes, rodadasAtivas };
    }

    function executarOffline(method, path, body) {
        const db = garantirDadosOffline();
        const parts = path.split('/').filter(Boolean); // ['api', 'idosos', '1']
        const recurso = parts[1];
        const id = parts[2] ? Number(parts[2]) : null;
        const acao = parts[3] || null;

        if (recurso === 'painel' && method === 'GET') {
            const rel = montarRelatoriosOffline(db);
            const alertasAtivos = db.alertas.filter(a => a.status === 'ATIVO');
            const criticos = rel.rodadasAtivas.filter(c => ['CRITICO', 'PRECISA_AJUDA'].includes(c.status));
            return {
                resumo: {
                    totalIdosos: db.idosos.filter(i => i.ativo).length,
                    totalVoluntarios: db.voluntarios.filter(v => v.ativo).length,
                    alertasAtivos: alertasAtivos.length,
                    casosCriticos: rel.rodadasAtivas.filter(c => c.status === 'CRITICO').length,
                    precisamAjuda: rel.rodadasAtivas.filter(c => c.status === 'PRECISA_AJUDA').length,
                    precisamAtencao: rel.rodadasAtivas.filter(c => c.status === 'PRECISA_ATENCAO').length,
                    pendentes: rel.rodadasAtivas.filter(c => c.status === 'PENDENTE').length,
                    checadosOk: rel.rodadasAtivas.filter(c => c.status === 'ESTA_BEM').length,
                    idososRedeIncompleta: rel.redeIncompleta.length,
                    percentualChecados4h: rel.cobertura4h[0]?.percentualAte4h || 0
                },
                casosCriticos: criticos,
                alertas: db.alertas.map(a => ({ ...a, nomeRegiao: db.regioes.find(r => r.id === a.idRegiao)?.nome || 'Todas as Regioes' })),
                mapaRegioes: rel.agregadoRegioes
            };
        }

        if (recurso === 'relatorios' && method === 'GET') {
            const rel = montarRelatoriosOffline(db);
            return {
                cobertura4h: rel.cobertura4h,
                redeIncompleta: rel.redeIncompleta,
                taxaNaoAtendeu: rel.taxaNaoAtendeu,
                agregadoRegioes: rel.agregadoRegioes
            };
        }

        if (recurso === 'regioes' && method === 'GET') {
            return db.regioes;
        }

        if (recurso === 'idosos') {
            if (method === 'GET') return db.idosos.map(i => enriquecerIdoso(db, i));
            if (method === 'POST') {
                if (!body.consentimentoLgpd) throw new Error('O consentimento registrado do idoso (LGPD) e obrigatorio.');
                const novo = { ...body, id: db.seq.idoso++, idRegiao: Number(body.idRegiao), anoNascimento: Number(body.anoNascimento), ativo: true };
                db.idosos.push(novo);
                salvarOffline(db);
                return enriquecerIdoso(db, novo);
            }
            if (method === 'PUT' && id) {
                const idx = db.idosos.findIndex(x => x.id === id);
                if (idx < 0) throw new Error('Idoso nao encontrado.');
                db.idosos[idx] = { ...db.idosos[idx], ...body, id, idRegiao: Number(body.idRegiao), anoNascimento: Number(body.anoNascimento) };
                salvarOffline(db);
                return enriquecerIdoso(db, db.idosos[idx]);
            }
            if (method === 'DELETE' && id) {
                db.idosos = db.idosos.filter(x => x.id !== id);
                db.vinculos = db.vinculos.filter(x => x.idIdoso !== id);
                db.checagens = db.checagens.filter(x => x.idIdoso !== id);
                salvarOffline(db);
                return { mensagem: 'Idoso excluido com sucesso.' };
            }
        }

        if (recurso === 'voluntarios') {
            if (method === 'GET') {
                return db.voluntarios.map(v => ({ ...v, nomeRegiao: db.regioes.find(r => r.id === v.idRegiao)?.nome || '-' }));
            }
            if (method === 'POST') {
                if (db.voluntarios.some(v => v.telefone === body.telefone)) throw new Error('Ja existe um voluntario cadastrado com este telefone.');
                const novo = { ...body, id: db.seq.voluntario++, idRegiao: Number(body.idRegiao), ativo: true };
                db.voluntarios.push(novo);
                salvarOffline(db);
                return novo;
            }
            if (method === 'PUT' && id) {
                const idx = db.voluntarios.findIndex(x => x.id === id);
                if (idx < 0) throw new Error('Voluntario nao encontrado.');
                db.voluntarios[idx] = { ...db.voluntarios[idx], ...body, id, idRegiao: Number(body.idRegiao) };
                salvarOffline(db);
                return db.voluntarios[idx];
            }
            if (method === 'DELETE' && id) {
                db.voluntarios = db.voluntarios.filter(x => x.id !== id);
                db.vinculos = db.vinculos.filter(x => x.idVoluntario !== id);
                salvarOffline(db);
                return { mensagem: 'Voluntario excluido com sucesso.' };
            }
        }

        if (recurso === 'vinculos') {
            if (method === 'GET') {
                return db.vinculos.map(vc => {
                    const i = db.idosos.find(x => x.id === vc.idIdoso) || {};
                    const v = db.voluntarios.find(x => x.id === vc.idVoluntario) || {};
                    return { ...vc, nomeIdoso: i.nome, telefoneIdoso: i.telefone, nomeVoluntario: v.nome, telefoneVoluntario: v.telefone, relacaoVoluntario: v.relacaoComunidade };
                });
            }
            if (method === 'POST') {
                const idIdoso = Number(body.idIdoso);
                const idVoluntario = Number(body.idVoluntario);
                const papel = body.papel;
                if (db.vinculos.some(x => x.idIdoso === idIdoso && x.papel === papel)) {
                    throw new Error(`Este idoso ja possui um voluntario ${papel}.`);
                }
                if (db.vinculos.some(x => x.idIdoso === idIdoso && x.idVoluntario === idVoluntario)) {
                    throw new Error('Este voluntario ja esta vinculado a este idoso.');
                }
                const novo = { id: db.seq.vinculo++, idIdoso, idVoluntario, papel, prioridade: papel === 'TITULAR' ? 1 : 2 };
                db.vinculos.push(novo);
                salvarOffline(db);
                return novo;
            }
            if (method === 'DELETE' && id) {
                db.vinculos = db.vinculos.filter(x => x.id !== id);
                salvarOffline(db);
                return { mensagem: 'Vinculo removido.' };
            }
        }

        if (recurso === 'alertas') {
            if (method === 'GET' && !id) {
                return db.alertas.map(a => ({ ...a, nomeRegiao: db.regioes.find(r => r.id === a.idRegiao)?.nome || 'Todas as Regioes' }));
            }
            if (method === 'GET' && id && acao === 'rodada') {
                return obterRodadaAtualOffline(db, id);
            }
            if (method === 'POST' && !id) {
                const novo = {
                    id: db.seq.alerta++, titulo: body.titulo, tipo: body.tipo, severidade: body.severidade,
                    fonte: body.fonte, descricao: body.descricao, status: 'ATIVO',
                    idRegiao: body.idRegiao ? Number(body.idRegiao) : null,
                    emitidoEm: new Date().toISOString().slice(0, 19), encerradoEm: null
                };
                db.alertas.unshift(novo);
                const alvos = db.idosos.filter(i => i.ativo && (novo.idRegiao === null || i.idRegiao === novo.idRegiao));
                for (const idoso of alvos) {
                    const tit = db.vinculos.find(v => v.idIdoso === idoso.id && v.papel === 'TITULAR');
                    const res = db.vinculos.find(v => v.idIdoso === idoso.id && v.papel === 'RESERVA');
                    db.checagens.push({
                        id: db.seq.checagem++, idAlerta: novo.id, idIdoso: idoso.id,
                        idVoluntario: tit ? tit.idVoluntario : (res ? res.idVoluntario : null),
                        papelAtual: tit ? 'TITULAR' : (res ? 'RESERVA' : 'COORDENACAO'),
                        pontuacaoRisco: calcularRisco(idoso, novo.tipo),
                        status: 'PENDENTE', tentativas: 0, checklistResumo: null,
                        observacao: tit ? 'Aguardando titular.' : 'Sem titular cadastrado.',
                        criadoEm: new Date().toISOString().slice(0, 19), atualizadoEm: null
                    });
                }
                salvarOffline(db);
                return { alerta: novo, rodada: obterRodadaAtualOffline(db, novo.id) };
            }
            if (method === 'POST' && id && acao === 'encerrar') {
                const a = db.alertas.find(x => x.id === id);
                if (a) { a.status = 'ENCERRADO'; a.encerradoEm = new Date().toISOString().slice(0, 19); }
                salvarOffline(db);
                return a;
            }
            if (method === 'DELETE' && id) {
                db.alertas = db.alertas.filter(x => x.id !== id);
                db.checagens = db.checagens.filter(x => x.idAlerta !== id);
                salvarOffline(db);
                return { mensagem: 'Alerta excluido.' };
            }
        }

        if (recurso === 'checagens' && id && acao === 'registrar') {
            const c = db.checagens.find(x => x.id === id);
            if (!c) throw new Error('Checagem nao encontrada.');
            c.status = body.status;
            c.tentativas = (c.tentativas || 0) + 1;
            c.checklistResumo = body.checklistResumo || null;
            c.observacao = body.observacao || c.observacao;
            c.atualizadoEm = new Date().toISOString().slice(0, 19);

            let escalonado = false;
            let mensagem = 'Resultado registrado com sucesso.';
            if (body.status === 'NAO_ATENDEU') {
                if (c.papelAtual === 'TITULAR') {
                    const res = db.vinculos.find(v => v.idIdoso === c.idIdoso && v.papel === 'RESERVA');
                    if (res) {
                        db.checagens.push({
                            id: db.seq.checagem++, idAlerta: c.idAlerta, idIdoso: c.idIdoso,
                            idVoluntario: res.idVoluntario, papelAtual: 'RESERVA',
                            pontuacaoRisco: c.pontuacaoRisco, status: 'PENDENTE', tentativas: 0,
                            observacao: 'Escalonado automaticamente apos titular nao obter resposta.',
                            criadoEm: new Date().toISOString().slice(0, 19), atualizadoEm: null
                        });
                        escalonado = true;
                        mensagem = 'Titular nao obteve resposta. Passado automaticamente para o voluntario RESERVA.';
                    } else {
                        db.checagens.push({
                            id: db.seq.checagem++, idAlerta: c.idAlerta, idIdoso: c.idIdoso,
                            idVoluntario: null, papelAtual: 'COORDENACAO',
                            pontuacaoRisco: c.pontuacaoRisco, status: 'CRITICO', tentativas: 0,
                            observacao: 'Titular nao atendeu e nao ha reserva. Enviar alguem a casa e ligar 193, 192 ou 199.',
                            criadoEm: new Date().toISOString().slice(0, 19), atualizadoEm: new Date().toISOString().slice(0, 19)
                        });
                        escalonado = true;
                        mensagem = 'CASO CRITICO: Sem reserva cadastrado. Envie alguem ate a casa e acione 193/192/199.';
                    }
                } else {
                    db.checagens.push({
                        id: db.seq.checagem++, idAlerta: c.idAlerta, idIdoso: c.idIdoso,
                        idVoluntario: null, papelAtual: 'COORDENACAO',
                        pontuacaoRisco: c.pontuacaoRisco, status: 'CRITICO', tentativas: 0,
                        observacao: 'Titular e Reserva nao obtiveram resposta. Enviar alguem a casa e acionar 193, 192 ou 199.',
                        criadoEm: new Date().toISOString().slice(0, 19), atualizadoEm: new Date().toISOString().slice(0, 19)
                    });
                    escalonado = true;
                    mensagem = 'CASO CRITICO NO PAINEL: Titular e Reserva nao atenderam. Enviar alguem a casa e ligar 193, 192 ou 199.';
                }
            } else if (body.status === 'PRECISA_AJUDA') {
                mensagem = 'EMERGENCIA REGISTRADA: Acione imediatamente o SAMU 192, Bombeiros 193 ou Defesa Civil 199.';
            }
            salvarOffline(db);
            return { checagem: enriquecerChecagem(db, c), escalonado, mensagem };
        }

        throw new Error('Operacao nao suportada no modo offline.');
    }

    function mostrarFeedback(containerId, mensagem, tipo = 'success') {
        const el = document.getElementById(containerId);
        if (!el) return;
        const icone = tipo === 'danger' ? 'fa-exclamation-circle'
            : tipo === 'warning' ? 'fa-exclamation-triangle'
            : tipo === 'info' ? 'fa-info-circle' : 'fa-check-circle';
        el.innerHTML = `
            <div class="alert alert-${tipo} alert-dismissible fade show shadow-sm mb-3" role="alert">
                <i class="fas ${icone} me-2"></i>${escaparHtml(mensagem)}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
            </div>`;
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function escaparHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function limparTelefoneHref(tel) {
        return 'tel:' + String(tel || '').replace(/[^\d+]/g, '');
    }

    function formatarDataHora(iso) {
        if (!iso) return '-';
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return iso;
        return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    }

    return {
        get: (path) => request('GET', path),
        post: (path, body) => request('POST', path, body),
        put: (path, body) => request('PUT', path, body),
        del: (path) => request('DELETE', path),
        detectarBackend,
        mostrarFeedback,
        escaparHtml,
        limparTelefoneHref,
        formatarDataHora
    };
})();
