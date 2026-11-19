package br.com.plantaoclima;

import br.com.plantaoclima.cli.ConsoleApp;
import br.com.plantaoclima.dao.AlertaDao;
import br.com.plantaoclima.dao.ChecagemDao;
import br.com.plantaoclima.dao.DatabaseConfig;
import br.com.plantaoclima.dao.IdosoDao;
import br.com.plantaoclima.dao.RegiaoDao;
import br.com.plantaoclima.dao.RelatorioDao;
import br.com.plantaoclima.dao.VinculoCuidadoDao;
import br.com.plantaoclima.dao.VoluntarioDao;
import br.com.plantaoclima.http.PlantaoClimaServer;
import br.com.plantaoclima.service.AlertaService;
import br.com.plantaoclima.service.ChecagemService;
import br.com.plantaoclima.service.IdosoService;
import br.com.plantaoclima.service.RegiaoService;
import br.com.plantaoclima.service.RelatorioService;
import br.com.plantaoclima.service.VinculoService;
import br.com.plantaoclima.service.VoluntarioService;

import java.nio.file.Path;

/**
 * Ponto de entrada do sistema Plantao Clima (CitizenTech - FIAP GS 2026/2).
 * Inicializa o banco H2 (modo Oracle), monta as camadas DAO/Service e sobe o servidor HTTP.
 */
public class Main {

    public static void main(String[] args) {
        int porta = Integer.parseInt(System.getProperty("plantaoclima.port", "8080"));
        boolean modoCli = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg)) {
                modoCli = true;
            }
        }

        try {
            Path sqlPath = Path.of("sql", "plantao_clima.sql");
            Path webRoot = Path.of("web");

            DatabaseConfig.inicializarBanco(sqlPath);

            // Camada DAO (JDBC)
            RegiaoDao regiaoDao = new RegiaoDao();
            IdosoDao idosoDao = new IdosoDao();
            VoluntarioDao voluntarioDao = new VoluntarioDao();
            VinculoCuidadoDao vinculoDao = new VinculoCuidadoDao();
            AlertaDao alertaDao = new AlertaDao();
            ChecagemDao checagemDao = new ChecagemDao();
            RelatorioDao relatorioDao = new RelatorioDao();

            // Camada Service (Regras de Negocio)
            RegiaoService regiaoService = new RegiaoService(regiaoDao);
            IdosoService idosoService = new IdosoService(idosoDao, regiaoDao);
            VoluntarioService voluntarioService = new VoluntarioService(voluntarioDao, regiaoDao);
            VinculoService vinculoService = new VinculoService(vinculoDao, idosoDao, voluntarioDao);
            AlertaService alertaService = new AlertaService(alertaDao, regiaoDao, idosoDao, vinculoDao, checagemDao);
            ChecagemService checagemService = new ChecagemService(checagemDao, vinculoDao);
            RelatorioService relatorioService = new RelatorioService(relatorioDao);

            if (modoCli) {
                new ConsoleApp(idosoService, voluntarioService, alertaService, checagemService, relatorioService)
                        .executar();
                return;
            }

            // Camada HTTP (API REST + Interface Web)
            PlantaoClimaServer server = new PlantaoClimaServer(
                    porta, webRoot, regiaoService, idosoService, voluntarioService,
                    vinculoService, alertaService, checagemService, relatorioService);
            server.iniciar();

            System.out.println("=================================================================");
            System.out.println("  PLANTAO CLIMA - CitizenTech (Java 17 + JDBC + H2 Modo Oracle)  ");
            System.out.println("=================================================================");
            System.out.println("  Interface Web : http://localhost:" + porta);
            System.out.println("  API REST      : http://localhost:" + porta + "/api/health");
            System.out.println("  Banco H2      : " + DatabaseConfig.getJdbcUrl());
            System.out.println("  Pressione CTRL+C para encerrar o servidor.");
            System.out.println("=================================================================");
        } catch (Exception e) {
            System.err.println("Erro fatal ao iniciar Plantao Clima: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
