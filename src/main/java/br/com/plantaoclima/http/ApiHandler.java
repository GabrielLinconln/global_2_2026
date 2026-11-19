package br.com.plantaoclima.http;

import br.com.plantaoclima.exception.PersistenciaException;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.Alerta;
import br.com.plantaoclima.model.Checagem;
import br.com.plantaoclima.model.Idoso;
import br.com.plantaoclima.model.PapelVinculo;
import br.com.plantaoclima.model.Regiao;
import br.com.plantaoclima.model.RelatorioDtos.CoberturaAlertaDto;
import br.com.plantaoclima.model.RelatorioDtos.PainelResumoDto;
import br.com.plantaoclima.model.RelatorioDtos.RedeIncompletaDto;
import br.com.plantaoclima.model.RelatorioDtos.RelatorioAgregadoRegiaoDto;
import br.com.plantaoclima.model.RelatorioDtos.TaxaNaoAtendeuDto;
import br.com.plantaoclima.model.SeveridadeAlerta;
import br.com.plantaoclima.model.StatusAlerta;
import br.com.plantaoclima.model.StatusChecagem;
import br.com.plantaoclima.model.TipoAlerta;
import br.com.plantaoclima.model.VinculoCuidado;
import br.com.plantaoclima.model.Voluntario;
import br.com.plantaoclima.service.AlertaService;
import br.com.plantaoclima.service.ChecagemService;
import br.com.plantaoclima.service.IdosoService;
import br.com.plantaoclima.service.RegiaoService;
import br.com.plantaoclima.service.RelatorioService;
import br.com.plantaoclima.service.VinculoService;
import br.com.plantaoclima.service.VoluntarioService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler REST da camada HTTP que conecta a interface web diretamente aos servicos Java + JDBC.
 */
public class ApiHandler implements HttpHandler {

    private final RegiaoService regiaoService;
    private final IdosoService idosoService;
    private final VoluntarioService voluntarioService;
    private final VinculoService vinculoService;
    private final AlertaService alertaService;
    private final ChecagemService checagemService;
    private final RelatorioService relatorioService;

    public ApiHandler(RegiaoService regiaoService, IdosoService idosoService,
                      VoluntarioService voluntarioService, VinculoService vinculoService,
                      AlertaService alertaService, ChecagemService checagemService,
                      RelatorioService relatorioService) {
        this.regiaoService = regiaoService;
        this.idosoService = idosoService;
        this.voluntarioService = voluntarioService;
        this.vinculoService = vinculoService;
        this.alertaService = alertaService;
        this.checagemService = checagemService;
        this.relatorioService = relatorioService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        adicionarCabecalhosCors(exchange);
        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        try {
            rotear(exchange, method, path);
        } catch (ValidacaoException e) {
            enviarErro(exchange, 400, e.getMessage());
        } catch (RecursoNaoEncontradoException e) {
            enviarErro(exchange, 404, e.getMessage());
        } catch (PersistenciaException e) {
            enviarErro(exchange, e.isConflito() ? 409 : 500, e.getMessage());
        } catch (IllegalArgumentException e) {
            enviarErro(exchange, 400, "Valor invalido informado na requisicao: " + e.getMessage());
        } catch (Exception e) {
            enviarErro(exchange, 500, "Erro interno inesperado: " + e.getMessage());
        }
    }

    private void rotear(HttpExchange exchange, String method, String path) throws IOException {
        if ("/api/health".equals(path) && "GET".equals(method)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "ok");
            body.put("sistema", "Plantao Clima - CitizenTech");
            body.put("banco", "H2 (Modo Oracle via JDBC)");
            enviarJson(exchange, 200, body);
            return;
        }

        if ("/api/painel".equals(path) && "GET".equals(method)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("resumo", toMap(relatorioService.obterResumoPainel()));
            body.put("casosCriticos", checagemService.listarCasosCriticosAtivos().stream().map(this::toMap).toList());
            body.put("alertas", alertaService.listarTodos().stream().map(this::toMap).toList());
            body.put("mapaRegioes", relatorioService.listarRelatorioAgregadoPorRegiao().stream().map(this::toMap).toList());
            enviarJson(exchange, 200, body);
            return;
        }

