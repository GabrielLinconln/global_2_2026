package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Vinculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// tabela associativa N:N (chave composta idoso + voluntario), por isso nao usa Dao<T>
public class VinculoDao {

    public void inserir(Vinculo v) {
        v.validar();
        String sql = "INSERT INTO vinculo_cuidado (id_idoso, id_voluntario, papel, desde) VALUES (?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, v.getIdIdoso());
            ps.setLong(2, v.getIdVoluntario());
            ps.setString(3, v.getPapel());
            ps.setDate(4, Dao.data(v.getDesde()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Dao.erro("criar o v\u00ednculo (o idoso j\u00e1 tem " + v.getPapel() + " ou j\u00e1 est\u00e1 ligado a esse volunt\u00e1rio)", e);
        }
    }

    // com nomes, para a tela de vinculos
    public List<Map<String, Object>> listar() {
        String sql = "SELECT vc.id_idoso, i.nome AS idoso, vc.id_voluntario, v.nome AS voluntario, v.telefone AS telefone_voluntario, "
                + "vc.papel, vc.desde FROM vinculo_cuidado vc "
                + "JOIN idoso i ON i.id_idoso = vc.id_idoso JOIN voluntario v ON v.id_voluntario = vc.id_voluntario "
                + "ORDER BY i.nome, vc.papel DESC";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return Dao.linhas(rs);
        } catch (SQLException e) {
            throw Dao.erro("listar os v\u00ednculos", e);
        }
    }

    public void atualizarPapel(Vinculo v) {
        v.validar();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE vinculo_cuidado SET papel = ? WHERE id_idoso = ? AND id_voluntario = ?")) {
            ps.setString(1, v.getPapel());
            ps.setLong(2, v.getIdIdoso());
            ps.setLong(3, v.getIdVoluntario());
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("V\u00ednculo n\u00e3o encontrado.");
        } catch (SQLException e) {
            throw Dao.erro("trocar o papel (o idoso j\u00e1 tem algu\u00e9m nesse papel)", e);
        }
    }

    public void excluir(long idIdoso, long idVoluntario) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM vinculo_cuidado WHERE id_idoso = ? AND id_voluntario = ?")) {
            ps.setLong(1, idIdoso);
            ps.setLong(2, idVoluntario);
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("V\u00ednculo n\u00e3o encontrado.");
        } catch (SQLException e) {
            throw Dao.erro("excluir o v\u00ednculo", e);
        }
    }

    // voluntario ativo no papel pedido, ou null se o idoso nao tem ninguem nesse papel
    public Long voluntarioNoPapel(Connection c, long idIdoso, String papel) throws SQLException {
        String sql = "SELECT vc.id_voluntario FROM vinculo_cuidado vc JOIN voluntario v ON v.id_voluntario = vc.id_voluntario "
                + "WHERE vc.id_idoso = ? AND vc.papel = ? AND v.ativo = 'S'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idIdoso);
            ps.setString(2, papel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }
}
