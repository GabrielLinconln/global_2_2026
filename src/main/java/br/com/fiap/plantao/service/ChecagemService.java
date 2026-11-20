package br.com.fiap.plantao.service;

import br.com.fiap.plantao.dao.AlertaDao;
import br.com.fiap.plantao.dao.ChecagemDao;
import br.com.fiap.plantao.dao.ConnectionFactory;
import br.com.fiap.plantao.dao.Dao;
import br.com.fiap.plantao.dao.VinculoDao;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Checagem;
import br.com.fiap.plantao.model.Resultado;
import br.com.fiap.plantao.model.TipoAlerta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class ChecagemService {
    private final ChecagemDao checagemDao = new ChecagemDao();
    private final VinculoDao vinculoDao = new VinculoDao();
    private final AlertaDao alertaDao = new AlertaDao();

    // registra o resultado e faz a escalada titular -> reserva -> critico numa transacao:
    // ou grava o resultado E cria a ligacao do reserva, ou nao grava nada
    public String registrar(long idChecagem, String resultado, String canal, String observacao, boolean registradoDepois) {
        Resultado r;
        try {
            r = Resultado.valueOf(resultado);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraNegocioException("Resultado inv\u00e1lido.");
        }
        if (r == Resultado.PENDENTE) throw new RegraNegocioException("Escolha o resultado da liga\u00e7\u00e3o.");
        if (canal == null || canal.isEmpty()) canal = "LIGACAO";
        if (!Arrays.asList("LIGACAO", "VISITA", "MENSAGEM").contains(canal)) throw new RegraNegocioException("Canal inv\u00e1lido.");
        if (observacao != null && observacao.length() > 300) throw new RegraNegocioException("Observa\u00e7\u00e3o com no m\u00e1ximo 300 caracteres.");

        Checagem ch = checagemDao.buscarPorId(idChecagem);
        ch.setResultado(r);
        ch.setCanal(canal);
        ch.setObservacao(observacao == null || observacao.trim().isEmpty() ? null : observacao.trim());
        ch.setRegistradoDepois(registradoDepois);
        TipoAlerta tipo = alertaDao.buscarPorId(ch.getIdAlerta()).getTipo();

        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                checagemDao.registrar(c, ch);
                String mensagem = mensagem(c, ch, tipo);
                c.commit();
                return mensagem;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Dao.erro("registrar a liga\u00e7\u00e3o", e);
        }
    }

    private String mensagem(Connection c, Checagem ch, TipoAlerta tipo) throws SQLException {
        String nome = ch.getIdoso().getNome();
        switch (ch.getResultado()) {
            case URGENTE:
                return tipo == TipoAlerta.CHUVA
                        ? "Ligue agora 193 (Bombeiros) ou 199 (Defesa Civil). Se houver mal-estar, 192 (SAMU)."
                        : "Ligue agora 192 (SAMU). Enquanto espera: lugar fresco, \u00e1gua aos goles, roupa leve.";
            case NAO_ATENDEU:
                if (ch.getTentativa() == 1) {
                    Long reserva = vinculoDao.voluntarioNoPapel(c, ch.getIdIdoso(), "RESERVA");
                    if (reserva != null) {
                        checagemDao.criarTentativaReserva(c, ch.getIdAlerta(), ch.getIdIdoso(), reserva);
                        return "Ningu\u00e9m atendeu. A liga\u00e7\u00e3o para " + nome + " foi passada para o volunt\u00e1rio reserva.";
                    }
                }
                return "CR\u00cdTICO: ningu\u00e9m conseguiu falar com " + nome
                        + (nome.endsWith(".") ? "" : ".") + " Pe\u00e7a para algu\u00e9m ir at\u00e9 a casa agora; se ningu\u00e9m abrir, ligue 193 ou 192"
                        + (tipo == TipoAlerta.CHUVA ? " (ou 199, Defesa Civil)." : ".");
            default:
                return "Registrado.";
        }
    }
}