        if ("/api/relatorios".equals(path) && "GET".equals(method)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("cobertura4h", relatorioService.listarCoberturaAte4Horas().stream().map(this::toMap).toList());
            body.put("redeIncompleta", relatorioService.listarIdososComRedeIncompleta().stream().map(this::toMap).toList());
            body.put("taxaNaoAtendeu", relatorioService.listarTaxaNaoAtendeuPorVoluntario().stream().map(this::toMap).toList());
            body.put("agregadoRegioes", relatorioService.listarRelatorioAgregadoPorRegiao().stream().map(this::toMap).toList());
            enviarJson(exchange, 200, body);
            return;
        }

        if (path.startsWith("/api/regioes")) {
            tratarRegioes(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/idosos")) {
            tratarIdosos(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/voluntarios")) {
            tratarVoluntarios(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/vinculos")) {
            tratarVinculos(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/alertas")) {
            tratarAlertas(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/checagens")) {
            tratarChecagens(exchange, method, path);
            return;
        }

        enviarErro(exchange, 404, "Endpoint nao encontrado: " + path);
    }

    private void tratarRegioes(HttpExchange exchange, String method, String path) throws IOException {
        Long id = extrairId(path, "/api/regioes");
        if (id == null) {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, regiaoService.listarTodas().stream().map(this::toMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Regiao nova = regiaoService.criar(lerRegiao(lerCorpoJson(exchange)));
                enviarJson(exchange, 201, toMap(nova));
                return;
            }
        } else {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, toMap(regiaoService.buscarPorId(id)));
                return;
            }
            if ("PUT".equals(method)) {
                Regiao atualizada = regiaoService.atualizar(id, lerRegiao(lerCorpoJson(exchange)));
                enviarJson(exchange, 200, toMap(atualizada));
                return;
            }
            if ("DELETE".equals(method)) {
                regiaoService.excluir(id);
                enviarJson(exchange, 200, Map.of("mensagem", "Regiao excluida com sucesso."));
                return;
            }
        }
        enviarErro(exchange, 405, "Metodo nao permitido.");
    }

    private void tratarIdosos(HttpExchange exchange, String method, String path) throws IOException {
        Long id = extrairId(path, "/api/idosos");
        if (id == null) {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, idosoService.listarTodos().stream().map(this::toMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Idoso novo = idosoService.criar(lerIdoso(lerCorpoJson(exchange)));
                enviarJson(exchange, 201, toMap(novo));
                return;
            }
        } else {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, toMap(idosoService.buscarPorId(id)));
                return;
            }
            if ("PUT".equals(method)) {
                Idoso atualizado = idosoService.atualizar(id, lerIdoso(lerCorpoJson(exchange)));
                enviarJson(exchange, 200, toMap(atualizado));
                return;
            }
            if ("DELETE".equals(method)) {
                idosoService.excluir(id);
                enviarJson(exchange, 200, Map.of("mensagem", "Idoso excluido com sucesso."));
                return;
            }
        }
        enviarErro(exchange, 405, "Metodo nao permitido.");
    }

