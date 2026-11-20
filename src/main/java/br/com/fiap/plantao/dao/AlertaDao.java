package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Alerta;
import br.com.fiap.plantao.model.TipoAlerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AlertaDao implements Dao<Alerta> {

    @Override
    public Alerta inserir(Alerta a) {
        a.validar();
        String sql = "INSERT INTO alerta (id_regiao, tipo, data_alerta, nivel, temp_max, origem, fonte_aviso) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"ID_ALERTA"})) {
            preencher(ps, a);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                a.setId(rs.getLong(1));
            }
            return buscarPorId(a.getId());
        } catch (SQLException e) {
            throw Dao.erro("cadastrar o alerta (j\u00e1 existe alerta desse tipo nessa regi\u00e3o e data?)", e);
        }
    }

    @Override
    public Alerta buscarPorId(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM alerta WHERE id_alerta = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecursoNaoEncontradoException("Alerta n\u00e3o encontrado.");
                return montar(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("buscar o alerta", e);
        }
    }

    @Override
    public List<Alerta> listar() {
        List<Alerta> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM alerta ORDER BY data_alerta DESC, id_alerta DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(montar(rs));
            return lista;
        } catch (SQLException e) {
            throw Dao.erro("listar os alertas", e);
        }
    }

    @Override
    public void atualizar(Alerta a) {
        a.validar();
        String sql = "UPDATE alerta SET id_regiao = ?, tipo = ?, data_alerta = ?, nivel = ?, temp_max = ?, origem = ?, fonte_aviso = ? WHERE id_alerta = ?";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, a);
            ps.setLong(8, a.getId());
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Alerta n\u00e3o encontrado.");
        } catch (SQLException e) {
            throw Dao.erro("atualizar o alerta", e);
        }
    }

    // a FK impede excluir alerta que ja tem ligacoes (historico das metricas)
    @Override
    public void excluir(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM alerta WHERE id_alerta = ?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Alerta n\u00e3o encontrado.");
        } catch (SQLException e) {
            throw Dao.erro("excluir o alerta (j\u00e1 tem liga\u00e7\u00f5es registradas?)", e);
        }
    }

    private void preencher(PreparedStatement ps, Alerta a) throws SQLException {
        ps.setLong(1, a.getIdRegiao());
        ps.setString(2, a.getTipo().name());
        ps.setDate(3, Dao.data(a.getDataAlerta()));
        ps.setString(4, a.getNivel());
        if (a.getTempMax() == null) ps.setNull(5, Types.NUMERIC); else ps.setDouble(5, a.getTempMax());
        ps.setString(6, a.getOrigem());
        ps.setString(7, a.getFonteAviso());
    }

    static Alerta montar(ResultSet rs) throws SQLException {
        Alerta a = new Alerta();
        a.setId(rs.getLong("id_alerta"));
        a.setIdRegiao(rs.getLong("id_regiao"));
        a.setTipo(TipoAlerta.valueOf(rs.getString("tipo")));
        a.setDataAlerta(rs.getDate("data_alerta").toLocalDate());
        a.setNivel(rs.getString("nivel"));
        double t = rs.getDouble("temp_max");
        a.setTempMax(rs.wasNull() ? null : t);
        a.setOrigem(rs.getString("origem"));
        a.setFonteAviso(rs.getString("fonte_aviso"));
        a.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        return a;
    }
}
