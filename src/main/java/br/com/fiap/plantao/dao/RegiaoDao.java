package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Regiao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class RegiaoDao implements Dao<Regiao> {

    @Override
    public Regiao inserir(Regiao r) {
        r.validar();
        String sql = "INSERT INTO regiao (nome, cidade, uf, latitude, longitude) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"ID_REGIAO"})) {
            preencher(ps, r);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                r.setId(rs.getLong(1));
            }
            return r;
        } catch (SQLException e) {
            throw Dao.erro("cadastrar a regi\u00e3o", e);
        }
    }

    @Override
    public Regiao buscarPorId(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM regiao WHERE id_regiao = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecursoNaoEncontradoException("Regi\u00e3o n\u00e3o encontrada.");
                return montar(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("buscar a regi\u00e3o", e);
        }
    }

    @Override
    public List<Regiao> listar() {
        List<Regiao> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM regiao ORDER BY cidade, nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(montar(rs));
            return lista;
        } catch (SQLException e) {
            throw Dao.erro("listar as regi\u00f5es", e);
        }
    }

    @Override
    public void atualizar(Regiao r) {
        r.validar();
        String sql = "UPDATE regiao SET nome = ?, cidade = ?, uf = ?, latitude = ?, longitude = ? WHERE id_regiao = ?";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, r);
            ps.setLong(6, r.getId());
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Regi\u00e3o n\u00e3o encontrada.");
        } catch (SQLException e) {
            throw Dao.erro("atualizar a regi\u00e3o", e);
        }
    }

    // a FK impede excluir regiao que ja tem idoso, voluntario ou alerta
    @Override
    public void excluir(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM regiao WHERE id_regiao = ?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Regi\u00e3o n\u00e3o encontrada.");
        } catch (SQLException e) {
            throw Dao.erro("excluir a regi\u00e3o", e);
        }
    }

    private void preencher(PreparedStatement ps, Regiao r) throws SQLException {
        ps.setString(1, r.getNome().trim());
        ps.setString(2, r.getCidade().trim());
        ps.setString(3, r.getUf());
        if (r.getLatitude() == null) ps.setNull(4, Types.NUMERIC); else ps.setDouble(4, r.getLatitude());
        if (r.getLongitude() == null) ps.setNull(5, Types.NUMERIC); else ps.setDouble(5, r.getLongitude());
    }

    private Regiao montar(ResultSet rs) throws SQLException {
        Regiao r = new Regiao();
        r.setId(rs.getLong("id_regiao"));
        r.setNome(rs.getString("nome"));
        r.setCidade(rs.getString("cidade"));
        r.setUf(rs.getString("uf"));
        double lat = rs.getDouble("latitude");
        r.setLatitude(rs.wasNull() ? null : lat);
        double lon = rs.getDouble("longitude");
        r.setLongitude(rs.wasNull() ? null : lon);
        return r;
    }
}
