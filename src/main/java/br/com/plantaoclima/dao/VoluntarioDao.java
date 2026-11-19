package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.Voluntario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VoluntarioDao implements CrudDao<Voluntario> {

    private static final String BASE_SELECT = """
            SELECT v.ID_VOLUNTARIO, v.NOME, v.TELEFONE, v.RELACAO_COMUNIDADE,
                   v.DISPONIBILIDADE, v.ID_REGIAO, r.NOME AS NOME_REGIAO, v.ATIVO, v.CRIADO_EM
              FROM VOLUNTARIO v
              JOIN REGIAO r ON r.ID_REGIAO = v.ID_REGIAO
            """;

    @Override
    public Voluntario inserir(Voluntario voluntario) {
        String sql = """
                INSERT INTO VOLUNTARIO (NOME, TELEFONE, RELACAO_COMUNIDADE, DISPONIBILIDADE, ID_REGIAO, ATIVO)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_VOLUNTARIO"})) {
            ps.setString(1, voluntario.getNome());
            ps.setString(2, voluntario.getTelefone());
            ps.setString(3, voluntario.getRelacaoComunidade());
            ps.setString(4, voluntario.getDisponibilidade());
            ps.setLong(5, voluntario.getIdRegiao());
            ps.setInt(6, voluntario.isAtivo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    voluntario.setId(rs.getLong(1));
                }
            }
            return buscarPorId(voluntario.getId()).orElse(voluntario);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("inserir voluntario", e);
        }
    }

    @Override
    public List<Voluntario> listarTodos() {
        String sql = BASE_SELECT + " ORDER BY v.ATIVO DESC, v.NOME";
        List<Voluntario> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar voluntarios", e);
        }
    }

    @Override
    public Optional<Voluntario> buscarPorId(Long id) {
        String sql = BASE_SELECT + " WHERE v.ID_VOLUNTARIO = ?";
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
            throw DatabaseConfig.traduzirErroSql("buscar voluntario", e);
        }
    }

    @Override
    public Voluntario atualizar(Voluntario voluntario) {
        String sql = """
                UPDATE VOLUNTARIO
                   SET NOME = ?, TELEFONE = ?, RELACAO_COMUNIDADE = ?,
                       DISPONIBILIDADE = ?, ID_REGIAO = ?, ATIVO = ?
                 WHERE ID_VOLUNTARIO = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voluntario.getNome());
            ps.setString(2, voluntario.getTelefone());
            ps.setString(3, voluntario.getRelacaoComunidade());
            ps.setString(4, voluntario.getDisponibilidade());
            ps.setLong(5, voluntario.getIdRegiao());
            ps.setInt(6, voluntario.isAtivo() ? 1 : 0);
            ps.setLong(7, voluntario.getId());
            ps.executeUpdate();
            return buscarPorId(voluntario.getId()).orElse(voluntario);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("atualizar voluntario", e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "DELETE FROM VOLUNTARIO WHERE ID_VOLUNTARIO = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("excluir voluntario", e);
        }
    }

    private Voluntario mapear(ResultSet rs) throws SQLException {
        Voluntario v = new Voluntario();
        v.setId(rs.getLong("ID_VOLUNTARIO"));
        v.setNome(rs.getString("NOME"));
        v.setTelefone(rs.getString("TELEFONE"));
        v.setRelacaoComunidade(rs.getString("RELACAO_COMUNIDADE"));
        v.setDisponibilidade(rs.getString("DISPONIBILIDADE"));
        v.setIdRegiao(rs.getLong("ID_REGIAO"));
        v.setNomeRegiao(rs.getString("NOME_REGIAO"));
        v.setAtivo(rs.getInt("ATIVO") == 1);
        Timestamp criado = rs.getTimestamp("CRIADO_EM");
        if (criado != null) {
            v.setCriadoEm(criado.toLocalDateTime());
        }
        return v;
    }
}
