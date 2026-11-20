package br.com.fiap.plantao;

import br.com.fiap.plantao.dao.AlertaDao;
import br.com.fiap.plantao.dao.ChecagemDao;
import br.com.fiap.plantao.dao.ConnectionFactory;
import br.com.fiap.plantao.dao.IdosoDao;
import br.com.fiap.plantao.dao.RegiaoDao;
import br.com.fiap.plantao.dao.RelatorioDao;
import br.com.fiap.plantao.dao.VinculoDao;
import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Alerta;
import br.com.fiap.plantao.model.AvaliavelRisco;
import br.com.fiap.plantao.model.Checagem;
import br.com.fiap.plantao.model.Idoso;
import br.com.fiap.plantao.model.Risco;
import br.com.fiap.plantao.model.TipoAlerta;
import br.com.fiap.plantao.model.Vinculo;
import br.com.fiap.plantao.service.AlertaService;
import br.com.fiap.plantao.service.ChecagemService;
import br.com.fiap.plantao.service.InmetService;
import br.com.fiap.plantao.service.PrevisaoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// Roda o fluxo inteiro num banco H2 em memoria (criado pelo sql/script.sql) e confere as regras.
// Uso: java -cp "target/classes:lib/*" br.com.fiap.plantao.Verificacao  (na pasta do projeto)
public class Verificacao {
    private static int ok = 0;

