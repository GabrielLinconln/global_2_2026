/**
 * Plantão Clima — Motion & Interatividade Temática
 * 1) Monitor SVG animado de Calor / Chuva (homenagem FloodGuard) + fluxo de escalonamento ao vivo
 * 2) Contagem suave (count-up) nos indicadores numéricos
 * 3) Entrada escalonada leve nas seções e cards
 */
const PlantaoMotion = (() => {
    let simTimer = null;
    let modoAtual = 'CALOR';

    function animarNumero(el, valorFinal, sufixo = '', duracao = 520) {
        if (!el) return;
        const alvo = Number(valorFinal);
        if (Number.isNaN(alvo)) {
            el.textContent = `${valorFinal}${sufixo}`;
            return;
        }
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            el.textContent = `${alvo}${sufixo}`;
            return;
        }
        const inicio = performance.now();
        const temDecimal = !Number.isInteger(alvo);
        function passo(agora) {
            const t = Math.min(1, (agora - inicio) / duracao);
            const ease = 1 - Math.pow(1 - t, 3);
            const atual = alvo * ease;
            el.textContent = (temDecimal ? atual.toFixed(1) : Math.round(atual)) + sufixo;
            if (t < 1) {
                requestAnimationFrame(passo);
            } else {
                el.textContent = `${alvo}${sufixo}`;
            }
        }
        requestAnimationFrame(passo);
    }

    function iniciarEntradas() {
        const alvos = document.querySelectorAll('.pc-card, .metrics-strip, .critical-box');
        alvos.forEach((el, idx) => {
            el.classList.add('pc-reveal');
            el.style.animationDelay = `${Math.min(idx * 55, 260)}ms`;
        });
    }

    function atualizarModoClima(modo) {
        modoAtual = modo === 'CHUVA' ? 'CHUVA' : 'CALOR';
        const stage = document.getElementById('climateStage');
        if (!stage) return;

        stage.dataset.mode = modoAtual;

        const btnCalor = document.getElementById('btnModoCalor');
        const btnChuva = document.getElementById('btnModoChuva');
        if (btnCalor && btnChuva) {
            btnCalor.classList.toggle('is-active', modoAtual === 'CALOR');
            btnChuva.classList.toggle('is-active', modoAtual === 'CHUVA');
        }

        const criterio = document.getElementById('motionCriterio');
        if (criterio) {
            criterio.textContent = modoAtual === 'CALOR'
                ? 'PESO CALOR · Doença crônica (+35) · Casa sem ventilação (+30) · Idade (+25)'
                : 'PESO CHUVA · Área de risco / alagamento (+35) · Mobilidade reduzida (+30) · Idade (+25)';
        }

        const tituloEvento = document.getElementById('nodeAlertaSub');
        if (tituloEvento) {
            tituloEvento.textContent = modoAtual === 'CALOR' ? 'INMET · Onda de Calor' : 'Defesa Civil · Chuva Forte';
        }
    }

    function definirPassoFluxo(stepIndex, legenda) {
        for (let i = 1; i <= 4; i++) {
            const node = document.getElementById(`flowNode${i}`);
            if (node) {
                node.classList.toggle('is-active', i === stepIndex);
                node.classList.toggle('is-done', i < stepIndex);
                node.classList.toggle('is-critical-step', i === 4 && stepIndex === 4);
            }
        }
        for (let i = 1; i <= 3; i++) {
            const wire = document.getElementById(`flowWire${i}`);
            if (wire) {
                wire.classList.toggle('is-flowing', i < stepIndex);
            }
        }
        const statusEl = document.getElementById('motionStatusText');
        if (statusEl && legenda) {
            statusEl.textContent = legenda;
        }
    }

    function simularEscalonamento() {
        if (simTimer) {
            simTimer.forEach(clearTimeout);
        }
        const btn = document.getElementById('btnSimularFluxo');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Simulando...';
        }

        const evento = modoAtual === 'CALOR' ? 'alerta de calor extremo' : 'alerta de chuva intensa';
        const passos = [
            { ms: 0, step: 1, msg: `1/4 · ${evento.charAt(0).toUpperCase() + evento.slice(1)} registrado. Fila ordenada por pontuação de risco.` },
            { ms: 1300, step: 2, msg: '2/4 · Voluntário Titular liga para o idoso — sem resposta na chamada.' },
            { ms: 2700, step: 3, msg: '3/4 · Escalonamento automático acionado: ligação transferida ao Reserva — sem resposta.' },
            { ms: 4100, step: 4, msg: '4/4 · Caso promovido para CRÍTICO no topo do painel · Orientação: visita local + 192 / 193 / 199.' }
        ];

        simTimer = passos.map(p => setTimeout(() => definirPassoFluxo(p.step, p.msg), p.ms));
        simTimer.push(setTimeout(() => {
            if (btn) {
                btn.disabled = false;
                btn.textContent = 'Simular escalonamento';
            }
        }, 4400));
    }

    function iniciarMonitor() {
        const stage = document.getElementById('climateStage');
        if (!stage) return;

        document.getElementById('btnModoCalor')?.addEventListener('click', () => {
            atualizarModoClima('CALOR');
            definirPassoFluxo(1, 'Monitoramento em modo CALOR · Clique em "Simular escalonamento" para ver o protocolo.');
        });
        document.getElementById('btnModoChuva')?.addEventListener('click', () => {
            atualizarModoClima('CHUVA');
            definirPassoFluxo(1, 'Monitoramento em modo CHUVA (FloodGuard) · Clique em "Simular escalonamento" para ver o protocolo.');
        });
        document.getElementById('btnSimularFluxo')?.addEventListener('click', simularEscalonamento);

        atualizarModoClima('CALOR');
        definirPassoFluxo(4, 'Fluxo ativo: 1 caso crítico escalonado automaticamente no alerta em vigor.');
    }

    document.addEventListener('DOMContentLoaded', () => {
        iniciarEntradas();
        iniciarMonitor();
    });

    return {
        animarNumero,
        atualizarModoClima,
        simularEscalonamento
    };
})();
