package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.Checagem;
import br.com.plantaoclima.model.PapelChecagem;
import br.com.plantaoclima.model.StatusChecagem;
import br.com.plantaoclima.model.TipoAlerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChecagemDao implements CrudDao<Checagem> {

    private static final String BASE_SELECT = """
            SELECT c.ID_CHECAGEM, c.ID_ALERTA, c.ID_IDOSO, c.ID_VOLUNTARIO,
                   c.PAPEL_ATUAL, c.PONTUACAO_RISCO, c.STATUS, c.TENTATIVAS,
                   c.CHECKLIST_RESUMO, c.OBSERVACAO, c.CRIADO_EM, c.ATUALIZADO_EM,
                   a.TITULO AS TITULO_ALERTA, a.TIPO AS TIPO_ALERTA,
                   i.NOME AS NOME_IDOSO, i.TELEFONE AS TEL_IDOSO, i.ANO_NASCIMENTO,
                   i.ENDERECO_RESUMO, i.DOENCA_CRONICA, i.MOBILIDADE_REDUZIDA,
                   i.CASA_SEM_VENTILACAO, i.AREA_RISCO,
                   r.NOME AS NOME_REGIAO,
                   v.NOME AS NOME_VOLUNTARIO, v.TELEFONE AS TEL_VOLUNTARIO
              FROM CHECAGEM c
              JOIN ALERTA a ON a.ID_ALERTA = c.ID_ALERTA
              JOIN IDOSO i ON i.ID_IDOSO = c.ID_IDOSO
              JOIN REGIAO r ON r.ID_REGIAO = i.ID_REGIAO
         LEFT JOIN VOLUNTARIO v ON v.ID_VOLUNTARIO = c.ID_VOLUNTARIO
            """;

    @Override
    public Checagem inserir(Checagem checagem) {
        String sql = """
                INSERT INTO CHECAGEM (
                    ID_ALERTA, ID_IDOSO, ID_VOLUNTARIO, PAPEL_ATUAL, PONTUACAO_RISCO,
                    STATUS, TENTATIVAS, CHECKLIST_RESUMO, OBSERVACAO, ATUALIZADO_EM
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_CHECAGEM"})) {
            ps.setLong(1, checagem.getIdAlerta());
            ps.setLong(2, checagem.getIdIdoso());
            if (checagem.getIdVoluntario() != null) {
                ps.setLong(3, checagem.getIdVoluntario());
            } else {
                ps.setNull(3, Types.NUMERIC);
            }
            ps.setString(4, checagem.getPapelAtual().name());
            ps.setInt(5, checagem.getPontuacaoRisco());
            ps.setString(6, checagem.getStatus().name());
            ps.setInt(7, checagem.getTentativas());
            ps.setString(8, checagem.getChecklistResumo());
            ps.setString(9, checagem.getObservacao());
            if (checagem.getAtualizadoEm() != null) {
                ps.setTimestamp(10, Timestamp.valueOf(checagem.getAtualizadoEm()));
            } else {
                ps.setNull(10, Types.TIMESTAMP);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    checagem.setId(rs.getLong(1));
                }
            }
            return buscarPorId(checagem.getId()).orElse(checagem);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("inserir checagem", e);
        }
    }

    @Override
    public List<Checagem> listarTodos() {
        String sql = BASE_SELECT + " ORDER BY c.ID_ALERTA DESC, c.PONTUACAO_RISCO DESC, c.ID_CHECAGEM DESC";
        List<Checagem> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar checagens", e);
        }
    }

    /**
     * Retorna a rodada de ligacoes de um alerta (etapa mais recente de cada idoso),
     * priorizando CRITICO -> PRECISA_AJUDA -> PENDENTE -> PRECISA_ATENCAO -> ESTA_BEM,
     * e dentro de cada grupo pela maior pontuacao de risco.
     */
    public List<Checagem> listarRodadaAtualPorAlerta(Long idAlerta) {
        String sql = BASE_SELECT + """
                WHERE c.ID_ALERTA = ?
                  AND c.ID_CHECAGEM = (
                      SELECT MAX(c2.ID_CHECAGEM)
                        FROM CHECAGEM c2
                       WHERE c2.ID_ALERTA = c.ID_ALERTA
                         AND c2.ID_IDOSO = c.ID_IDOSO
                  )
                ORDER BY CASE c.STATUS
                             WHEN 'CRITICO' THEN 1
                             WHEN 'PRECISA_AJUDA' THEN 2
                             WHEN 'PENDENTE' THEN 3
                             WHEN 'PRECISA_ATENCAO' THEN 4
                             WHEN 'NAO_ATENDEU' THEN 5
                             ELSE 6
                         END,
                         c.PONTUACAO_RISCO DESC,
                         i.NOME
                """;
        List<Checagem> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idAlerta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar rodada do alerta", e);
        }
    }

    /**
     * Lista casos criticos ou de ajuda imediata em alertas ativos para o topo do Painel.
     */
    public List<Checagem> listarCasosCriticosAtivos() {
        String sql = BASE_SELECT + """
                WHERE a.STATUS = 'ATIVO'
                  AND c.STATUS IN ('CRITICO', 'PRECISA_AJUDA')
                  AND c.ID_CHECAGEM = (
                      SELECT MAX(c2.ID_CHECAGEM)
                        FROM CHECAGEM c2
                       WHERE c2.ID_ALERTA = c.ID_ALERTA
                         AND c2.ID_IDOSO = c.ID_IDOSO
                  )
                ORDER BY CASE c.STATUS WHEN 'CRITICO' THEN 0 ELSE 1 END,
                         c.PONTUACAO_RISCO DESC
                """;
        List<Checagem> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar casos criticos ativos", e);
        }
    }

    public Optional<Checagem> buscarPorAlertaIdosoEPapel(Long idAlerta, Long idIdoso, PapelChecagem papel) {
        String sql = BASE_SELECT + " WHERE c.ID_ALERTA = ? AND c.ID_IDOSO = ? AND c.PAPEL_ATUAL = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idAlerta);
            ps.setLong(2, idIdoso);
            ps.setString(3, papel.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("buscar etapa de checagem", e);
        }
    }

    @Override
    public Optional<Checagem> buscarPorId(Long id) {
        String sql = BASE_SELECT + " WHERE c.ID_CHECAGEM = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("buscar checagem", e);
        }
    }

    @Override
    public Checagem atualizar(Checagem checagem) {
        String sql = """
                UPDATE CHECAGEM
                   SET ID_VOLUNTARIO = ?, PAPEL_ATUAL = ?, PONTUACAO_RISCO = ?,
                       STATUS = ?, TENTATIVAS = ?, CHECKLIST_RESUMO = ?,
                       OBSERVACAO = ?, ATUALIZADO_EM = ?
                 WHERE ID_CHECAGEM = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (checagem.getIdVoluntario() != null) {
                ps.setLong(1, checagem.getIdVoluntario());
            } else {
                ps.setNull(1, Types.NUMERIC);
            }
            ps.setString(2, checagem.getPapelAtual().name());
            ps.setInt(3, checagem.getPontuacaoRisco());
            ps.setString(4, checagem.getStatus().name());
            ps.setInt(5, checagem.getTentativas());
            ps.setString(6, checagem.getChecklistResumo());
            ps.setString(7, checagem.getObservacao());
            if (checagem.getAtualizadoEm() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(checagem.getAtualizadoEm()));
            } else {
                ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            }
            ps.setLong(9, checagem.getId());
            ps.executeUpdate();
            return buscarPorId(checagem.getId()).orElse(checagem);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("atualizar checagem", e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "DELETE FROM CHECAGEM WHERE ID_CHECAGEM = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("excluir checagem", e);
        }
    }

    private Checagem mapear(ResultSet rs) throws SQLException {
        Checagem c = new Checagem();
        c.setId(rs.getLong("ID_CHECAGEM"));
        c.setIdAlerta(rs.getLong("ID_ALERTA"));
        c.setIdIdoso(rs.getLong("ID_IDOSO"));
        long idVol = rs.getLong("ID_VOLUNTARIO");
        if (!rs.wasNull()) {
            c.setIdVoluntario(idVol);
        }
        c.setPapelAtual(PapelChecagem.valueOf(rs.getString("PAPEL_ATUAL")));
        c.setPontuacaoRisco(rs.getInt("PONTUACAO_RISCO"));
        c.setStatus(StatusChecagem.valueOf(rs.getString("STATUS")));
        c.setTentativas(rs.getInt("TENTATIVAS"));
        c.setChecklistResumo(rs.getString("CHECKLIST_RESUMO"));
        c.setObservacao(rs.getString("OBSERVACAO"));
        Timestamp criado = rs.getTimestamp("CRIADO_EM");
        if (criado != null) {
            c.setCriadoEm(criado.toLocalDateTime());
        }
        Timestamp atualizado = rs.getTimestamp("ATUALIZADO_EM");
        if (atualizado != null) {
            c.setAtualizadoEm(atualizado.toLocalDateTime());
        }
        c.setTituloAlerta(rs.getString("TITULO_ALERTA"));
        c.setTipoAlerta(TipoAlerta.valueOf(rs.getString("TIPO_ALERTA")));
        c.setNomeIdoso(rs.getString("NOME_IDOSO"));
        c.setTelefoneIdoso(rs.getString("TEL_IDOSO"));
        c.setAnoNascimentoIdoso(rs.getInt("ANO_NASCIMENTO"));
        c.setEnderecoIdoso(rs.getString("ENDERECO_RESUMO"));
        c.setDoencaCronica(rs.getInt("DOENCA_CRONICA") == 1);
        c.setMobilidadeReduzida(rs.getInt("MOBILIDADE_REDUZIDA") == 1);
        c.setCasaSemVentilacao(rs.getInt("CASA_SEM_VENTILACAO") == 1);
        c.setAreaRisco(rs.getInt("AREA_RISCO") == 1);
        c.setNomeRegiao(rs.getString("NOME_REGIAO"));
        c.setNomeVoluntario(rs.getString("NOME_VOLUNTARIO"));
        c.setTelefoneVoluntario(rs.getString("TEL_VOLUNTARIO"));
        return c;
    }
}
