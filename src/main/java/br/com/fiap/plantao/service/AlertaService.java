package br.com.fiap.plantao.service;

import br.com.fiap.plantao.dao.AlertaDao;
import br.com.fiap.plantao.dao.ChecagemDao;
import br.com.fiap.plantao.dao.ConnectionFactory;
import br.com.fiap.plantao.dao.Dao;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Alerta;
import br.com.fiap.plantao.model.Checagem;
import br.com.fiap.plantao.model.Resultado;
import br.com.fiap.plantao.model.TipoAlerta;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlertaService {
    private final AlertaDao alertaDao = new AlertaDao();
    private final ChecagemDao checagemDao = new ChecagemDao();

    // cria uma ligacao pendente para o titular de cada idoso da regiao; rodar de novo nao duplica
    public int gerarRodada(long idAlerta) {
        Alerta a = alertaDao.buscarPorId(idAlerta);
        try (Connection c = ConnectionFactory.getConnection()) {
            int criadas = checagemDao.criarRodada(c, idAlerta, a.getIdRegiao());
            if (criadas == 0 && checagemDao.listarPorAlerta(idAlerta).isEmpty()) {
                throw new RegraNegocioException("Nenhum idoso dessa regi\u00e3o tem volunt\u00e1rio TITULAR. Cadastre os v\u00ednculos.");
            }
            return criadas;
        } catch (SQLException e) {
            throw Dao.erro("gerar a rodada", e);
        }
    }

    // braco FloodGuard: o sensor do corrego manda o nivel da agua (% da cota de transbordo).
    // 70% vira alerta de chuva, 90% vira nivel maximo, e a rodada de ligacoes sai na hora, sem cadastro manual.
    public Map<String, Object> leituraSensor(long idRegiao, int percentual) {
        if (percentual < 0 || percentual > 200) {
            throw new RegraNegocioException("Leitura do sensor fora da faixa (0 a 200% da cota de transbordo).");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        if (percentual < 70) {
            r.put("mensagem", "N\u00edvel normal: " + percentual + "% da cota de transbordo. Nenhum alerta.");
            return r;
        }
        String nivel = percentual >= 90 ? "MAXIMO" : "ALERTA";
        Alerta a = alertaDoDia(idRegiao, TipoAlerta.CHUVA, nivel, "FLOODGUARD",
                "Sensor FloodGuard: c\u00f3rrego a " + percentual + "% da cota de transbordo");
        int criadas = gerarRodada(a.getId());
        r.put("mensagem", "Alerta de chuva (" + a.getNivel() + ") ativo. " + criadas + " liga\u00e7\u00f5es novas na rodada.");
        r.put("idAlerta", a.getId());
        r.put("criadas", criadas);
        return r;
    }

    // alerta de hoje da regiao para o tipo: cria se nao existe; se existe, so sobe o nivel (nunca baixa)
    public Alerta alertaDoDia(long idRegiao, TipoAlerta tipo, String nivel, String origem, String fonte) {
        LocalDate hoje = LocalDate.now();
        Alerta a = alertaDao.listar().stream()
                .filter(x -> x.getIdRegiao() == idRegiao && x.getTipo() == tipo && hoje.equals(x.getDataAlerta()))
                .findFirst().orElse(null);
        List<String> escala = Arrays.asList("OBSERVACAO", "ATENCAO", "ALERTA", "MAXIMO");
        if (a == null) {
            a = new Alerta();
            a.setIdRegiao(idRegiao);
            a.setTipo(tipo);
            a.setDataAlerta(hoje);
            a.setNivel(nivel);
            a.setOrigem(origem);
            a.setFonteAviso(fonte);
            return alertaDao.inserir(a);
        }
        if (escala.indexOf(nivel) > escala.indexOf(a.getNivel())) {
            a.setNivel(nivel);
            a.setFonteAviso(fonte);
            alertaDao.atualizar(a);
        }
        return a;
    }

    // ligacoes do alerta, risco ALTO primeiro (o risco depende do tipo: calor ou chuva)
    public List<Checagem> listarRodada(long idAlerta) {
        Alerta a = alertaDao.buscarPorId(idAlerta);
        List<Checagem> rodada = checagemDao.listarPorAlerta(idAlerta);
        for (Checagem ch : rodada) {
            ch.setRisco(ch.getIdoso().calcularRisco(a.getTipo()));
        }
        rodada.sort(Comparator.comparing(Checagem::getRisco)
                .thenComparing(ch -> ch.getIdoso().getNome())
                .thenComparing(Checagem::getTentativa));
        return rodada;
    }

    // painel do dia: alerta mais recente (ou o escolhido), contadores, criticos e rodada
    public Map<String, Object> painel(Long idAlerta) {
        Map<String, Object> painel = new LinkedHashMap<>();
        if (idAlerta == null) {
            List<Alerta> alertas = alertaDao.listar();
            if (alertas.isEmpty()) return painel;
            idAlerta = alertas.get(0).getId();
        }
        List<Checagem> rodada = listarRodada(idAlerta);
        List<Map<String, Object>> criticos = checagemDao.listarCriticos(idAlerta);
        Map<String, Integer> contadores = new LinkedHashMap<>();
        for (Resultado r : Resultado.values()) contadores.put(r.name(), 0);
        for (Checagem ch : rodada) contadores.merge(ch.getResultado().name(), 1, Integer::sum);
        contadores.put("CRITICO", criticos.size());

        painel.put("alerta", alertaDao.buscarPorId(idAlerta));
        painel.put("contadores", contadores);
        painel.put("criticos", criticos);
        painel.put("rodada", rodada);
        return painel;
    }
}
