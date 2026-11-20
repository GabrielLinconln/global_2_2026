package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.RecursoNaoEncontradoException;
import br.com.fiap.plantao.exception.RegraNegocioException;
import br.com.fiap.plantao.model.Checagem;
import br.com.fiap.plantao.model.Resultado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// metodos que recebem Connection participam da transacao aberta pelo service
public class ChecagemDao {
    private static final String SELECT_RODADA = "SELECT c.id_checagem, c.id_alerta, c.id_voluntario, c.tentativa, c.resultado, c.canal, "
            + "c.data_hora, c.observacao, c.registrado_depois, v.nome AS nome_voluntario, i.* "
            + "FROM checagem c JOIN idoso i ON i.id_idoso = c.id_idoso JOIN voluntario v ON v.id_voluntario = c.id_voluntario ";

    // uma ligacao pendente (tentativa 1) para o titular de cada idoso ativo da regiao
    public int criarRodada(Connection c, long idAlerta, long idRegiao) throws SQLException {
        String sql = "INSERT INTO checagem (id_alerta, id_idoso, id_voluntario, tentativa) "
                + "SELECT ?, vc.id_idoso, vc.id_voluntario, 1 FROM vinculo_cuidado vc "
                + "JOIN idoso i ON i.id_idoso = vc.id_idoso JOIN voluntario v ON v.id_voluntario = vc.id_voluntario "
                + "WHERE vc.papel = 'TITULAR' AND i.ativo = 'S' AND v.ativo = 'S' AND i.id_regiao = ? "
                + "AND NOT EXISTS (SELECT 1 FROM checagem x WHERE x.id_alerta = ? AND x.id_idoso = vc.id_idoso)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idAlerta);
            ps.setLong(2, idRegiao);
            ps.setLong(3, idAlerta);
            return ps.executeUpdate();
        }
    }

    public int criarTentativaReserva(Connection c, long idAlerta, long idIdoso, long idVoluntario) throws SQLException {
        String sql = "INSERT INTO checagem (id_alerta, id_idoso, id_voluntario, tentativa) SELECT ?, ?, ?, 2 FROM dual "
                + "WHERE NOT EXISTS (SELECT 1 FROM checagem x WHERE x.id_alerta = ? AND x.id_idoso = ? AND x.tentativa = 2)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idAlerta);
            ps.setLong(2, idIdoso);
            ps.setLong(3, idVoluntario);
            ps.setLong(4, idAlerta);
            ps.setLong(5, idIdoso);
            return ps.executeUpdate();
        }
    }

    public void registrar(Connection c, Checagem ch) throws SQLException {
        String sql = "UPDATE checagem SET resultado = ?, canal = ?, data_hora = CURRENT_TIMESTAMP, observacao = ?, "
                + "registrado_depois = ? WHERE id_checagem = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ch.getResultado().name());
            ps.setString(2, ch.getCanal());
            ps.setString(3, ch.getObservacao());
            ps.setString(4, Dao.sn(ch.isRegistradoDepois()));
            ps.setLong(5, ch.getId());
            ps.executeUpdate();
        }
    }

    public Checagem buscarPorId(long id) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_RODADA + "WHERE c.id_checagem = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RecursoNaoEncontradoException("Liga\u00e7\u00e3o n\u00e3o encontrada.");
                return montar(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("buscar a liga\u00e7\u00e3o", e);
        }
    }

    public List<Checagem> listarPorAlerta(long idAlerta) {
        List<Checagem> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_RODADA + "WHERE c.id_alerta = ? ORDER BY c.id_idoso, c.tentativa")) {
            ps.setLong(1, idAlerta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(montar(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw Dao.erro("listar as liga\u00e7\u00f5es", e);
        }
    }

    // critico: alguem nao atendeu, nada pendente e ninguem conseguiu contato
    // (cobre o idoso sem reserva: o titular nao atendeu e ja vira critico)
    public List<Map<String, Object>> listarCriticos(long idAlerta) {
        String sql = "SELECT i.id_idoso, i.nome, i.telefone, i.telefone_fixo, i.endereco_ref, i.contato_familiar, "
                + "MAX(c.data_hora) AS ultima_tentativa, MAX(c.id_checagem) AS id_checagem FROM checagem c JOIN idoso i ON i.id_idoso = c.id_idoso "
                + "WHERE c.id_alerta = ? AND c.resultado = 'NAO_ATENDEU' "
                + "AND NOT EXISTS (SELECT 1 FROM checagem ok WHERE ok.id_alerta = c.id_alerta AND ok.id_idoso = c.id_idoso "
                + "AND ok.resultado IN ('OK','ATENCAO','URGENTE','PENDENTE')) "
                + "GROUP BY i.id_idoso, i.nome, i.telefone, i.telefone_fixo, i.endereco_ref, i.contato_familiar "
                + "ORDER BY ultima_tentativa";
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idAlerta);
            try (ResultSet rs = ps.executeQuery()) {
                return Dao.linhas(rs);
            }
        } catch (SQLException e) {
            throw Dao.erro("listar os cr\u00edticos", e);
        }
    }

    private Checagem montar(ResultSet rs) throws SQLException {
        Checagem ch = new Checagem();
        ch.setId(rs.getLong("id_checagem"));
        ch.setIdAlerta(rs.getLong("id_alerta"));
        ch.setIdIdoso(rs.getLong("id_idoso"));
        ch.setIdVoluntario(rs.getLong("id_voluntario"));
        ch.setTentativa(rs.getInt("tentativa"));
        ch.setResultado(Resultado.valueOf(rs.getString("resultado")));
        ch.setCanal(rs.getString("canal"));
        Timestamp t = rs.getTimestamp("data_hora");
        ch.setDataHora(t == null ? null : t.toLocalDateTime());
        ch.setObservacao(rs.getString("observacao"));
        ch.setRegistradoDepois(Dao.sn(rs.getString("registrado_depois")));
        ch.setNomeVoluntario(rs.getString("nome_voluntario"));
        ch.setIdoso(IdosoDao.montar(rs));
        return ch;
    }
}
