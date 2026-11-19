package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.AlertaDao;
import br.com.plantaoclima.dao.ChecagemDao;
import br.com.plantaoclima.dao.IdosoDao;
import br.com.plantaoclima.dao.RegiaoDao;
import br.com.plantaoclima.dao.VinculoCuidadoDao;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.Alerta;
import br.com.plantaoclima.model.Checagem;
import br.com.plantaoclima.model.Idoso;
import br.com.plantaoclima.model.PapelChecagem;
import br.com.plantaoclima.model.PapelVinculo;
import br.com.plantaoclima.model.StatusAlerta;
import br.com.plantaoclima.model.StatusChecagem;
import br.com.plantaoclima.model.VinculoCuidado;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AlertaService {

    private final AlertaDao alertaDao;
    private final RegiaoDao regiaoDao;
    private final IdosoDao idosoDao;
    private final VinculoCuidadoDao vinculoDao;
    private final ChecagemDao checagemDao;

    public AlertaService(AlertaDao alertaDao, RegiaoDao regiaoDao, IdosoDao idosoDao,
                         VinculoCuidadoDao vinculoDao, ChecagemDao checagemDao) {
        this.alertaDao = alertaDao;
        this.regiaoDao = regiaoDao;
        this.idosoDao = idosoDao;
        this.vinculoDao = vinculoDao;
        this.checagemDao = checagemDao;
    }

    public List<Alerta> listarTodos() {
        return alertaDao.listarTodos();
    }

    public Alerta buscarPorId(Long id) {
        return alertaDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta nao encontrado com ID " + id));
    }

    /**
     * Registra um alerta climatico e gera automaticamente a rodada de ligacoes ordenada por risco.
     */
    public Alerta criarEGerarRodada(Alerta alerta) {
        validar(alerta);
        alerta.setStatus(StatusAlerta.ATIVO);
        Alerta salvo = alertaDao.inserir(alerta);

        List<Idoso> idososAlvo = idosoDao.listarAtivosPorRegiao(salvo.getIdRegiao());
        idososAlvo.sort(Comparator.comparingInt((Idoso i) -> i.calcularPontuacaoRisco(salvo.getTipo())).reversed());

        for (Idoso idoso : idososAlvo) {
            int risco = idoso.calcularPontuacaoRisco(salvo.getTipo());
            Optional<VinculoCuidado> titular = vinculoDao.buscarPorIdosoEPapel(idoso.getId(), PapelVinculo.TITULAR);
            Optional<VinculoCuidado> reserva = vinculoDao.buscarPorIdosoEPapel(idoso.getId(), PapelVinculo.RESERVA);

            Checagem checagem = new Checagem();
            checagem.setIdAlerta(salvo.getId());
            checagem.setIdIdoso(idoso.getId());
            checagem.setPontuacaoRisco(risco);
            checagem.setStatus(StatusChecagem.PENDENTE);
            checagem.setTentativas(0);

            if (titular.isPresent()) {
                checagem.setIdVoluntario(titular.get().getIdVoluntario());
                checagem.setPapelAtual(PapelChecagem.TITULAR);
                checagem.setObservacao("Aguardando ligacao do voluntario titular.");
            } else if (reserva.isPresent()) {
                checagem.setIdVoluntario(reserva.get().getIdVoluntario());
                checagem.setPapelAtual(PapelChecagem.RESERVA);
                checagem.setObservacao("Idoso sem titular cadastrado - atribuido ao reserva.");
            } else {
                checagem.setIdVoluntario(null);
                checagem.setPapelAtual(PapelChecagem.COORDENACAO);
                checagem.setObservacao("Idoso sem voluntario vinculado - coordenacao deve assumir ligacao.");
            }
            checagemDao.inserir(checagem);
        }

        return salvo;
    }

    public Alerta atualizar(Long id, Alerta alerta) {
        Alerta existente = buscarPorId(id);
        validar(alerta);
        alerta.setId(id);
        if (alerta.getStatus() == null) {
            alerta.setStatus(existente.getStatus());
        }
        if (alerta.getStatus() == StatusAlerta.ENCERRADO && existente.getEncerradoEm() == null) {
            alerta.setEncerradoEm(LocalDateTime.now());
        } else if (alerta.getStatus() == StatusAlerta.ATIVO) {
            alerta.setEncerradoEm(null);
        } else {
            alerta.setEncerradoEm(existente.getEncerradoEm());
        }
        return alertaDao.atualizar(alerta);
    }

    public Alerta encerrar(Long id) {
        Alerta existente = buscarPorId(id);
        existente.setStatus(StatusAlerta.ENCERRADO);
        existente.setEncerradoEm(LocalDateTime.now());
        return alertaDao.atualizar(existente);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        alertaDao.excluir(id);
    }

    private void validar(Alerta alerta) {
        if (alerta == null) {
            throw new ValidacaoException("Dados do alerta sao obrigatorios.");
        }
        if (alerta.getTitulo() == null || alerta.getTitulo().trim().length() < 5) {
            throw new ValidacaoException("Informe o titulo do alerta (minimo 5 caracteres).");
        }
        if (alerta.getTipo() == null) {
            throw new ValidacaoException("Selecione o tipo do alerta: CALOR ou CHUVA.");
        }
        if (alerta.getSeveridade() == null) {
            throw new ValidacaoException("Selecione a severidade: AMARELO, LARANJA ou VERMELHO.");
        }
        if (alerta.getFonte() == null || alerta.getFonte().trim().isEmpty()) {
            throw new ValidacaoException("Informe a fonte oficial do alerta (ex.: INMET, Defesa Civil).");
        }
        if (alerta.getDescricao() == null || alerta.getDescricao().trim().length() < 10) {
            throw new ValidacaoException("Informe a descricao/orientacao do alerta (minimo 10 caracteres).");
        }
        if (alerta.getIdRegiao() != null && regiaoDao.buscarPorId(alerta.getIdRegiao()).isEmpty()) {
            throw new ValidacaoException("Regiao informada para o alerta nao existe.");
        }
        alerta.setTitulo(alerta.getTitulo().trim());
        alerta.setFonte(alerta.getFonte().trim());
        alerta.setDescricao(alerta.getDescricao().trim());
    }
}
