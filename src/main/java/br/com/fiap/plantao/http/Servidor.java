package br.com.fiap.plantao.http;

import br.com.fiap.plantao.dao.AlertaDao;
import br.com.fiap.plantao.dao.IdosoDao;
import br.com.fiap.plantao.dao.RegiaoDao;
import br.com.fiap.plantao.dao.RelatorioDao;
import br.com.fiap.plantao.dao.VinculoDao;
import br.com.fiap.plantao.dao.VoluntarioDao;
import br.com.fiap.plantao.exception.PersistenciaException;
import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Alerta;
import br.com.fiap.plantao.model.Idoso;
import br.com.fiap.plantao.model.Regiao;
import br.com.fiap.plantao.model.TipoAlerta;
import br.com.fiap.plantao.model.Vinculo;
import br.com.fiap.plantao.model.Voluntario;
import br.com.fiap.plantao.service.AlertaService;
import br.com.fiap.plantao.service.ChecagemService;
import br.com.fiap.plantao.service.InmetService;
import br.com.fiap.plantao.service.PrevisaoService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

// Servidor HTTP do JDK: /api/* devolve JSON, o resto serve os arquivos da pasta web/.
// So GET e POST com formulario (application/x-www-form-urlencoded).
public class Servidor {
    private static final Path WEB = Paths.get("web").toAbsolutePath().normalize();

    private final RegiaoDao regiaoDao = new RegiaoDao();
    private final IdosoDao idosoDao = new IdosoDao();
    private final VoluntarioDao voluntarioDao = new VoluntarioDao();
    private final VinculoDao vinculoDao = new VinculoDao();
    private final AlertaDao alertaDao = new AlertaDao();
    private final RelatorioDao relatorioDao = new RelatorioDao();
    private final AlertaService alertaService = new AlertaService();
    private final ChecagemService checagemService = new ChecagemService();
    private final PrevisaoService previsaoService = new PrevisaoService();
    private final InmetService inmetService = new InmetService();

