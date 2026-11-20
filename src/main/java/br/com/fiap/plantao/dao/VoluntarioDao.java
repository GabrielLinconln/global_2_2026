package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Voluntario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VoluntarioDao implements Dao<Voluntario> {

    @Override
    public Voluntario inserir(Voluntario v) {
        v.validar();
        String sql = "INSERT INTO voluntario (id_regiao, nome, telefone, tipo) VALUES (?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"ID_VOLUNTARIO"})) {
            preencher(ps, v);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                v.setId(rs.getLong(1));
            }
            return v;
        } catch (SQLException e) {
            throw Dao.erro("cadastrar o volunt\u00e1rio (telefone j\u00e1 cadastrado?)", e);
        }
    }

    @Override
    public Voluntario buscarPorId(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM voluntario WHERE id_voluntario = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecursoNaoEncontradoException("Volunt\u00e1rio n\u00e3o encontrado.");
                return montar(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("buscar o volunt\u00e1rio", e);
        }
    }

    @Override
    public List<Voluntario> listar() {
        List<Voluntario> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM voluntario WHERE ativo = 'S' ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(montar(rs));
            return lista;
        } catch (SQLException e) {
            throw Dao.erro("listar os volunt\u00e1rios", e);
        }
    }

    @Override
    public void atualizar(Voluntario v) {
        v.validar();
        String sql = "UPDATE voluntario SET id_regiao = ?, nome = ?, telefone = ?, tipo = ? WHERE id_voluntario = ? AND ativo = 'S'";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, v);
            ps.setLong(5, v.getId());
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Volunt\u00e1rio n\u00e3o encontrado ou desativado.");
        } catch (SQLException e) {
            throw Dao.erro("atualizar o volunt\u00e1rio (telefone j\u00e1 cadastrado?)", e);
        }
    }

    // exclusao logica: o historico de ligacoes continua apontando para ele;
    // os vinculos saem, e o idoso aparece no relatorio de furos na rede
    @Override
    public void excluir(long id) {
        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement des = c.prepareStatement("UPDATE voluntario SET ativo = 'N' WHERE id_voluntario = ? AND ativo = 'S'");
                 PreparedStatement vinc = c.prepareStatement("DELETE FROM vinculo_cuidado WHERE id_voluntario = ?")) {
                des.setLong(1, id);
                if (des.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Volunt\u00e1rio n\u00e3o encontrado ou j\u00e1 desativado.");
                vinc.setLong(1, id);
                vinc.executeUpdate();
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Dao.erro("desativar o volunt\u00e1rio", e);
        }
    }

    private void preencher(PreparedStatement ps, Voluntario v) throws SQLException {
        ps.setLong(1, v.getIdRegiao());
        ps.setString(2, v.getNome().trim());
        ps.setString(3, v.getTelefone());
        ps.setString(4, v.getTipo());
    }

    private Voluntario montar(ResultSet rs) throws SQLException {
        Voluntario v = new Voluntario();
        v.setId(rs.getLong("id_voluntario"));
        v.setIdRegiao(rs.getLong("id_regiao"));
        v.setNome(rs.getString("nome"));
        v.setTelefone(rs.getString("telefone"));
        v.setTipo(rs.getString("tipo"));
        v.setAtivo(Dao.sn(rs.getString("ativo")));
        return v;
    }
}
