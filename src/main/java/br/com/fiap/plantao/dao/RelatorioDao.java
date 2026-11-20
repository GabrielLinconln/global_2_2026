package br.com.fiap.plantao.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// relatorios com JOIN e agregacao (mesmas queries do final de sql/script.sql)
public class RelatorioDao {

    // R1: cobertura de cada alerta do dia por regiao; agregado, sem nomes (vai para UBS/Defesa Civil)
    public List<Map<String, Object>> coberturaPorRegiao(LocalDate dia) {
        String sql = "WITH por_idoso AS ("
                + " SELECT a.id_alerta, a.id_regiao, a.tipo, a.nivel, i.id_idoso,"
                + "  MAX(CASE WHEN c.resultado IN ('OK','ATENCAO','URGENTE') THEN 1 ELSE 0 END) AS contatado,"
                + "  MAX(CASE WHEN c.resultado IN ('OK','ATENCAO','URGENTE') AND c.data_hora <= a.criado_em + INTERVAL '4' HOUR THEN 1 ELSE 0 END) AS ate_4h,"
                + "  MAX(CASE WHEN c.resultado = 'URGENTE' THEN 1 ELSE 0 END) AS urgente,"
                + "  MAX(CASE WHEN c.resultado = 'NAO_ATENDEU' THEN 1 ELSE 0 END) AS nao_atendeu,"
                + "  MAX(CASE WHEN c.resultado = 'PENDENTE' THEN 1 ELSE 0 END) AS pendente"
                + " FROM alerta a"
                + " JOIN idoso i ON i.id_regiao = a.id_regiao AND i.ativo = 'S'"
                + " LEFT JOIN checagem c ON c.id_alerta = a.id_alerta AND c.id_idoso = i.id_idoso"
                + " WHERE a.data_alerta = ?"
                + " GROUP BY a.id_alerta, a.id_regiao, a.tipo, a.nivel, i.id_idoso)"
                + " SELECT r.nome AS regiao, r.cidade, p.tipo, p.nivel, COUNT(*) AS idosos_ativos,"
                + "  SUM(p.contatado) AS contatados, SUM(p.ate_4h) AS contatados_ate_4h, SUM(p.urgente) AS urgentes,"
                + "  SUM(CASE WHEN p.nao_atendeu = 1 AND p.pendente = 0 AND p.contatado = 0 THEN 1 ELSE 0 END) AS criticos,"
                + "  ROUND(100.0 * SUM(p.ate_4h) / NULLIF(COUNT(*), 0), 1) AS pct_ate_4h"
                + " FROM por_idoso p JOIN regiao r ON r.id_regiao = p.id_regiao"
                + " GROUP BY r.id_regiao, r.nome, r.cidade, p.id_alerta, p.tipo, p.nivel"
                + " ORDER BY pct_ate_4h";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Dao.data(dia));
            try (ResultSet rs = ps.executeQuery()) {
                return Dao.linhas(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("gerar o relat\u00f3rio de cobertura", e);
        }
    }

    // R2: furos na rede - idosos com menos de 2 cuidadores ativos
    public List<Map<String, Object>> furosNaRede() {
        return consultar("SELECT r.nome AS regiao, i.nome AS idoso, COUNT(v.id_voluntario) AS cuidadores_ativos"
                + " FROM idoso i JOIN regiao r ON r.id_regiao = i.id_regiao"
                + " LEFT JOIN vinculo_cuidado vc ON vc.id_idoso = i.id_idoso"
                + " LEFT JOIN voluntario v ON v.id_voluntario = vc.id_voluntario AND v.ativo = 'S'"
                + " WHERE i.ativo = 'S'"
                + " GROUP BY r.nome, i.id_idoso, i.nome"
                + " HAVING COUNT(v.id_voluntario) < 2"
                + " ORDER BY cuidadores_ativos, r.nome", "gerar o relat\u00f3rio de furos na rede");
    }

    // R3: carga e taxa de "nao atendeu" por voluntario
    public List<Map<String, Object>> cargaVoluntarios() {
        return consultar("SELECT v.nome AS voluntario, v.tipo,"
                + " COUNT(DISTINCT vc.id_idoso) AS idosos_sob_cuidado,"
                + " COUNT(DISTINCT c.id_checagem) AS ligacoes_feitas,"
                + " COUNT(DISTINCT CASE WHEN c.resultado = 'NAO_ATENDEU' THEN c.id_checagem END) AS nao_atendeu,"
                + " ROUND(100.0 * COUNT(DISTINCT CASE WHEN c.resultado = 'NAO_ATENDEU' THEN c.id_checagem END)"
                + "  / NULLIF(COUNT(DISTINCT c.id_checagem), 0), 1) AS pct_nao_atendeu"
                + " FROM voluntario v"
                + " LEFT JOIN vinculo_cuidado vc ON vc.id_voluntario = v.id_voluntario"
                + " LEFT JOIN checagem c ON c.id_voluntario = v.id_voluntario AND c.resultado <> 'PENDENTE'"
                + " WHERE v.ativo = 'S'"
                + " GROUP BY v.id_voluntario, v.nome, v.tipo"
                + " ORDER BY pct_nao_atendeu DESC NULLS LAST", "gerar o relat\u00f3rio de volunt\u00e1rios");
    }

    private List<Map<String, Object>> consultar(String sql, String acao) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return Dao.linhas(rs);
        } catch (SQLException e) {
            throw Dao.erro(acao, e);
        }
    }
}