    public void iniciar(int porta) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", porta), 0);
        server.createContext("/api/", this::api);
        server.createContext("/", this::arquivo);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private void api(HttpExchange ex) throws IOException {
        int status = 200;
        String corpo;
        try {
            String rota = ex.getRequestMethod() + " " + ex.getRequestURI().getPath();
            Map<String, String> q = form(ex.getRequestURI().getRawQuery());
            Map<String, String> f = "POST".equals(ex.getRequestMethod()) ? form(lerCorpo(ex.getRequestBody())) : q;
            corpo = Json.of(rotear(rota, q, f));
        } catch (RecursoNaoEncontradoException e) {
            status = 404;
            corpo = erro(e.getMessage());
        } catch (RegraNegocioException e) {
            status = 400;
            corpo = erro(e.getMessage());
        } catch (NumberFormatException | DateTimeParseException e) {
            status = 400;
            corpo = erro("Dado em formato inv\u00e1lido. Confira n\u00fameros e datas.");
        } catch (PersistenciaException e) {
            e.printStackTrace();
            status = 500;
            corpo = erro(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            status = 500;
            corpo = erro("Erro inesperado no servidor.");
        }
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        enviar(ex, status, "application/json; charset=utf-8", corpo.getBytes(StandardCharsets.UTF_8));
    }

    private Object rotear(String rota, Map<String, String> q, Map<String, String> f) {
        switch (rota) {
            // regioes
            case "GET /api/regioes": return q.containsKey("id") ? regiaoDao.buscarPorId(id(q)) : regiaoDao.listar();
            case "POST /api/regioes": {
                Regiao r = regiao(f);
                if (r.getId() == null) regiaoDao.inserir(r); else regiaoDao.atualizar(r);
                return r;
            }
            case "POST /api/regioes/excluir": regiaoDao.excluir(id(f)); return ok();
            // idosos
            case "GET /api/idosos": return q.containsKey("id") ? idosoDao.buscarPorId(id(q)) : idosoDao.listar();
            case "POST /api/idosos": {
                Idoso i = idoso(f);
                if (i.getId() == null) idosoDao.inserir(i); else idosoDao.atualizar(i);
                return i;
            }
            case "POST /api/idosos/excluir": idosoDao.excluir(id(f)); return ok();
            // voluntarios
            case "GET /api/voluntarios": return q.containsKey("id") ? voluntarioDao.buscarPorId(id(q)) : voluntarioDao.listar();
            case "POST /api/voluntarios": {
                Voluntario v = voluntario(f);
                if (v.getId() == null) voluntarioDao.inserir(v); else voluntarioDao.atualizar(v);
                return v;
            }
            case "POST /api/voluntarios/excluir": voluntarioDao.excluir(id(f)); return ok();
            // vinculos (N:N)
            case "GET /api/vinculos": return vinculoDao.listar();
            case "POST /api/vinculos": vinculoDao.inserir(vinculo(f)); return ok();
            case "POST /api/vinculos/papel": vinculoDao.atualizarPapel(vinculo(f)); return ok();
            case "POST /api/vinculos/excluir":
                vinculoDao.excluir(Long.parseLong(f.get("idIdoso")), Long.parseLong(f.get("idVoluntario")));
                return ok();
            // alertas, rodada e ligacoes
            case "GET /api/alertas": return q.containsKey("id") ? alertaDao.buscarPorId(id(q)) : alertaDao.listar();
            case "POST /api/alertas": {
                Alerta a = alerta(f);
                return a.getId() == null ? alertaDao.inserir(a) : atualizar(a);
            }
            case "POST /api/alertas/excluir": alertaDao.excluir(id(f)); return ok();
            case "POST /api/rodada": return Collections.singletonMap("criadas", alertaService.gerarRodada(id(f)));
            case "POST /api/sensor":
                return alertaService.leituraSensor(Long.parseLong(f.get("idRegiao")), Integer.parseInt(f.get("percentual")));
            case "GET /api/painel": return alertaService.painel(vazio(q.get("alerta")) ? null : Long.parseLong(q.get("alerta")));
            case "POST /api/checagens":
                return Collections.singletonMap("mensagem", checagemService.registrar(id(f), f.get("resultado"),
                        f.get("canal"), f.get("observacao"), "S".equals(f.get("registradoDepois"))));
            case "GET /api/inmet": return InmetService.ultimaConsulta();
            case "POST /api/inmet": return inmetService.importar();
            case "GET /api/previsao":
                return previsaoService.sugerir(regiaoDao.buscarPorId(Long.parseLong(q.get("regiao"))), LocalDate.parse(q.get("data")));
            // relatorios
            case "GET /api/relatorios/cobertura":
                return relatorioDao.coberturaPorRegiao(vazio(q.get("data")) ? LocalDate.now() : LocalDate.parse(q.get("data")));
            case "GET /api/relatorios/furos": return relatorioDao.furosNaRede();
            case "GET /api/relatorios/voluntarios": return relatorioDao.cargaVoluntarios();
            default: throw new RecursoNaoEncontradoException("Rota n\u00e3o encontrada: " + rota);
        }
    }

    private Alerta atualizar(Alerta a) {
        alertaDao.atualizar(a);
        return alertaDao.buscarPorId(a.getId());
    }

    // ===== formulario -> objeto do model =====

    private Regiao regiao(Map<String, String> f) {
        Regiao r = new Regiao();
        r.setId(idOpcional(f));
        r.setNome(f.get("nome"));
        r.setCidade(f.get("cidade"));
        r.setUf(f.get("uf") == null ? null : f.get("uf").toUpperCase());
        r.setLatitude(vazio(f.get("latitude")) ? null : Double.valueOf(f.get("latitude")));
        r.setLongitude(vazio(f.get("longitude")) ? null : Double.valueOf(f.get("longitude")));
        return r;
    }

    private Idoso idoso(Map<String, String> f) {
        Idoso i = new Idoso();
        i.setId(idOpcional(f));
        i.setIdRegiao(Long.parseLong(f.get("idRegiao")));
        i.setNome(f.get("nome"));
        i.setAnoNascimento(Integer.parseInt(f.get("anoNascimento")));
        i.setTelefone(f.get("telefone"));
        i.setTelefoneFixo(f.get("telefoneFixo"));
        i.setEnderecoRef(f.get("enderecoRef"));
        i.setContatoFamiliar(f.get("contatoFamiliar"));
        i.setMoraSozinho(!"N".equals(f.get("moraSozinho")));
        i.setCondicaoCronica("S".equals(f.get("condicaoCronica")));
        i.setSemVentilacao("S".equals(f.get("semVentilacao")));
        i.setMobilidadeReduzida("S".equals(f.get("mobilidadeReduzida")));
        i.setAreaRisco("S".equals(f.get("areaRisco")));
        i.setConsentimentoEm(vazio(f.get("consentimentoEm")) ? null : LocalDate.parse(f.get("consentimentoEm")));
        i.setConsentidoPor(f.get("consentidoPor"));
        return i;
    }

    private Voluntario voluntario(Map<String, String> f) {
        Voluntario v = new Voluntario();
        v.setId(idOpcional(f));
        v.setIdRegiao(Long.parseLong(f.get("idRegiao")));
        v.setNome(f.get("nome"));
        v.setTelefone(f.get("telefone"));
        v.setTipo(f.get("tipo"));
        return v;
    }

    private Vinculo vinculo(Map<String, String> f) {
        Vinculo v = new Vinculo();
        v.setIdIdoso(Long.parseLong(f.get("idIdoso")));
        v.setIdVoluntario(Long.parseLong(f.get("idVoluntario")));
        v.setPapel(f.get("papel"));
        return v;
    }

    private Alerta alerta(Map<String, String> f) {
        Alerta a = new Alerta();
        a.setId(idOpcional(f));
        a.setIdRegiao(Long.parseLong(f.get("idRegiao")));
        try {
            a.setTipo(TipoAlerta.valueOf(f.get("tipo")));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraNegocioException("Tipo de alerta deve ser CALOR ou CHUVA.");
        }
        a.setDataAlerta(vazio(f.get("dataAlerta")) ? null : LocalDate.parse(f.get("dataAlerta")));
        a.setNivel(f.get("nivel"));
        a.setTempMax(vazio(f.get("tempMax")) ? null : Double.valueOf(f.get("tempMax").replace(',', '.')));
        a.setOrigem(f.get("origem"));
        a.setFonteAviso(f.get("fonteAviso"));
        return a;
    }

    // ===== utilitarios =====

    private static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static long id(Map<String, String> m) {
        if (vazio(m.get("id"))) throw new RegraNegocioException("Informe o id.");
        return Long.parseLong(m.get("id"));
    }

    private static Long idOpcional(Map<String, String> m) {
        return vazio(m.get("id")) ? null : Long.valueOf(m.get("id"));
    }

    private static Map<String, Object> ok() {
        return Collections.singletonMap("ok", true);
    }

    private static String erro(String mensagem) {
        return "{\"erro\":" + Json.texto(mensagem == null ? "Erro" : mensagem) + "}";
    }

    private static Map<String, String> form(String raw) throws IOException {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String par : raw.split("&")) {
            int i = par.indexOf('=');
            if (i <= 0) continue;
            m.put(URLDecoder.decode(par.substring(0, i), "UTF-8"), URLDecoder.decode(par.substring(i + 1), "UTF-8"));
        }
        return m;
    }

    private static String lerCorpo(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    // arquivos estaticos da pasta web/ (bloqueia ../ para nao sair da pasta)
    private void arquivo(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (caminho.equals("/")) caminho = "/index.html";
        Path p = WEB.resolve(caminho.substring(1)).normalize();
        if (!p.startsWith(WEB) || !Files.isRegularFile(p)) {
            enviar(ex, 404, "text/plain; charset=utf-8", "N\u00e3o encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        enviar(ex, 200, tipo(p.toString()), Files.readAllBytes(p));
    }

    private static String tipo(String nome) {
        if (nome.endsWith(".html")) return "text/html; charset=utf-8";
        if (nome.endsWith(".css")) return "text/css; charset=utf-8";
        if (nome.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (nome.endsWith(".svg")) return "image/svg+xml";
        if (nome.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private static void enviar(HttpExchange ex, int status, String tipo, byte[] corpo) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(status, corpo.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(corpo);
        }
    }
}
