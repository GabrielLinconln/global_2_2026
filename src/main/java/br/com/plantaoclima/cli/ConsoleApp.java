package br.com.plantaoclima.cli;

import br.com.plantaoclima.model.Alerta;
import br.com.plantaoclima.model.Checagem;
import br.com.plantaoclima.model.Idoso;
import br.com.plantaoclima.model.RelatorioDtos.CoberturaAlertaDto;
import br.com.plantaoclima.model.RelatorioDtos.RedeIncompletaDto;
import br.com.plantaoclima.model.RelatorioDtos.TaxaNaoAtendeuDto;
import br.com.plantaoclima.model.TipoAlerta;
import br.com.plantaoclima.model.Voluntario;
import br.com.plantaoclima.service.AlertaService;
import br.com.plantaoclima.service.ChecagemService;
import br.com.plantaoclima.service.IdosoService;
import br.com.plantaoclima.service.RelatorioService;
import br.com.plantaoclima.service.VoluntarioService;

import java.util.List;
import java.util.Scanner;

/**
 * Interface de linha de comando (CLI) opcional para inspecao rapida via terminal.
 */
public class ConsoleApp {

    private final IdosoService idosoService;
    private final VoluntarioService voluntarioService;
    private final AlertaService alertaService;
    private final ChecagemService checagemService;
    private final RelatorioService relatorioService;

    public ConsoleApp(IdosoService idosoService, VoluntarioService voluntarioService,
                      AlertaService alertaService, ChecagemService checagemService,
                      RelatorioService relatorioService) {
        this.idosoService = idosoService;
        this.voluntarioService = voluntarioService;
        this.alertaService = alertaService;
        this.checagemService = checagemService;
        this.relatorioService = relatorioService;
    }

    public void executar() {
        try (Scanner sc = new Scanner(System.in)) {
            boolean rodando = true;
            while (rodando) {
                System.out.println("\n========== PLANTAO CLIMA (MODO CONSOLE) ==========");
                System.out.println("1 - Listar idosos e pontuacao de risco");
                System.out.println("2 - Listar voluntarios");
                System.out.println("3 - Listar alertas e casos criticos ativos");
                System.out.println("4 - Relatorios SQL (4h, Rede Incompleta, Nao Atendeu)");
                System.out.println("0 - Sair");
                System.out.print("Escolha uma opcao: ");
                String op = sc.nextLine().trim();

                try {
                    switch (op) {
                        case "1" -> listarIdosos();
                        case "2" -> listarVoluntarios();
                        case "3" -> listarAlertasECriticos();
                        case "4" -> exibirRelatorios();
                        case "0" -> rodando = false;
                        default -> System.out.println("Opcao invalida.");
                    }
                } catch (Exception e) {
                    System.out.println("Erro tratado: " + e.getMessage());
                }
            }
        }
    }

    private void listarIdosos() {
        List<Idoso> idosos = idosoService.listarTodos();
        System.out.println("\n--- IDOSOS PROTEGIDOS (" + idosos.size() + ") ---");
        for (Idoso i : idosos) {
            System.out.printf("#%d | %s | Nasc: %d | Regiao: %s | Risco Calor: %d | Risco Chuva: %d%n",
                    i.getId(), i.getNome(), i.getAnoNascimento(), i.getNomeRegiao(),
                    i.calcularPontuacaoRisco(TipoAlerta.CALOR),
                    i.calcularPontuacaoRisco(TipoAlerta.CHUVA));
        }
    }

    private void listarVoluntarios() {
        List<Voluntario> vols = voluntarioService.listarTodos();
        System.out.println("\n--- VOLUNTARIOS (" + vols.size() + ") ---");
        for (Voluntario v : vols) {
            System.out.printf("#%d | %s | %s | %s | Regiao: %s%n",
                    v.getId(), v.getNome(), v.getTelefone(), v.getRelacaoComunidade(), v.getNomeRegiao());
        }
    }

    private void listarAlertasECriticos() {
        List<Alerta> alertas = alertaService.listarTodos();
        System.out.println("\n--- ALERTAS (" + alertas.size() + ") ---");
        for (Alerta a : alertas) {
            System.out.printf("#%d | [%s/%s] %s | Status: %s | Regiao: %s%n",
                    a.getId(), a.getTipo(), a.getSeveridade(), a.getTitulo(), a.getStatus(), a.getNomeRegiao());
        }
        List<Checagem> criticos = checagemService.listarCasosCriticosAtivos();
        System.out.println("\n--- CASOS CRITICOS ATIVOS (" + criticos.size() + ") ---");
        for (Checagem c : criticos) {
            System.out.printf("CRITICO: %s (%s) | Endereco: %s | Risco: %d | %s%n",
                    c.getNomeIdoso(), c.getTelefoneIdoso(), c.getEnderecoIdoso(),
                    c.getPontuacaoRisco(), c.getObservacao());
        }
    }

    private void exibirRelatorios() {
        System.out.println("\n--- RELATORIO 1: COBERTURA EM ATE 4 HORAS ---");
        for (CoberturaAlertaDto c : relatorioService.listarCoberturaAte4Horas()) {
            System.out.printf("Alerta #%d (%s): %d/%d checados em 4h (%.1f%%) | Criticos: %d%n",
                    c.idAlerta(), c.titulo(), c.checadosAte4h(), c.totalIdososRodada(),
                    c.percentualAte4h(), c.criticos());
        }
        System.out.println("\n--- RELATORIO 2: IDOSOS COM REDE INCOMPLETA ---");
        for (RedeIncompletaDto r : relatorioService.listarIdososComRedeIncompleta()) {
            System.out.printf("Idoso #%d (%s - %s): %s%n",
                    r.idIdoso(), r.nomeIdoso(), r.nomeRegiao(), r.diagnostico());
        }
        System.out.println("\n--- RELATORIO 3: TAXA DE NAO ATENDEU POR VOLUNTARIO ---");
        for (TaxaNaoAtendeuDto t : relatorioService.listarTaxaNaoAtendeuPorVoluntario()) {
            System.out.printf("%s (%s): %d/%d nao atendeu (%.1f%%)%n",
                    t.nomeVoluntario(), t.relacaoComunidade(), t.totalNaoAtendeu(),
                    t.totalLigacoes(), t.taxaNaoAtendeuPct());
        }
    }
}
