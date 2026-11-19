package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.Regiao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegiaoDao implements CrudDao<Regiao> {

    @Override
    public Regiao inserir(Regiao regiao) {
        String sql = """
                INSERT INTO REGIAO (NOME, BAIRRO_ZONA, RISCO_CALOR, RISCO_CHUVA, REFERENCIA_UBS, ATIVO)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_REGIAO"})) {
            ps.setString(1, regiao.getNome());
            ps.setString(2, regiao.getBairroZona());
            ps.setString(3, regiao.getRiscoCalor());
            ps.setString(4, regiao.getRiscoChuva());
            ps.setString(5, regiao.getReferenciaUbs());
            ps.setInt(6, regiao.isAtivo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    regiao.setId(rs.getLong(1));
                }
            }
            return regiao;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("inserir regiao", e);
        }
    }

    @Override
    public List<Regiao> listarTodos() {
        String sql = "SELECT ID_REGIAO, NOME, BAIRRO_ZONA, RISCO_CALOR, RISCO_CHUVA, REFERENCIA_UBS, ATIVO FROM REGIAO ORDER BY NOME";
        List<Regiao> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar regioes", e);
        }
    }

    @Override
    public Optional<Regiao> buscarPorId(Long id) {
        String sql = "SELECT ID_REGIAO, NOME, BAIRRO_ZONA, RISCO_CALOR, RISCO_CHUVA, REFERENCIA_UBS, ATIVO FROM REGIAO WHERE ID_REGIAO = ?";
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
            throw DatabaseConfig.traduzirErroSql("buscar regiao", e);
        }
    }

    @Override
    public Regiao atualizar(Regiao regiao) {
        String sql = """
                UPDATE REGIAO
                   SET NOME = ?, BAIRRO_ZONA = ?, RISCO_CALOR = ?, RISCO_CHUVA = ?, REFERENCIA_UBS = ?, ATIVO = ?
                 WHERE ID_REGIAO = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regiao.getNome());
            ps.setString(2, regiao.getBairroZona());
            ps.setString(3, regiao.getRiscoCalor());
            ps.setString(4, regiao.getRiscoChuva());
            ps.setString(5, regiao.getReferenciaUbs());
            ps.setInt(6, regiao.isAtivo() ? 1 : 0);
            ps.setLong(7, regiao.getId());
            ps.executeUpdate();
            return regiao;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("atualizar regiao", e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "DELETE FROM REGIAO WHERE ID_REGIAO = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("excluir regiao", e);
        }
    }

    private Regiao mapear(ResultSet rs) throws SQLException {
        return new Regiao(
                rs.getLong("ID_REGIAO"),
                rs.getString("NOME"),
                rs.getString("BAIRRO_ZONA"),
                rs.getString("RISCO_CALOR"),
                rs.getString("RISCO_CHUVA"),
                rs.getString("REFERENCIA_UBS"),
                rs.getInt("ATIVO") == 1
        );
    }
}
