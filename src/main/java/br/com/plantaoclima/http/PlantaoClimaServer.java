package br.com.plantaoclima.http;

import br.com.plantaoclima.service.AlertaService;
import br.com.plantaoclima.service.ChecagemService;
import br.com.plantaoclima.service.IdosoService;
import br.com.plantaoclima.service.RegiaoService;
import br.com.plantaoclima.service.RelatorioService;
import br.com.plantaoclima.service.VinculoService;
import br.com.plantaoclima.service.VoluntarioService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embutido (JDK com.sun.net.httpserver) que expoe a API REST e a interface Web.
 */
public class PlantaoClimaServer {

    private final HttpServer server;
    private final int porta;

    public PlantaoClimaServer(int porta, Path webRoot,
                              RegiaoService regiaoService, IdosoService idosoService,
                              VoluntarioService voluntarioService, VinculoService vinculoService,
                              AlertaService alertaService, ChecagemService checagemService,
                              RelatorioService relatorioService) throws IOException {
        this.porta = porta;
        this.server = HttpServer.create(new InetSocketAddress(porta), 0);
        this.server.createContext("/api", new ApiHandler(
                regiaoService, idosoService, voluntarioService,
                vinculoService, alertaService, checagemService, relatorioService));
        this.server.createContext("/", new StaticFileHandler(webRoot));
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void iniciar() {
        server.start();
    }

    public void parar() {
        server.stop(0);
    }

    public int getPorta() {
        return porta;
    }
}
