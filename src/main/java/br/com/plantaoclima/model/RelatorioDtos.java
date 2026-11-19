package br.com.plantaoclima.model;

/**
 * DTOs imutaveis (Java 17 records) para os relatorios SQL agregados.
 */
public final class RelatorioDtos {

    private RelatorioDtos() {
    }

    public record CoberturaAlertaDto(
            Long idAlerta,
            String titulo,
            String tipo,
            String status,
            int totalIdososRodada,
            int checadosAte4h,
            int totalChecados,
            int criticos,
            double percentualAte4h
    ) {
    }

    public record RedeIncompletaDto(
            Long idIdoso,
            String nomeIdoso,
            String nomeRegiao,
            int anoNascimento,
            int qtdTitular,
            int qtdReserva,
            String diagnostico
    ) {
    }

    public record TaxaNaoAtendeuDto(
            Long idVoluntario,
            String nomeVoluntario,
            String relacaoComunidade,
            String nomeRegiao,
            int totalLigacoes,
            int totalNaoAtendeu,
            double taxaNaoAtendeuPct
    ) {
    }

    /**
     * Relatorio agregado e anonimizado por regiao para UBS e Defesa Civil (sem nomes pessoais).
     */
    public record RelatorioAgregadoRegiaoDto(
            Long idRegiao,
            String nomeRegiao,
            String bairroZona,
            String riscoCalor,
            String riscoChuva,
            String referenciaUbs,
            int totalIdosos,
            int comDoencaCronica,
            int comMobilidadeReduzida,
            int semVentilacao,
            int emAreaRisco,
            int alertasAtivos,
            int pendentesAtivos,
            int casosCriticosAtivos,
            int casosAjudaAtivos
    ) {
    }

    public record PainelResumoDto(
            int totalIdosos,
            int totalVoluntarios,
            int alertasAtivos,
            int casosCriticos,
            int precisamAjuda,
            int precisamAtencao,
            int pendentes,
            int checadosOk,
            int idososRedeIncompleta,
            double percentualChecados4h
    ) {
    }
}