    public static void main(String[] args) {
        ConnectionFactory.usarUrl("jdbc:h2:mem:verificacao;MODE=Oracle;DB_CLOSE_DELAY=-1");
        ConnectionFactory.inicializar();
        IdosoDao idosoDao = new IdosoDao();
        AlertaDao alertaDao = new AlertaDao();
        ChecagemDao checagemDao = new ChecagemDao();
        AlertaService alertaService = new AlertaService();
        ChecagemService checagemService = new ChecagemService();

        checar(new RegiaoDao().listar().size() == 5, "seed: 5 regioes");
        checar(idosoDao.listar().size() == 16, "seed: 16 idosos");

        // regras do cadastro
        Idoso jovem = novoIdoso("Fulano", LocalDate.now().getYear() - 50);
        checar(falha(() -> idosoDao.inserir(jovem)), "rejeita menor de 60");
        Idoso semTel = novoIdoso("Sem Telefone", 1950);
        semTel.setTelefone(null);
        checar(falha(() -> idosoDao.inserir(semTel)), "rejeita idoso sem telefone");

        // CRUD do idoso
        Idoso novo = idosoDao.inserir(novoIdoso("Teste Silva", 1950));
        checar(novo.getId() != null, "insere idoso e recebe id");
        novo.setNome("Teste Souza");
        idosoDao.atualizar(novo);
        checar(idosoDao.buscarPorId(novo.getId()).getNome().equals("Teste Souza"), "atualiza idoso");
        idosoDao.excluir(novo.getId());
        Idoso anon = idosoDao.buscarPorId(novo.getId());
        checar(!anon.isAtivo() && anon.getNome().equals("ANONIMIZADO-" + novo.getId()) && anon.getTelefone() == null,
                "exclusao logica anonimiza nome e telefone");
        checar(idosoDao.listar().size() == 16, "desativado some da lista");

        // risco depende do tipo de alerta
        checar(idosoDao.buscarPorId(1).calcularRisco(TipoAlerta.CALOR) == Risco.ALTO, "Maria: risco ALTO no calor");
        checar(idosoDao.buscarPorId(3).calcularRisco(TipoAlerta.CHUVA) == Risco.ALTO, "Ana: risco ALTO na chuva");
        checar(idosoDao.buscarPorId(2).calcularRisco(TipoAlerta.CALOR) == Risco.BAIXO, "Jose: risco BAIXO no calor");

        // N:N: so um titular por idoso (UNIQUE no banco)
        Vinculo outroTitular = new Vinculo();
        outroTitular.setIdIdoso(1);
        outroTitular.setIdVoluntario(3);
        outroTitular.setPapel("TITULAR");
        checar(falha(() -> new VinculoDao().inserir(outroTitular)), "rejeita segundo titular");

        // alerta de hoje + rodada
        Alerta alerta = new Alerta();
        alerta.setIdRegiao(1);
        alerta.setTipo(TipoAlerta.CALOR);
        alerta.setDataAlerta(LocalDate.now());
        alerta.setNivel("ALERTA");
        alerta.setTempMax(35.5);
        alerta.setOrigem("INMET");
        alerta = alertaDao.inserir(alerta);
        Alerta duplicado = alerta;
        checar(falha(() -> alertaDao.inserir(duplicado)), "rejeita alerta duplicado (regiao, data, tipo)");
        long idAlerta = alerta.getId();
        checar(alertaService.gerarRodada(idAlerta) == 2, "rodada cria 2 ligacoes (titulares da Vila Mariana)");
        checar(alertaService.gerarRodada(idAlerta) == 0, "rodada repetida nao duplica");
        checar(alertaService.listarRodada(idAlerta).get(0).getIdoso().getNome().equals("Maria A."), "rodada comeca pelo risco ALTO");

        // escalada: Maria tem reserva, Jose nao
        Checagem maria = ligacao(alertaService, idAlerta, "Maria A.", 1);
        String msg = checagemService.registrar(maria.getId(), "NAO_ATENDEU", "LIGACAO", null, false);
        checar(msg.contains("reserva"), "titular nao atendeu -> vai para o reserva");
        Checagem mariaReserva = ligacao(alertaService, idAlerta, "Maria A.", 2);
        checar(mariaReserva.getNomeVoluntario().startsWith("Pedro"), "reserva da Maria e o Pedro");
        msg = checagemService.registrar(mariaReserva.getId(), "NAO_ATENDEU", "LIGACAO", null, false);
        checar(msg.startsWith("CR\u00cdTICO"), "reserva nao atendeu -> CRITICO");

        Checagem jose = ligacao(alertaService, idAlerta, "Jose B.", 1);
        msg = checagemService.registrar(jose.getId(), "NAO_ATENDEU", "LIGACAO", null, false);
        checar(msg.startsWith("CR\u00cdTICO"), "sem reserva: titular nao atendeu -> CRITICO direto");
        checar(checagemDao.listarCriticos(idAlerta).size() == 2, "painel mostra 2 criticos");
        checagemService.registrar(jose.getId(), "OK", "VISITA", "Vizinha foi ate a casa, esta bem", true);
        checar(checagemDao.listarCriticos(idAlerta).size() == 1, "contato depois tira o idoso dos criticos");
        checar(falha(() -> checagemService.registrar(jose.getId(), "PENDENTE", null, null, false)), "rejeita voltar para PENDENTE");

        // relatorios
        List<Map<String, Object>> r1 = new RelatorioDao().coberturaPorRegiao(LocalDate.now());
        checar(r1.size() == 1 && ((Number) r1.get(0).get("criticos")).intValue() == 1
                && ((Number) r1.get(0).get("contatados")).intValue() == 1, "R1: 1 contatado e 1 critico hoje");
        checar(new RelatorioDao().furosNaRede().stream().anyMatch(l -> "Jose B.".equals(l.get("idoso"))), "R2: Jose sem reserva");
        checar(!new RelatorioDao().cargaVoluntarios().isEmpty(), "R3: carga por voluntario");

        // interface e 404
        AvaliavelRisco avaliavel = idosoDao.buscarPorId(1);
        checar(avaliavel.calcularRisco(TipoAlerta.CALOR) != null, "Idoso e AvaliavelRisco (interface)");
        try {
            idosoDao.buscarPorId(9999);
            checar(false, "id inexistente deveria falhar");
        } catch (RecursoNaoEncontradoException e) {
            checar(true, "id inexistente -> RecursoNaoEncontradoException (404)");
        }

        // regua de temperatura
        checar("ALERTA".equals(PrevisaoService.nivelPelaRegua(35.0)) && PrevisaoService.nivelPelaRegua(29.9) == null, "regua 30/32/35/37");

        // braco FloodGuard: sensor do corrego gera alerta + rodada sozinho
        checar(!alertaService.leituraSensor(2, 40).containsKey("idAlerta"), "sensor a 40%: nenhum alerta");
        Map<String, Object> s1 = alertaService.leituraSensor(2, 75);
        long idSensor = ((Number) s1.get("idAlerta")).longValue();
        checar("ALERTA".equals(alertaDao.buscarPorId(idSensor).getNivel()) && ((Number) s1.get("criadas")).intValue() == 6,
                "sensor a 75%: alerta de chuva + rodada com 6 ligacoes");
        Map<String, Object> s2 = alertaService.leituraSensor(2, 95);
        checar(((Number) s2.get("idAlerta")).longValue() == idSensor && "MAXIMO".equals(alertaDao.buscarPorId(idSensor).getNivel())
                && ((Number) s2.get("criadas")).intValue() == 0, "sensor a 95%: sobe para MAXIMO sem duplicar alerta nem ligacoes");
        alertaService.leituraSensor(2, 80);
        checar("MAXIMO".equals(alertaDao.buscarPorId(idSensor).getNivel()), "sensor nunca baixa o nivel do dia");

        // avisos oficiais do Inmet: amostra no formato de apiprevmet3.inmet.gov.br/avisos/ativos (sem internet)
        String inmet = "{\"hoje\":[{\"id\":1,\"poligono\":\"{\\\"type\\\":\\\"Polygon\\\"}\",\"municipios\":\"S\\u00e3o Paulo - SP (3550308),Santos - SP (3548500)\","
                + "\"inicio\":\"2026-11-19 08:45\",\"fim\":\"2026-11-19 23:59\",\"descricao\":\"Tempestade\",\"severidade\":\"Perigo Potencial\"},"
                + "{\"id\":2,\"municipios\":\"Sao Paulo - SP (3550308)\",\"inicio\":\"x\",\"fim\":\"y\",\"descricao\":\"Baixa Umidade\",\"severidade\":\"Perigo\"}],"
                + "\"futuro\":[{\"id\":3,\"municipios\":\"Sao Paulo - SP (3550308)\",\"descricao\":\"Onda de Calor\",\"severidade\":\"Grande Perigo\"}]}";
        checar(InmetService.lerAvisos(inmet).size() == 2, "Inmet: le so os avisos de hoje");
        checar(InmetService.atinge("S\u00e3o Paulo - SP (3550308)", "Sao Paulo", "SP")
                && !InmetService.atinge("S\u00e3o Paulo de Oliven\u00e7a - AM (1303908)", "Sao Paulo", "SP"), "Inmet: acha a cidade ignorando acento");
        checar("MAXIMO".equals(InmetService.nivel("Grande Perigo")) && "ALERTA".equals(InmetService.nivel("Perigo"))
                && "ATENCAO".equals(InmetService.nivel("Perigo Potencial")) && InmetService.tipo("Baixa Umidade") == null,
                "Inmet: cores amarelo/laranja/vermelho viram ATENCAO/ALERTA/MAXIMO");
        Map<String, Object> i1 = new InmetService().importar(inmet);
        TreeSet<Long> regioesInmet = new TreeSet<>();
        boolean alertasInmetOk = true;
        for (Object id : (List<?>) i1.get("alertas")) {
            Alerta a = alertaDao.buscarPorId(((Number) id).longValue());
            regioesInmet.add(a.getIdRegiao());
            alertasInmetOk &= a.getTipo() == TipoAlerta.CHUVA && "ATENCAO".equals(a.getNivel()) && "INMET".equals(a.getOrigem())
                    && "Tempestade, aviso amarelo (Perigo Potencial), de 19/11/2026 08:45 a 19/11/2026 23:59".equals(a.getFonteAviso());
        }
        checar(alertasInmetOk && "[1, 3, 4, 5]".equals(regioesInmet.toString()) && ((Number) i1.get("ligacoes")).intValue() == 10,
                "Inmet: tempestade em Sao Paulo vira alerta (com o periodo do aviso) nas 4 regioes de Sao Paulo + 10 ligacoes");
        List<?> avisosInmet = (List<?>) i1.get("avisos");
        Map<?, ?> aviso = avisosInmet.size() == 1 ? (Map<?, ?>) avisosInmet.get(0) : null;
        checar(aviso != null && ((String) aviso.get("poligono")).contains("Polygon")
                && "[Brasil\u00e2ndia, Cap\u00e3o Redondo, Itaquera, Vila Mariana]".equals(String.valueOf(aviso.get("regioes"))),
                "Inmet: avisos traz so o aviso que atingiu regioes, com o poligono e os nomes das regioes");
        checar(((Number) new InmetService().importar(inmet).get("ligacoes")).intValue() == 0, "Inmet: consultar de novo nao duplica nada");

        System.out.println("\nVerificacao concluida: " + ok + " checagens OK.");
    }

    private static Idoso novoIdoso(String nome, int ano) {
        Idoso i = new Idoso();
        i.setIdRegiao(1);
        i.setNome(nome);
        i.setAnoNascimento(ano);
        i.setTelefone("11912345678");
        i.setConsentimentoEm(LocalDate.now());
        i.setConsentidoPor("PROPRIO");
        return i;
    }

    private static Checagem ligacao(AlertaService s, long idAlerta, String nome, int tentativa) {
        for (Checagem ch : s.listarRodada(idAlerta)) {
            if (ch.getIdoso().getNome().equals(nome) && ch.getTentativa() == tentativa) return ch;
        }
        throw new IllegalStateException("ligacao nao encontrada: " + nome + " tentativa " + tentativa);
    }

    private static boolean falha(Runnable r) {
        try {
            r.run();
            return false;
        } catch (RegraNegocioException e) {
            return true;
        }
    }

    private static void checar(boolean condicao, String descricao) {
        if (!condicao) throw new IllegalStateException("FALHOU: " + descricao);
        ok++;
        System.out.println("OK  " + descricao);
    }
}
