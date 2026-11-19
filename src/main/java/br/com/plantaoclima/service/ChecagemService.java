package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.ChecagemDao;
import br.com.plantaoclima.dao.VinculoCuidadoDao;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.Checagem;
import br.com.plantaoclima.model.PapelChecagem;
import br.com.plantaoclima.model.PapelVinculo;
import br.com.plantaoclima.model.StatusChecagem;
import br.com.plantaoclima.model.VinculoCuidado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servico responsavel pelo registro das ligacoes e pela regra de escalonamento automatico:
 * TITULAR (NAO_ATENDEU) -> RESERVA -> COORDENACAO (CRITICO).
 */
public class ChecagemService {

    public record ResultadoRegistro(Checagem checagemAtual, boolean escalonado, String mensagem) {
    }

    private final ChecagemDao checagemDao;
    private final VinculoCuidadoDao vinculoDao;

    public ChecagemService(ChecagemDao checagemDao, VinculoCuidadoDao vinculoDao) {
        this.checagemDao = checagemDao;
        this.vinculoDao = vinculoDao;
    }

    public List<Checagem> listarRodadaPorAlerta(Long idAlerta) {
        return checagemDao.listarRodadaAtualPorAlerta(idAlerta);
    }

    public List<Checagem> listarCasosCriticosAtivos() {
        return checagemDao.listarCasosCriticosAtivos();
    }

    public List<Checagem> listarTodas() {
        return checagemDao.listarTodos();
    }

    public Checagem buscarPorId(Long id) {
        return checagemDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Checagem nao encontrada com ID " + id));
    }

    public ResultadoRegistro registrarResultado(Long idChecagem, StatusChecagem novoStatus,
                                                String checklistResumo, String observacao) {
        if (novoStatus == null || novoStatus == StatusChecagem.PENDENTE) {
            throw new ValidacaoException("Informe um resultado valido para a ligacao.");
        }

        Checagem atual = buscarPorId(idChecagem);
        atual.setStatus(novoStatus);
        atual.setTentativas(atual.getTentativas() + 1);
        atual.setChecklistResumo(checklistResumo != null ? checklistResumo.trim() : null);
        atual.setObservacao(observacao != null && !observacao.trim().isEmpty()
                ? observacao.trim()
                : atual.getObservacao());
        atual.setAtualizadoEm(LocalDateTime.now());
        Checagem atualizada = checagemDao.atualizar(atual);

        if (novoStatus == StatusChecagem.NAO_ATENDEU) {
            return executarEscalonamento(atualizada);
        }

        if (novoStatus == StatusChecagem.PRECISA_AJUDA) {
            return new ResultadoRegistro(
                    atualizada,
                    false,
                    "EMERGENCIA REGISTRADA: Oriente acionar imediatamente o SAMU 192, Bombeiros 193 ou Defesa Civil 199."
            );
        }

        if (novoStatus == StatusChecagem.PRECISA_ATENCAO) {
            return new ResultadoRegistro(
                    atualizada,
                    false,
                    "Checklist de orientacao registrado. Mantenha acompanhamento prioritario deste idoso."
            );
        }

        return new ResultadoRegistro(atualizada, false, "Checagem registrada com sucesso: idoso esta bem.");
    }

    private ResultadoRegistro executarEscalonamento(Checagem atual) {
        if (atual.getPapelAtual() == PapelChecagem.TITULAR) {
            Optional<VinculoCuidado> reservaOpt =
                    vinculoDao.buscarPorIdosoEPapel(atual.getIdIdoso(), PapelVinculo.RESERVA);

            if (reservaOpt.isPresent()) {
                Optional<Checagem> etapaReservaExistente = checagemDao.buscarPorAlertaIdosoEPapel(
                        atual.getIdAlerta(), atual.getIdIdoso(), PapelChecagem.RESERVA);
                if (etapaReservaExistente.isEmpty()) {
                    VinculoCuidado reserva = reservaOpt.get();
                    Checagem novaEtapa = new Checagem();
                    novaEtapa.setIdAlerta(atual.getIdAlerta());
                    novaEtapa.setIdIdoso(atual.getIdIdoso());
                    novaEtapa.setIdVoluntario(reserva.getIdVoluntario());
                    novaEtapa.setPapelAtual(PapelChecagem.RESERVA);
                    novaEtapa.setPontuacaoRisco(atual.getPontuacaoRisco());
                    novaEtapa.setStatus(StatusChecagem.PENDENTE);
                    novaEtapa.setTentativas(0);
                    novaEtapa.setObservacao("Escalonado automaticamente apos titular nao obter resposta.");
                    Checagem criada = checagemDao.inserir(novaEtapa);
                    return new ResultadoRegistro(
                            criada,
                            true,
                            "Titular nao obteve resposta. Ligacao passada automaticamente para o voluntario RESERVA ("
                                    + reserva.getNomeVoluntario() + ")."
                    );
                }
            }
            // Sem reserva cadastrado -> sobe direto para CRITICO na Coordenacao
            return escalonarParaCritico(atual,
                    "Titular nao obteve resposta e idoso nao possui reserva cadastrado. Enviar alguem a casa e ligar 193, 192 ou 199.");
        }

        if (atual.getPapelAtual() == PapelChecagem.RESERVA) {
            return escalonarParaCritico(atual,
                    "Titular e Reserva nao obtiveram resposta. Caso CRITICO: enviar alguem ate a residencia e acionar 193, 192 ou 199.");
        }

        // Ja estava na Coordenacao -> promove para CRITICO
        atual.setStatus(StatusChecagem.CRITICO);
        atual.setAtualizadoEm(LocalDateTime.now());
        atual.setObservacao("CRITICO: Sem resposta na checagem da coordenacao. Enviar visita presencial e acionar 193, 192 ou 199.");
        Checagem critico = checagemDao.atualizar(atual);
        return new ResultadoRegistro(
                critico,
                true,
                "CASO CRITICO: Sem resposta. Envie alguem ate o endereco e ligue 193, 192 ou 199."
        );
    }

    private ResultadoRegistro escalonarParaCritico(Checagem origem, String motivo) {
        Optional<Checagem> coordOpt = checagemDao.buscarPorAlertaIdosoEPapel(
                origem.getIdAlerta(), origem.getIdIdoso(), PapelChecagem.COORDENACAO);

        Checagem critico;
        if (coordOpt.isPresent()) {
            Checagem existente = coordOpt.get();
            existente.setStatus(StatusChecagem.CRITICO);
            existente.setObservacao(motivo);
            existente.setAtualizadoEm(LocalDateTime.now());
            critico = checagemDao.atualizar(existente);
        } else {
            Checagem novoCritico = new Checagem();
            novoCritico.setIdAlerta(origem.getIdAlerta());
            novoCritico.setIdIdoso(origem.getIdIdoso());
            novoCritico.setIdVoluntario(null);
            novoCritico.setPapelAtual(PapelChecagem.COORDENACAO);
            novoCritico.setPontuacaoRisco(origem.getPontuacaoRisco());
            novoCritico.setStatus(StatusChecagem.CRITICO);
            novoCritico.setTentativas(0);
            novoCritico.setObservacao(motivo);
            novoCritico.setAtualizadoEm(LocalDateTime.now());
            critico = checagemDao.inserir(novoCritico);
        }

        return new ResultadoRegistro(
                critico,
                true,
                "CASO CRITICO NO PAINEL: " + motivo
        );
    }

    public void excluir(Long id) {
        buscarPorId(id);
        checagemDao.excluir(id);
    }
}