    private void tratarVoluntarios(HttpExchange exchange, String method, String path) throws IOException {
        Long id = extrairId(path, "/api/voluntarios");
        if (id == null) {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, voluntarioService.listarTodos().stream().map(this::toMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Voluntario novo = voluntarioService.criar(lerVoluntario(lerCorpoJson(exchange)));
                enviarJson(exchange, 201, toMap(novo));
                return;
            }
        } else {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, toMap(voluntarioService.buscarPorId(id)));
                return;
            }
            if ("PUT".equals(method)) {
                Voluntario atualizado = voluntarioService.atualizar(id, lerVoluntario(lerCorpoJson(exchange)));
                enviarJson(exchange, 200, toMap(atualizado));
                return;
            }
            if ("DELETE".equals(method)) {
                voluntarioService.excluir(id);
                enviarJson(exchange, 200, Map.of("mensagem", "Voluntario excluido com sucesso."));
                return;
            }
        }
        enviarErro(exchange, 405, "Metodo nao permitido.");
    }

    private void tratarVinculos(HttpExchange exchange, String method, String path) throws IOException {
        Long id = extrairId(path, "/api/vinculos");
        if (id == null) {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, vinculoService.listarTodos().stream().map(this::toMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                VinculoCuidado novo = vinculoService.criar(lerVinculo(lerCorpoJson(exchange)));
                enviarJson(exchange, 201, toMap(novo));
                return;
            }
        } else {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, toMap(vinculoService.buscarPorId(id)));
                return;
            }
            if ("PUT".equals(method)) {
                VinculoCuidado atualizado = vinculoService.atualizar(id, lerVinculo(lerCorpoJson(exchange)));
                enviarJson(exchange, 200, toMap(atualizado));
                return;
            }
            if ("DELETE".equals(method)) {
                vinculoService.excluir(id);
                enviarJson(exchange, 200, Map.of("mensagem", "Vinculo removido com sucesso."));
                return;
            }
        }
        enviarErro(exchange, 405, "Metodo nao permitido.");
    }

    private void tratarAlertas(HttpExchange exchange, String method, String path) throws IOException {
        String[] partes = path.split("/");
        // /api/alertas -> len 3
        // /api/alertas/{id} -> len 4
        // /api/alertas/{id}/rodada ou /api/alertas/{id}/encerrar -> len 5
        if (partes.length == 3) {
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, alertaService.listarTodos().stream().map(this::toMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Alerta criado = alertaService.criarEGerarRodada(lerAlerta(lerCorpoJson(exchange)));
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("alerta", toMap(criado));
                resp.put("rodada", checagemService.listarRodadaPorAlerta(criado.getId()).stream().map(this::toMap).toList());
                enviarJson(exchange, 201, resp);
                return;
            }
        } else if (partes.length == 4) {
            Long id = parseId(partes[3]);
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, toMap(alertaService.buscarPorId(id)));
                return;
            }
            if ("PUT".equals(method)) {
                Alerta atualizado = alertaService.atualizar(id, lerAlerta(lerCorpoJson(exchange)));
                enviarJson(exchange, 200, toMap(atualizado));
                return;
            }
            if ("DELETE".equals(method)) {
                alertaService.excluir(id);
                enviarJson(exchange, 200, Map.of("mensagem", "Alerta excluido com sucesso."));
                return;
            }
        } else if (partes.length == 5) {
            Long id = parseId(partes[3]);
            String acao = partes[4];
            if ("rodada".equals(acao) && "GET".equals(method)) {
                List<Map<String, Object>> rodada = checagemService.listarRodadaPorAlerta(id)
                        .stream().map(this::toMap).toList();
                enviarJson(exchange, 200, rodada);
                return;
            }
            if ("encerrar".equals(acao) && "POST".equals(method)) {
                enviarJson(exchange, 200, toMap(alertaService.encerrar(id)));
                return;
            }
        }
        enviarErro(exchange, 405, "Operacao nao suportada em /api/alertas.");
    }

    private void tratarChecagens(HttpExchange exchange, String method, String path) throws IOException {
        String[] partes = path.split("/");
        if (partes.length == 3 && "GET".equals(method)) {
            enviarJson(exchange, 200, checagemService.listarTodas().stream().map(this::toMap).toList());
            return;
        }
        if (partes.length == 4) {
            Long id = parseId(partes[3]);
            if ("GET".equals(method)) {
                enviarJson(exchange, 200, toMap(checagemService.buscarPorId(id)));
                return;
            }
            if ("DELETE".equals(method)) {
                checagemService.excluir(id);
                enviarJson(exchange, 200, Map.of("mensagem", "Checagem excluida com sucesso."));
                return;
            }
        }
        if (partes.length == 5 && "registrar".equals(partes[4]) && ("POST".equals(method) || "PUT".equals(method))) {
            Long id = parseId(partes[3]);
            Map<String, Object> body = lerCorpoJson(exchange);
            String statusStr = JsonUtil.getString(body, "status");
            if (statusStr == null || statusStr.isBlank()) {
                throw new ValidacaoException("Informe o status da ligacao.");
            }
            StatusChecagem status = StatusChecagem.valueOf(statusStr.trim().toUpperCase());
            String checklist = JsonUtil.getString(body, "checklistResumo");
            String obs = JsonUtil.getString(body, "observacao");

            ChecagemService.ResultadoRegistro resultado =
                    checagemService.registrarResultado(id, status, checklist, obs);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("checagem", toMap(resultado.checagemAtual()));
            resp.put("escalonado", resultado.escalonado());
            resp.put("mensagem", resultado.mensagem());
            enviarJson(exchange, 200, resp);
            return;
        }
        enviarErro(exchange, 405, "Operacao nao suportada em /api/checagens.");
    }

    // --- Conversores de entrada ---

    private Regiao lerRegiao(Map<String, Object> body) {
        Regiao r = new Regiao();
        r.setNome(JsonUtil.getString(body, "nome"));
        r.setBairroZona(JsonUtil.getString(body, "bairroZona"));
        r.setRiscoCalor(JsonUtil.getString(body, "riscoCalor"));
        r.setRiscoChuva(JsonUtil.getString(body, "riscoChuva"));
        r.setReferenciaUbs(JsonUtil.getString(body, "referenciaUbs"));
        r.setAtivo(JsonUtil.getBoolean(body, "ativo", true));
        return r;
    }

    private Idoso lerIdoso(Map<String, Object> body) {
        Idoso i = new Idoso();
        i.setNome(JsonUtil.getString(body, "nome"));
        i.setTelefone(JsonUtil.getString(body, "telefone"));
        i.setAnoNascimento(JsonUtil.getInt(body, "anoNascimento", 0));
        i.setEnderecoResumo(JsonUtil.getString(body, "enderecoResumo"));
        i.setDoencaCronica(JsonUtil.getBoolean(body, "doencaCronica", false));
        i.setMobilidadeReduzida(JsonUtil.getBoolean(body, "mobilidadeReduzida", false));
        i.setCasaSemVentilacao(JsonUtil.getBoolean(body, "casaSemVentilacao", false));
        i.setAreaRisco(JsonUtil.getBoolean(body, "areaRisco", false));
        i.setConsentimentoLgpd(JsonUtil.getBoolean(body, "consentimentoLgpd", false));
        i.setObservacoes(JsonUtil.getString(body, "observacoes"));
        i.setIdRegiao(JsonUtil.getLong(body, "idRegiao"));
        i.setAtivo(JsonUtil.getBoolean(body, "ativo", true));
        return i;
    }

    private Voluntario lerVoluntario(Map<String, Object> body) {
        Voluntario v = new Voluntario();
        v.setNome(JsonUtil.getString(body, "nome"));
        v.setTelefone(JsonUtil.getString(body, "telefone"));
        v.setRelacaoComunidade(JsonUtil.getString(body, "relacaoComunidade"));
        v.setDisponibilidade(JsonUtil.getString(body, "disponibilidade"));
        v.setIdRegiao(JsonUtil.getLong(body, "idRegiao"));
        v.setAtivo(JsonUtil.getBoolean(body, "ativo", true));
        return v;
    }

    private VinculoCuidado lerVinculo(Map<String, Object> body) {
        VinculoCuidado vc = new VinculoCuidado();
        vc.setIdIdoso(JsonUtil.getLong(body, "idIdoso"));
        vc.setIdVoluntario(JsonUtil.getLong(body, "idVoluntario"));
        String papel = JsonUtil.getString(body, "papel");
        if (papel != null && !papel.isBlank()) {
            vc.setPapel(PapelVinculo.valueOf(papel.trim().toUpperCase()));
        }
        return vc;
    }

    private Alerta lerAlerta(Map<String, Object> body) {
        Alerta a = new Alerta();
        a.setTitulo(JsonUtil.getString(body, "titulo"));
        String tipo = JsonUtil.getString(body, "tipo");
        if (tipo != null && !tipo.isBlank()) {
            a.setTipo(TipoAlerta.valueOf(tipo.trim().toUpperCase()));
        }
        String sev = JsonUtil.getString(body, "severidade");
        if (sev != null && !sev.isBlank()) {
            a.setSeveridade(SeveridadeAlerta.valueOf(sev.trim().toUpperCase()));
        }
        a.setFonte(JsonUtil.getString(body, "fonte"));
        a.setDescricao(JsonUtil.getString(body, "descricao"));
        String status = JsonUtil.getString(body, "status");
        if (status != null && !status.isBlank()) {
            a.setStatus(StatusAlerta.valueOf(status.trim().toUpperCase()));
        }
        a.setIdRegiao(JsonUtil.getLong(body, "idRegiao"));
        return a;
    }

    // --- Serializadores de saida ---

    private Map<String, Object> toMap(Regiao r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("nome", r.getNome());
        m.put("bairroZona", r.getBairroZona());
        m.put("riscoCalor", r.getRiscoCalor());
        m.put("riscoChuva", r.getRiscoChuva());
        m.put("referenciaUbs", r.getReferenciaUbs());
        m.put("ativo", r.isAtivo());
        return m;
    }

    private Map<String, Object> toMap(Idoso i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("tipoPessoa", i.getTipoPessoa());
        m.put("nome", i.getNome());
        m.put("telefone", i.getTelefone());
        m.put("anoNascimento", i.getAnoNascimento());
        m.put("idadeEstimada", i.getIdadeEstimada());
        m.put("enderecoResumo", i.getEnderecoResumo());
        m.put("doencaCronica", i.isDoencaCronica());
        m.put("mobilidadeReduzida", i.isMobilidadeReduzida());
        m.put("casaSemVentilacao", i.isCasaSemVentilacao());
        m.put("areaRisco", i.isAreaRisco());
        m.put("consentimentoLgpd", i.isConsentimentoLgpd());
        m.put("dataConsentimento", i.getDataConsentimento());
        m.put("observacoes", i.getObservacoes());
        m.put("idRegiao", i.getIdRegiao());
        m.put("nomeRegiao", i.getNomeRegiao());
        m.put("ativo", i.isAtivo());
        m.put("pontuacaoCalor", i.calcularPontuacaoRisco(TipoAlerta.CALOR));
        m.put("pontuacaoChuva", i.calcularPontuacaoRisco(TipoAlerta.CHUVA));
        m.put("nivelCalor", i.classificarNivelRisco(TipoAlerta.CALOR));
        m.put("nivelChuva", i.classificarNivelRisco(TipoAlerta.CHUVA));
        return m;
    }

    private Map<String, Object> toMap(Voluntario v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("tipoPessoa", v.getTipoPessoa());
        m.put("nome", v.getNome());
        m.put("telefone", v.getTelefone());
        m.put("relacaoComunidade", v.getRelacaoComunidade());
        m.put("disponibilidade", v.getDisponibilidade());
        m.put("idRegiao", v.getIdRegiao());
        m.put("nomeRegiao", v.getNomeRegiao());
        m.put("ativo", v.isAtivo());
        return m;
    }

    private Map<String, Object> toMap(VinculoCuidado vc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", vc.getId());
        m.put("idIdoso", vc.getIdIdoso());
        m.put("idVoluntario", vc.getIdVoluntario());
        m.put("papel", vc.getPapel());
        m.put("prioridade", vc.getPrioridade());
        m.put("nomeIdoso", vc.getNomeIdoso());
        m.put("telefoneIdoso", vc.getTelefoneIdoso());
        m.put("nomeVoluntario", vc.getNomeVoluntario());
        m.put("telefoneVoluntario", vc.getTelefoneVoluntario());
        m.put("relacaoVoluntario", vc.getRelacaoVoluntario());
        return m;
    }

    private Map<String, Object> toMap(Alerta a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("titulo", a.getTitulo());
        m.put("tipo", a.getTipo());
        m.put("severidade", a.getSeveridade());
        m.put("fonte", a.getFonte());
        m.put("descricao", a.getDescricao());
        m.put("status", a.getStatus());
        m.put("idRegiao", a.getIdRegiao());
        m.put("nomeRegiao", a.getNomeRegiao());
        m.put("emitidoEm", a.getEmitidoEm());
        m.put("encerradoEm", a.getEncerradoEm());
        return m;
    }

    private Map<String, Object> toMap(Checagem c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("idAlerta", c.getIdAlerta());
        m.put("tituloAlerta", c.getTituloAlerta());
        m.put("tipoAlerta", c.getTipoAlerta());
        m.put("idIdoso", c.getIdIdoso());
        m.put("nomeIdoso", c.getNomeIdoso());
        m.put("telefoneIdoso", c.getTelefoneIdoso());
        m.put("anoNascimentoIdoso", c.getAnoNascimentoIdoso());
        m.put("enderecoIdoso", c.getEnderecoIdoso());
        m.put("nomeRegiao", c.getNomeRegiao());
        m.put("doencaCronica", c.isDoencaCronica());
        m.put("mobilidadeReduzida", c.isMobilidadeReduzida());
        m.put("casaSemVentilacao", c.isCasaSemVentilacao());
        m.put("areaRisco", c.isAreaRisco());
        m.put("idVoluntario", c.getIdVoluntario());
        m.put("nomeVoluntario", c.getNomeVoluntario());
        m.put("telefoneVoluntario", c.getTelefoneVoluntario());
        m.put("papelAtual", c.getPapelAtual());
        m.put("pontuacaoRisco", c.getPontuacaoRisco());
        m.put("status", c.getStatus());
        m.put("tentativas", c.getTentativas());
        m.put("checklistResumo", c.getChecklistResumo());
        m.put("observacao", c.getObservacao());
        m.put("criadoEm", c.getCriadoEm());
        m.put("atualizadoEm", c.getAtualizadoEm());
        return m;
    }

    private Map<String, Object> toMap(PainelResumoDto r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalIdosos", r.totalIdosos());
        m.put("totalVoluntarios", r.totalVoluntarios());
        m.put("alertasAtivos", r.alertasAtivos());
        m.put("casosCriticos", r.casosCriticos());
        m.put("precisamAjuda", r.precisamAjuda());
        m.put("precisamAtencao", r.precisamAtencao());
        m.put("pendentes", r.pendentes());
        m.put("checadosOk", r.checadosOk());
        m.put("idososRedeIncompleta", r.idososRedeIncompleta());
        m.put("percentualChecados4h", r.percentualChecados4h());
        return m;
    }

    private Map<String, Object> toMap(CoberturaAlertaDto d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idAlerta", d.idAlerta());
        m.put("titulo", d.titulo());
        m.put("tipo", d.tipo());
        m.put("status", d.status());
        m.put("totalIdososRodada", d.totalIdososRodada());
        m.put("checadosAte4h", d.checadosAte4h());
        m.put("totalChecados", d.totalChecados());
        m.put("criticos", d.criticos());
        m.put("percentualAte4h", d.percentualAte4h());
        return m;
    }

    private Map<String, Object> toMap(RedeIncompletaDto d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idIdoso", d.idIdoso());
        m.put("nomeIdoso", d.nomeIdoso());
        m.put("nomeRegiao", d.nomeRegiao());
        m.put("anoNascimento", d.anoNascimento());
        m.put("qtdTitular", d.qtdTitular());
        m.put("qtdReserva", d.qtdReserva());
        m.put("diagnostico", d.diagnostico());
        return m;
    }

    private Map<String, Object> toMap(TaxaNaoAtendeuDto d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idVoluntario", d.idVoluntario());
        m.put("nomeVoluntario", d.nomeVoluntario());
        m.put("relacaoComunidade", d.relacaoComunidade());
        m.put("nomeRegiao", d.nomeRegiao());
        m.put("totalLigacoes", d.totalLigacoes());
        m.put("totalNaoAtendeu", d.totalNaoAtendeu());
        m.put("taxaNaoAtendeuPct", d.taxaNaoAtendeuPct());
        return m;
    }

    private Map<String, Object> toMap(RelatorioAgregadoRegiaoDto d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idRegiao", d.idRegiao());
        m.put("nomeRegiao", d.nomeRegiao());
        m.put("bairroZona", d.bairroZona());
        m.put("riscoCalor", d.riscoCalor());
        m.put("riscoChuva", d.riscoChuva());
        m.put("referenciaUbs", d.referenciaUbs());
        m.put("totalIdosos", d.totalIdosos());
        m.put("comDoencaCronica", d.comDoencaCronica());
        m.put("comMobilidadeReduzida", d.comMobilidadeReduzida());
        m.put("semVentilacao", d.semVentilacao());
        m.put("emAreaRisco", d.emAreaRisco());
        m.put("alertasAtivos", d.alertasAtivos());
        m.put("pendentesAtivos", d.pendentesAtivos());
        m.put("casosCriticosAtivos", d.casosCriticosAtivos());
        m.put("casosAjudaAtivos", d.casosAjudaAtivos());
        return m;
    }

    // --- Helpers HTTP ---

    private Long extrairId(String path, String prefix) {
        if (path.equals(prefix)) {
            return null;
        }
        String resto = path.substring(prefix.length());
        if (!resto.startsWith("/")) {
            return null;
        }
        String seg = resto.substring(1);
        if (seg.contains("/")) {
            return null;
        }
        return parseId(seg);
    }

    private Long parseId(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("ID invalido na URL: " + str);
        }
    }

    private Map<String, Object> lerCorpoJson(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String texto = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtil.parseObject(texto);
        }
    }

    private void adicionarCabecalhosCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void enviarJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = JsonUtil.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void enviarErro(HttpExchange exchange, int status, String mensagem) throws IOException {
        Map<String, Object> erro = new LinkedHashMap<>();
        erro.put("erro", true);
        erro.put("status", status);
        erro.put("mensagem", mensagem);
        enviarJson(exchange, status, erro);
    }
}
