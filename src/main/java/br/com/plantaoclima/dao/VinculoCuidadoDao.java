package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.PapelVinculo;
import br.com.plantaoclima.model.VinculoCuidado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VinculoCuidadoDao implements CrudDao<VinculoCuidado> {

    private static final String BASE_SELECT = """
            SELECT vc.ID_VINCULO, vc.ID_IDOSO, vc.ID_VOLUNTARIO, vc.PAPEL, vc.PRIORIDADE, vc.CRIADO_EM,
                   i.NOME AS NOME_IDOSO, i.TELEFONE AS TEL_IDOSO,
                   v.NOME AS NOME_VOLUNTARIO, v.TELEFONE AS TEL_VOLUNTARIO, v.RELACAO_COMUNIDADE
              FROM VINCULO_CUIDADO vc
              JOIN IDOSO i ON i.ID_IDOSO = vc.ID_IDOSO
              JOIN VOLUNTARIO v ON v.ID_VOLUNTARIO = vc.ID_VOLUNTARIO
            """;

    @Override
    public VinculoCuidado inserir(VinculoCuidado vinculo) {
        String sql = """
                INSERT INTO VINCULO_CUIDADO (ID_IDOSO, ID_VOLUNTARIO, PAPEL, PRIORIDADE)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_VINCULO"})) {
            ps.setLong(1, vinculo.getIdIdoso());
            ps.setLong(2, vinculo.getIdVoluntario());
            ps.setString(3, vinculo.getPapel().name());
            ps.setInt(4, vinculo.getPrioridade());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    vinculo.setId(rs.getLong(1));
                }
            }
            return buscarPorId(vinculo.getId()).orElse(vinculo);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("inserir vinculo de cuidado", e);
        }
    }

    @Override
    public List<VinculoCuidado> listarTodos() {
        String sql = BASE_SELECT + " ORDER BY i.NOME, vc.PRIORIDADE";
        List<VinculoCuidado> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar vinculos de cuidado", e);
        }
    }

    public List<VinculoCuidado> listarPorIdoso(Long idIdoso) {
        String sql = BASE_SELECT + " WHERE vc.ID_IDOSO = ? ORDER BY vc.PRIORIDADE";
        List<VinculoCuidado> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idIdoso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar vinculos por idoso", e);
        }
    }

    public Optional<VinculoCuidado> buscarPorIdosoEPapel(Long idIdoso, PapelVinculo papel) {
        String sql = BASE_SELECT + " WHERE vc.ID_IDOSO = ? AND vc.PAPEL = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idIdoso);
            ps.setString(2, papel.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("buscar vinculo por papel", e);
        }
    }

    @Override
    public Optional<VinculoCuidado> buscarPorId(Long id) {
        String sql = BASE_SELECT + " WHERE vc.ID_VINCULO = ?";
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
            throw DatabaseConfig.traduzirErroSql("buscar vinculo de cuidado", e);
        }
    }

    @Override
    public VinculoCuidado atualizar(VinculoCuidado vinculo) {
        String sql = """
                UPDATE VINCULO_CUIDADO
                   SET ID_IDOSO = ?, ID_VOLUNTARIO = ?, PAPEL = ?, PRIORIDADE = ?
                 WHERE ID_VINCULO = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, vinculo.getIdIdoso());
            ps.setLong(2, vinculo.getIdVoluntario());
            ps.setString(3, vinculo.getPapel().name());
            ps.setInt(4, vinculo.getPrioridade());
            ps.setLong(5, vinculo.getId());
            ps.executeUpdate();
            return buscarPorId(vinculo.getId()).orElse(vinculo);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("atualizar vinculo de cuidado", e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "DELETE FROM VINCULO_CUIDADO WHERE ID_VINCULO = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("excluir vinculo de cuidado", e);
        }
    }

    private VinculoCuidado mapear(ResultSet rs) throws SQLException {
        VinculoCuidado vc = new VinculoCuidado();
        vc.setId(rs.getLong("ID_VINCULO"));
        vc.setIdIdoso(rs.getLong("ID_IDOSO"));
        vc.setIdVoluntario(rs.getLong("ID_VOLUNTARIO"));
        vc.setPapel(PapelVinculo.valueOf(rs.getString("PAPEL")));
        vc.setPrioridade(rs.getInt("PRIORIDADE"));
        Timestamp criado = rs.getTimestamp("CRIADO_EM");
        if (criado != null) {
            vc.setCriadoEm(criado.toLocalDateTime());
        }
        vc.setNomeIdoso(rs.getString("NOME_IDOSO"));
        vc.setTelefoneIdoso(rs.getString("TEL_IDOSO"));
        vc.setNomeVoluntario(rs.getString("NOME_VOLUNTARIO"));
        vc.setTelefoneVoluntario(rs.getString("TEL_VOLUNTARIO"));
        vc.setRelacaoVoluntario(rs.getString("RELACAO_COMUNIDADE"));
        return vc;
    }
}
