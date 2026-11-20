package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Idoso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IdosoDao implements Dao<Idoso> {
    private static final String CAMPOS = "id_regiao, nome, ano_nascimento, telefone, telefone_fixo, endereco_ref, "
            + "contato_familiar, mora_sozinho, condicao_cronica, sem_ventilacao, mobilidade_reduzida, area_risco, "
            + "consentimento_em, consentido_por";

    @Override
    public Idoso inserir(Idoso i) {
        i.validar();
        String sql = "INSERT INTO idoso (" + CAMPOS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"ID_IDOSO"})) {
            preencher(ps, i);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                i.setId(rs.getLong(1));
            }
            return i;
        } catch (SQLException e) {
            throw Dao.erro("cadastrar o idoso", e);
        }
    }

    @Override
    public Idoso buscarPorId(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM idoso WHERE id_idoso = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecursoNaoEncontradoException("Idoso n\u00e3o encontrado.");
                return montar(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("buscar o idoso", e);
        }
    }

    // so ativos: os desativados ja foram anonimizados
    @Override
    public List<Idoso> listar() {
        List<Idoso> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM idoso WHERE ativo = 'S' ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(montar(rs));
            return lista;
        } catch (SQLException e) {
            throw Dao.erro("listar os idosos", e);
        }
    }

    @Override
    public void atualizar(Idoso i) {
        i.validar();
        String sql = "UPDATE idoso SET id_regiao = ?, nome = ?, ano_nascimento = ?, telefone = ?, telefone_fixo = ?, "
                + "endereco_ref = ?, contato_familiar = ?, mora_sozinho = ?, condicao_cronica = ?, sem_ventilacao = ?, "
                + "mobilidade_reduzida = ?, area_risco = ?, consentimento_em = ?, consentido_por = ? "
                + "WHERE id_idoso = ? AND ativo = 'S'";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, i);
            ps.setLong(15, i.getId());
            if (ps.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Idoso n\u00e3o encontrado ou desativado.");
        } catch (SQLException e) {
            throw Dao.erro("atualizar o idoso", e);
        }
    }

    // exclusao logica + anonimizacao (LGPD): apaga dados pessoais e vinculos,
    // mas mantem as checagens para as metricas. A FK impede o DELETE fisico.
    @Override
    public void excluir(long id) {
        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement anon = c.prepareStatement("UPDATE idoso SET ativo = 'N', nome = 'ANONIMIZADO-' || id_idoso, "
                         + "telefone = NULL, telefone_fixo = NULL, endereco_ref = NULL, contato_familiar = NULL, "
                         + "condicao_cronica = 'N', sem_ventilacao = 'N', mobilidade_reduzida = 'N', area_risco = 'N' "
                         + "WHERE id_idoso = ? AND ativo = 'S'");
                 PreparedStatement vinc = c.prepareStatement("DELETE FROM vinculo_cuidado WHERE id_idoso = ?")) {
                anon.setLong(1, id);
                if (anon.executeUpdate() == 0) throw new RecursoNaoEncontradoException("Idoso n\u00e3o encontrado ou j\u00e1 desativado.");
                vinc.setLong(1, id);
                vinc.executeUpdate();
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Dao.erro("desativar o idoso", e);
        }
    }

    private void preencher(PreparedStatement ps, Idoso i) throws SQLException {
        ps.setLong(1, i.getIdRegiao());
        ps.setString(2, i.getNome().trim());
        ps.setInt(3, i.getAnoNascimento());
        ps.setString(4, vazioParaNull(i.getTelefone()));
        ps.setString(5, vazioParaNull(i.getTelefoneFixo()));
        ps.setString(6, vazioParaNull(i.getEnderecoRef()));
        ps.setString(7, vazioParaNull(i.getContatoFamiliar()));
        ps.setString(8, Dao.sn(i.isMoraSozinho()));
        ps.setString(9, Dao.sn(i.isCondicaoCronica()));
        ps.setString(10, Dao.sn(i.isSemVentilacao()));
        ps.setString(11, Dao.sn(i.isMobilidadeReduzida()));
        ps.setString(12, Dao.sn(i.isAreaRisco()));
        ps.setDate(13, Dao.data(i.getConsentimentoEm()));
        ps.setString(14, i.getConsentidoPor());
    }

    private static String vazioParaNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    static Idoso montar(ResultSet rs) throws SQLException {
        Idoso i = new Idoso();
        i.setId(rs.getLong("id_idoso"));
        i.setIdRegiao(rs.getLong("id_regiao"));
        i.setNome(rs.getString("nome"));
        i.setAnoNascimento(rs.getInt("ano_nascimento"));
        i.setTelefone(rs.getString("telefone"));
        i.setTelefoneFixo(rs.getString("telefone_fixo"));
        i.setEnderecoRef(rs.getString("endereco_ref"));
        i.setContatoFamiliar(rs.getString("contato_familiar"));
        i.setMoraSozinho(Dao.sn(rs.getString("mora_sozinho")));
        i.setCondicaoCronica(Dao.sn(rs.getString("condicao_cronica")));
        i.setSemVentilacao(Dao.sn(rs.getString("sem_ventilacao")));
        i.setMobilidadeReduzida(Dao.sn(rs.getString("mobilidade_reduzida")));
        i.setAreaRisco(Dao.sn(rs.getString("area_risco")));
        i.setConsentimentoEm(rs.getDate("consentimento_em").toLocalDate());
        i.setConsentidoPor(rs.getString("consentido_por"));
        i.setAtivo(Dao.sn(rs.getString("ativo")));
        return i;
    }
}
