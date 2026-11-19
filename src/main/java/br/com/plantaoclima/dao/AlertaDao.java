package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.Alerta;
import br.com.plantaoclima.model.SeveridadeAlerta;
import br.com.plantaoclima.model.StatusAlerta;
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

public class AlertaDao implements CrudDao<Alerta> {

    private static final String BASE_SELECT = """
            SELECT a.ID_ALERTA, a.TITULO, a.TIPO, a.SEVERIDADE, a.FONTE, a.DESCRICAO,
                   a.STATUS, a.ID_REGIAO, r.NOME AS NOME_REGIAO, a.EMITIDO_EM, a.ENCERRADO_EM
              FROM ALERTA a
         LEFT JOIN REGIAO r ON r.ID_REGIAO = a.ID_REGIAO
            """;

    @Override
    public Alerta inserir(Alerta alerta) {
        String sql = """
                INSERT INTO ALERTA (TITULO, TIPO, SEVERIDADE, FONTE, DESCRICAO, STATUS, ID_REGIAO)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_ALERTA"})) {
            ps.setString(1, alerta.getTitulo());
            ps.setString(2, alerta.getTipo().name());
            ps.setString(3, alerta.getSeveridade().name());
            ps.setString(4, alerta.getFonte());
            ps.setString(5, alerta.getDescricao());
            ps.setString(6, alerta.getStatus().name());
            if (alerta.getIdRegiao() != null) {
                ps.setLong(7, alerta.getIdRegiao());
            } else {
                ps.setNull(7, Types.NUMERIC);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    alerta.setId(rs.getLong(1));
                }
            }
            return buscarPorId(alerta.getId()).orElse(alerta);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("inserir alerta", e);
        }
    }

    @Override
    public List<Alerta> listarTodos() {
        String sql = BASE_SELECT + " ORDER BY CASE WHEN a.STATUS = 'ATIVO' THEN 0 ELSE 1 END, a.EMITIDO_EM DESC";
        List<Alerta> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar alertas", e);
        }
    }

    @Override
    public Optional<Alerta> buscarPorId(Long id) {
        String sql = BASE_SELECT + " WHERE a.ID_ALERTA = ?";
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
            throw DatabaseConfig.traduzirErroSql("buscar alerta", e);
        }
    }

    @Override
    public Alerta atualizar(Alerta alerta) {
        String sql = """
                UPDATE ALERTA
                   SET TITULO = ?, TIPO = ?, SEVERIDADE = ?, FONTE = ?, DESCRICAO = ?,
                       STATUS = ?, ID_REGIAO = ?, ENCERRADO_EM = ?
                 WHERE ID_ALERTA = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alerta.getTitulo());
            ps.setString(2, alerta.getTipo().name());
            ps.setString(3, alerta.getSeveridade().name());
            ps.setString(4, alerta.getFonte());
            ps.setString(5, alerta.getDescricao());
            ps.setString(6, alerta.getStatus().name());
            if (alerta.getIdRegiao() != null) {
                ps.setLong(7, alerta.getIdRegiao());
            } else {
                ps.setNull(7, Types.NUMERIC);
            }
            if (alerta.getEncerradoEm() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(alerta.getEncerradoEm()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }
            ps.setLong(9, alerta.getId());
            ps.executeUpdate();
            return buscarPorId(alerta.getId()).orElse(alerta);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("atualizar alerta", e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "DELETE FROM ALERTA WHERE ID_ALERTA = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("excluir alerta", e);
        }
    }

    private Alerta mapear(ResultSet rs) throws SQLException {
        Alerta a = new Alerta();
        a.setId(rs.getLong("ID_ALERTA"));
        a.setTitulo(rs.getString("TITULO"));
        a.setTipo(TipoAlerta.valueOf(rs.getString("TIPO")));
        a.setSeveridade(SeveridadeAlerta.valueOf(rs.getString("SEVERIDADE")));
        a.setFonte(rs.getString("FONTE"));
        a.setDescricao(rs.getString("DESCRICAO"));
        a.setStatus(StatusAlerta.valueOf(rs.getString("STATUS")));
        long regId = rs.getLong("ID_REGIAO");
        if (!rs.wasNull()) {
            a.setIdRegiao(regId);
        }
        a.setNomeRegiao(rs.getString("NOME_REGIAO") != null ? rs.getString("NOME_REGIAO") : "Todas as Regioes");
        Timestamp emitido = rs.getTimestamp("EMITIDO_EM");
        if (emitido != null) {
            a.setEmitidoEm(emitido.toLocalDateTime());
        }
        Timestamp encerrado = rs.getTimestamp("ENCERRADO_EM");
        if (encerrado != null) {
            a.setEncerradoEm(encerrado.toLocalDateTime());
        }
        return a;
    }
}
