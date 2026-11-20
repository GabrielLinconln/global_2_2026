package br.com.fiap.plantao.service;

import br.com.fiap.plantao.dao.RegiaoDao;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Alerta;
import br.com.fiap.plantao.model.Regiao;
import br.com.fiap.plantao.model.TipoAlerta;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Avisos oficiais do Inmet em vigor hoje (API publica, sem chave). Para cada regiao cadastrada,
// se a cidade dela esta num aviso de chuva/tempestade ou de calor, cria o alerta do dia
// (ou sobe o nivel) e gera a rodada de ligacoes. O App chama a cada 30 minutos.
public class InmetService {
    static final String URL_AVISOS = "https://apiprevmet3.inmet.gov.br/avisos/ativos";
    private static volatile Map<String, Object> ultima;
    private static final Map<String, String> COR = new LinkedHashMap<>();
    static {
        COR.put("OBSERVACAO", "de observa\u00e7\u00e3o");
        COR.put("ATENCAO", "amarelo");
        COR.put("ALERTA", "laranja");
        COR.put("MAXIMO", "vermelho");
    }

    private final RegiaoDao regiaoDao = new RegiaoDao();
    private final AlertaService alertaService = new AlertaService();

    public Map<String, Object> importar() {
        return importar(PrevisaoService.baixar(URL_AVISOS, "Inmet", "Cadastre o aviso manualmente."));
    }

    // separado do download para poder testar sem internet
    public Map<String, Object> importar(String json) {
        List<Map<String, String>> avisos = lerAvisos(json);
        Set<Long> alertas = new LinkedHashSet<>();
        List<String> regioes = new ArrayList<>();
        Map<Map<String, String>, Set<String>> atingidas = new LinkedHashMap<>();
        int ligacoes = 0;
        for (Regiao r : regiaoDao.listar()) {
            for (Map<String, String> av : avisos) {
                TipoAlerta tipo = tipo(av.get("descricao"));
                if (tipo == null || !atinge(av.get("municipios"), r.getCidade(), r.getUf())) continue;
                String nivel = nivel(av.get("severidade"));
                String fonte = av.get("descricao") + ", aviso " + COR.get(nivel) + " (" + av.get("severidade") + ")"
                        + periodo(av.get("inicio"), av.get("fim"));
                Alerta a = alertaService.alertaDoDia(r.getId(), tipo, nivel, "INMET",
                        fonte.length() > 200 ? fonte.substring(0, 200) : fonte);
                ligacoes += alertaService.gerarRodada(a.getId());
                if (alertas.add(a.getId())) regioes.add(r.getNome() + ": " + av.get("descricao") + " (" + av.get("severidade") + ")");
                atingidas.computeIfAbsent(av, k -> new LinkedHashSet<>()).add(r.getNome());
            }
        }
        // para o mapa: so os avisos que atingiram alguma regiao, com o poligono GeoJSON como veio
        List<Map<String, Object>> noMapa = new ArrayList<>();
        for (Map.Entry<Map<String, String>, Set<String>> e : atingidas.entrySet()) {
            Map<String, String> av = e.getKey();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("descricao", av.get("descricao"));
            item.put("severidade", av.get("severidade"));
            item.put("nivel", nivel(av.get("severidade")));
            item.put("inicio", av.get("inicio"));
            item.put("fim", av.get("fim"));
            item.put("poligono", av.get("poligono"));
            item.put("regioes", e.getValue());
            noMapa.add(item);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("consultadoEm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        m.put("avisosHoje", avisos.size());
        m.put("alertas", new ArrayList<>(alertas));
        m.put("regioes", regioes);
        m.put("ligacoes", ligacoes);
        m.put("mensagem", alertas.isEmpty()
                ? avisos.size() + " avisos do Inmet hoje no Brasil, nenhum de chuva ou calor nas regi\u00f5es cadastradas."
                : alertas.size() + " alerta(s) do Inmet nas regi\u00f5es cadastradas. " + ligacoes + " liga\u00e7\u00f5es novas na rodada.");
        m.put("avisos", noMapa);
        ultima = m;
        return m;
    }

    // "2026-11-19 08:45" e "2026-11-19 23:59" viram ", de 19/11/2026 08:45 a 19/11/2026 23:59"
    static String periodo(String inicio, String fim) {
        if (inicio.length() < 16 || fim.length() < 16) return "";
        return ", de " + dataHora(inicio) + " a " + dataHora(fim);
    }

    private static String dataHora(String s) {
        return s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4) + s.substring(10, 16);
    }

    public static Map<String, Object> ultimaConsulta() {
        Map<String, Object> m = ultima;
        return m != null ? m : Collections.singletonMap("mensagem", "Ainda n\u00e3o consultado.");
    }

    // so a lista "hoje"; cada aviso comeca com {"id":
    public static List<Map<String, String>> lerAvisos(String json) {
        int ini = json.indexOf("\"hoje\":[");
        if (ini < 0) throw new RegraNegocioException("Resposta do Inmet em formato inesperado.");
        int fim = json.indexOf("\"futuro\":[", ini);
        String hoje = json.substring(ini, fim > ini ? fim : json.length());
        List<Map<String, String>> avisos = new ArrayList<>();
        String[] partes = hoje.split("\\{\"id\":");
        for (int i = 1; i < partes.length; i++) {
            Map<String, String> av = new LinkedHashMap<>();
            for (String c : new String[] {"descricao", "severidade", "municipios", "inicio", "fim", "poligono"}) {
                av.put(c, campo(partes[i], c));
            }
            avisos.add(av);
        }
        return avisos;
    }

    // valor de "nome":"..." respeitando \" e \\uXXXX
    static String campo(String obj, String nome) {
        String chave = "\"" + nome + "\":\"";
        int i = obj.indexOf(chave);
        if (i < 0) return "";
        StringBuilder sb = new StringBuilder();
        for (i += chave.length(); i < obj.length(); i++) {
            char ch = obj.charAt(i);
            if (ch == '"') break;
            if (ch == '\\' && i + 1 < obj.length()) {
                char prox = obj.charAt(++i);
                if (prox == 'u' && i + 4 < obj.length()) {
                    sb.append((char) Integer.parseInt(obj.substring(i + 1, i + 5), 16));
                    i += 4;
                } else {
                    sb.append(prox);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // municipios vem como "Sao Paulo - SP (3550308),Santos - SP (3548500),..."
    public static boolean atinge(String municipios, String cidade, String uf) {
        String alvo = normalizar(cidade + " - " + uf + " (");
        for (String m : municipios.split(",")) {
            if (normalizar(m.trim()).startsWith(alvo)) return true;
        }
        return false;
    }

    public static TipoAlerta tipo(String descricao) {
        String d = normalizar(descricao);
        if (d.contains("calor")) return TipoAlerta.CALOR;
        if (d.contains("chuva") || d.contains("tempestade")) return TipoAlerta.CHUVA;
        return null;
    }

    // cores do Inmet: amarelo = Perigo Potencial, laranja = Perigo, vermelho = Grande Perigo
    public static String nivel(String severidade) {
        String s = normalizar(severidade);
        if (s.startsWith("grande perigo")) return "MAXIMO";
        if (s.contains("potencial")) return "ATENCAO";
        if (s.startsWith("perigo")) return "ALERTA";
        return "OBSERVACAO";
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
