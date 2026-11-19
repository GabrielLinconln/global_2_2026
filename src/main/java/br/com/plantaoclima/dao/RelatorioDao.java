package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.RelatorioDtos.CoberturaAlertaDto;
import br.com.plantaoclima.model.RelatorioDtos.PainelResumoDto;
import br.com.plantaoclima.model.RelatorioDtos.RedeIncompletaDto;
import br.com.plantaoclima.model.RelatorioDtos.RelatorioAgregadoRegiaoDto;
import br.com.plantaoclima.model.RelatorioDtos.TaxaNaoAtendeuDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsavel pelas consultas SQL analiticas com JOIN, GROUP BY e agregacoes.
 */
public class RelatorioDao {

    /**
     * Relatorio 1: Percentual de idosos checados em ate 4 horas por alerta.
     */
    public List<CoberturaAlertaDto> listarCoberturaAte4Horas() {
        String sql = """
                SELECT a.ID_ALERTA,
                       a.TITULO,
                       a.TIPO,
                       a.STATUS,
                       COUNT(DISTINCT c.ID_IDOSO) AS TOTAL_IDOSOS,
                       COUNT(DISTINCT CASE
                           WHEN c.STATUS IN ('ESTA_BEM', 'PRECISA_ATENCAO', 'PRECISA_AJUDA')
                            AND c.ATUALIZADO_EM IS NOT NULL
                            AND c.ATUALIZADO_EM <= a.EMITIDO_EM + INTERVAL '4' HOUR
                           THEN c.ID_IDOSO
                       END) AS CHECADOS_4H,
                       COUNT(DISTINCT CASE
                           WHEN c.STATUS IN ('ESTA_BEM', 'PRECISA_ATENCAO', 'PRECISA_AJUDA')
                           THEN c.ID_IDOSO
                       END) AS TOTAL_CHECADOS,
                       COUNT(DISTINCT CASE
                           WHEN c.STATUS = 'CRITICO'
                            AND c.ID_CHECAGEM = (
                                SELECT MAX(c2.ID_CHECAGEM)
                                  FROM CHECAGEM c2
                                 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO
                            )
                           THEN c.ID_IDOSO
                       END) AS CRITICOS
                  FROM ALERTA a
             LEFT JOIN CHECAGEM c ON c.ID_ALERTA = a.ID_ALERTA
              GROUP BY a.ID_ALERTA, a.TITULO, a.TIPO, a.STATUS, a.EMITIDO_EM
              ORDER BY a.EMITIDO_EM DESC
                """;
        List<CoberturaAlertaDto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int total = rs.getInt("TOTAL_IDOSOS");
                int ate4h = rs.getInt("CHECADOS_4H");
                double pct = total > 0 ? Math.round((ate4h * 1000.0) / total) / 10.0 : 0.0;
                lista.add(new CoberturaAlertaDto(
                        rs.getLong("ID_ALERTA"),
                        rs.getString("TITULO"),
                        rs.getString("TIPO"),
                        rs.getString("STATUS"),
                        total,
                        ate4h,
                        rs.getInt("TOTAL_CHECADOS"),
                        rs.getInt("CRITICOS"),
                        pct
                ));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("gerar relatorio de cobertura em 4h", e);
        }
    }

    /**
     * Relatorio 2: Idosos ativos com rede de cuidado incompleta (sem titular e/ou sem reserva).
     */
    public List<RedeIncompletaDto> listarIdososComRedeIncompleta() {
        String sql = """
                SELECT i.ID_IDOSO,
                       i.NOME AS NOME_IDOSO,
                       r.NOME AS NOME_REGIAO,
                       i.ANO_NASCIMENTO,
                       SUM(CASE WHEN vc.PAPEL = 'TITULAR' THEN 1 ELSE 0 END) AS QTD_TITULAR,
                       SUM(CASE WHEN vc.PAPEL = 'RESERVA' THEN 1 ELSE 0 END) AS QTD_RESERVA
                  FROM IDOSO i
                  JOIN REGIAO r ON r.ID_REGIAO = i.ID_REGIAO
             LEFT JOIN VINCULO_CUIDADO vc ON vc.ID_IDOSO = i.ID_IDOSO
                 WHERE i.ATIVO = 1
              GROUP BY i.ID_IDOSO, i.NOME, r.NOME, i.ANO_NASCIMENTO
                HAVING SUM(CASE WHEN vc.PAPEL = 'TITULAR' THEN 1 ELSE 0 END) = 0
                    OR SUM(CASE WHEN vc.PAPEL = 'RESERVA' THEN 1 ELSE 0 END) = 0
              ORDER BY QTD_TITULAR ASC, QTD_RESERVA ASC, i.NOME
                """;
        List<RedeIncompletaDto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int titular = rs.getInt("QTD_TITULAR");
                int reserva = rs.getInt("QTD_RESERVA");
                String diag;
                if (titular == 0 && reserva == 0) {
                    diag = "Sem titular e sem reserva";
                } else if (titular == 0) {
                    diag = "Sem voluntario titular";
                } else {
                    diag = "Sem voluntario reserva";
                }
                lista.add(new RedeIncompletaDto(
                        rs.getLong("ID_IDOSO"),
                        rs.getString("NOME_IDOSO"),
                        rs.getString("NOME_REGIAO"),
                        rs.getInt("ANO_NASCIMENTO"),
                        titular,
                        reserva,
                        diag
                ));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("gerar relatorio de rede incompleta", e);
        }
    }

    /**
     * Relatorio 3: Taxa de "nao atendeu" por voluntario.
     */
    public List<TaxaNaoAtendeuDto> listarTaxaNaoAtendeuPorVoluntario() {
        String sql = """
                SELECT v.ID_VOLUNTARIO,
                       v.NOME AS NOME_VOLUNTARIO,
                       v.RELACAO_COMUNIDADE,
                       r.NOME AS NOME_REGIAO,
                       COUNT(CASE WHEN c.STATUS <> 'PENDENTE' THEN 1 END) AS TOTAL_LIGACOES,
                       SUM(CASE WHEN c.STATUS = 'NAO_ATENDEU' THEN 1 ELSE 0 END) AS TOTAL_NAO_ATENDEU
                  FROM VOLUNTARIO v
                  JOIN REGIAO r ON r.ID_REGIAO = v.ID_REGIAO
             LEFT JOIN CHECAGEM c ON c.ID_VOLUNTARIO = v.ID_VOLUNTARIO
                 WHERE v.ATIVO = 1
              GROUP BY v.ID_VOLUNTARIO, v.NOME, v.RELACAO_COMUNIDADE, r.NOME
              ORDER BY TOTAL_NAO_ATENDEU DESC, TOTAL_LIGACOES DESC, v.NOME
                """;
        List<TaxaNaoAtendeuDto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int total = rs.getInt("TOTAL_LIGACOES");
                int naoAtendeu = rs.getInt("TOTAL_NAO_ATENDEU");
                double pct = total > 0 ? Math.round((naoAtendeu * 1000.0) / total) / 10.0 : 0.0;
                lista.add(new TaxaNaoAtendeuDto(
                        rs.getLong("ID_VOLUNTARIO"),
                        rs.getString("NOME_VOLUNTARIO"),
                        rs.getString("RELACAO_COMUNIDADE"),
                        rs.getString("NOME_REGIAO"),
                        total,
                        naoAtendeu,
                        pct
                ));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("gerar relatorio de taxa de nao atendeu", e);
        }
    }

    /**
     * Relatorio 4: Relatorio agregado e anonimizado por regiao para UBS e Defesa Civil (sem nomes pessoais).
     * Tambem alimenta o Mapa de Risco Comunitario (modulo herdado do FloodGuard).
     */
    public List<RelatorioAgregadoRegiaoDto> listarRelatorioAgregadoPorRegiao() {
        String sql = """
                SELECT r.ID_REGIAO,
                       r.NOME AS NOME_REGIAO,
                       r.BAIRRO_ZONA,
                       r.RISCO_CALOR,
                       r.RISCO_CHUVA,
                       r.REFERENCIA_UBS,
                       (SELECT COUNT(*) FROM IDOSO i WHERE i.ID_REGIAO = r.ID_REGIAO AND i.ATIVO = 1) AS TOTAL_IDOSOS,
                       (SELECT COUNT(*) FROM IDOSO i WHERE i.ID_REGIAO = r.ID_REGIAO AND i.ATIVO = 1 AND i.DOENCA_CRONICA = 1) AS COM_CRONICA,
                       (SELECT COUNT(*) FROM IDOSO i WHERE i.ID_REGIAO = r.ID_REGIAO AND i.ATIVO = 1 AND i.MOBILIDADE_REDUZIDA = 1) AS COM_MOBILIDADE,
                       (SELECT COUNT(*) FROM IDOSO i WHERE i.ID_REGIAO = r.ID_REGIAO AND i.ATIVO = 1 AND i.CASA_SEM_VENTILACAO = 1) AS SEM_VENTILACAO,
                       (SELECT COUNT(*) FROM IDOSO i WHERE i.ID_REGIAO = r.ID_REGIAO AND i.ATIVO = 1 AND i.AREA_RISCO = 1) AS AREA_RISCO,
                       (SELECT COUNT(*) FROM ALERTA a WHERE a.STATUS = 'ATIVO' AND (a.ID_REGIAO IS NULL OR a.ID_REGIAO = r.ID_REGIAO)) AS ALERTAS_ATIVOS,
                       (SELECT COUNT(*)
                          FROM CHECAGEM c
                          JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                          JOIN IDOSO i ON i.ID_IDOSO = c.ID_IDOSO
                         WHERE a.STATUS = 'ATIVO' AND i.ID_REGIAO = r.ID_REGIAO AND c.STATUS = 'PENDENTE'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS PENDENTES_ATIVOS,
                       (SELECT COUNT(*)
                          FROM CHECAGEM c
                          JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                          JOIN IDOSO i ON i.ID_IDOSO = c.ID_IDOSO
                         WHERE a.STATUS = 'ATIVO' AND i.ID_REGIAO = r.ID_REGIAO AND c.STATUS = 'CRITICO'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS CRITICOS_ATIVOS,
                       (SELECT COUNT(*)
                          FROM CHECAGEM c
                          JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                          JOIN IDOSO i ON i.ID_IDOSO = c.ID_IDOSO
                         WHERE a.STATUS = 'ATIVO' AND i.ID_REGIAO = r.ID_REGIAO AND c.STATUS = 'PRECISA_AJUDA'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS AJUDA_ATIVOS
                  FROM REGIAO r
                 WHERE r.ATIVO = 1
              ORDER BY CRITICOS_ATIVOS DESC, AJUDA_ATIVOS DESC, TOTAL_IDOSOS DESC, r.NOME
                """;
        List<RelatorioAgregadoRegiaoDto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new RelatorioAgregadoRegiaoDto(
                        rs.getLong("ID_REGIAO"),
                        rs.getString("NOME_REGIAO"),
                        rs.getString("BAIRRO_ZONA"),
                        rs.getString("RISCO_CALOR"),
                        rs.getString("RISCO_CHUVA"),
                        rs.getString("REFERENCIA_UBS"),
                        rs.getInt("TOTAL_IDOSOS"),
                        rs.getInt("COM_CRONICA"),
                        rs.getInt("COM_MOBILIDADE"),
                        rs.getInt("SEM_VENTILACAO"),
                        rs.getInt("AREA_RISCO"),
                        rs.getInt("ALERTAS_ATIVOS"),
                        rs.getInt("PENDENTES_ATIVOS"),
                        rs.getInt("CRITICOS_ATIVOS"),
                        rs.getInt("AJUDA_ATIVOS")
                ));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("gerar relatorio agregado por regiao", e);
        }
    }

    public PainelResumoDto obterResumoPainel() {
        String sql = """
                SELECT (SELECT COUNT(*) FROM IDOSO WHERE ATIVO = 1) AS TOTAL_IDOSOS,
                       (SELECT COUNT(*) FROM VOLUNTARIO WHERE ATIVO = 1) AS TOTAL_VOLUNTARIOS,
                       (SELECT COUNT(*) FROM ALERTA WHERE STATUS = 'ATIVO') AS ALERTAS_ATIVOS,
                       (SELECT COUNT(*) FROM CHECAGEM c JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                         WHERE a.STATUS = 'ATIVO' AND c.STATUS = 'CRITICO'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS CRITICOS,
                       (SELECT COUNT(*) FROM CHECAGEM c JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                         WHERE a.STATUS = 'ATIVO' AND c.STATUS = 'PRECISA_AJUDA'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS AJUDA,
                       (SELECT COUNT(*) FROM CHECAGEM c JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                         WHERE a.STATUS = 'ATIVO' AND c.STATUS = 'PRECISA_ATENCAO'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS ATENCAO,
                       (SELECT COUNT(*) FROM CHECAGEM c JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                         WHERE a.STATUS = 'ATIVO' AND c.STATUS = 'PENDENTE'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS PENDENTES,
                       (SELECT COUNT(*) FROM CHECAGEM c JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
                         WHERE a.STATUS = 'ATIVO' AND c.STATUS = 'ESTA_BEM'
                           AND c.ID_CHECAGEM = (SELECT MAX(c2.ID_CHECAGEM) FROM CHECAGEM c2 WHERE c2.ID_ALERTA = c.ID_ALERTA AND c2.ID_IDOSO = c.ID_IDOSO)
                       ) AS OK_ATIVOS
                  FROM DUAL
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int redeIncompleta = listarIdososComRedeIncompleta().size();
            List<CoberturaAlertaDto> coberturas = listarCoberturaAte4Horas();
            double pct4h = coberturas.isEmpty() ? 0.0 : coberturas.get(0).percentualAte4h();
            return new PainelResumoDto(
                    rs.getInt("TOTAL_IDOSOS"),
                    rs.getInt("TOTAL_VOLUNTARIOS"),
                    rs.getInt("ALERTAS_ATIVOS"),
                    rs.getInt("CRITICOS"),
                    rs.getInt("AJUDA"),
                    rs.getInt("ATENCAO"),
                    rs.getInt("PENDENTES"),
                    rs.getInt("OK_ATIVOS"),
                    redeIncompleta,
                    pct4h
            );
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("obter resumo do painel", e);
        }
    }
}
