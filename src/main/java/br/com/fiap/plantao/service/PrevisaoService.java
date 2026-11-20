package br.com.fiap.plantao.service;

import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Regiao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

// Sugestao de temperatura maxima pelo Open-Meteo (sem chave). So sugere:
// a fonte principal e o aviso do Inmet/Defesa Civil digitado pelo coordenador.
public class PrevisaoService {

    // regua do grupo, inspirada em Campinas; nao e protocolo oficial
    public static String nivelPelaRegua(double tempMax) {
        if (tempMax >= 37) return "MAXIMO";
        if (tempMax >= 35) return "ALERTA";
        if (tempMax >= 32) return "ATENCAO";
        if (tempMax >= 30) return "OBSERVACAO";
        return null;
    }

    public Map<String, Object> sugerir(Regiao r, LocalDate dia) {
        if (r.getLatitude() == null || r.getLongitude() == null) {
            throw new RegraNegocioException("A regi\u00e3o n\u00e3o tem latitude/longitude. Digite a temperatura do aviso.");
        }
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + r.getLatitude() + "&longitude=" + r.getLongitude()
                + "&daily=temperature_2m_max&timezone=America%2FSao_Paulo&start_date=" + dia + "&end_date=" + dia;
        double tempMax = extrairTempMax(baixar(url, "Open-Meteo", "Digite a temperatura do aviso."));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tempMax", tempMax);
        m.put("nivelSugerido", nivelPelaRegua(tempMax));
        m.put("fonte", "Open-Meteo (previs\u00e3o), r\u00e9gua do grupo 30/32/35/37 C - n\u00e3o \u00e9 protocolo oficial");
        return m;
    }

    // le "temperature_2m_max":[27.1] sem biblioteca de JSON
    static double extrairTempMax(String json) {
        String chave = "\"temperature_2m_max\":[";
        int i = json.indexOf(chave);
        if (i < 0) throw new RegraNegocioException("Previs\u00e3o indispon\u00edvel para essa data. Digite a temperatura.");
        int ini = i + chave.length();
        String valor = json.substring(ini, json.indexOf(']', ini)).split(",")[0].trim();
        if (valor.isEmpty() || "null".equals(valor)) {
            throw new RegraNegocioException("Previs\u00e3o indispon\u00edvel para essa data. Digite a temperatura.");
        }
        return Double.parseDouble(valor);
    }

    // GET simples para APIs publicas (Open-Meteo, Inmet); seAFalhar diz ao usuario o que fazer sem a API
    static String baixar(String url, String servico, String seAFalhar) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(30000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (PlantaoClima)");
            if (con.getResponseCode() != 200) {
                throw new RegraNegocioException(servico + " respondeu " + con.getResponseCode() + ". " + seAFalhar);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = in.readLine()) != null) sb.append(linha);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RegraNegocioException("Sem conex\u00e3o com o " + servico + ". " + seAFalhar);
        }
    }
}
