package br.com.plantaoclima.dao;

import br.com.plantaoclima.model.Idoso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IdosoDao implements CrudDao<Idoso> {

    private static final String BASE_SELECT = """
            SELECT i.ID_IDOSO, i.NOME, i.TELEFONE, i.ANO_NASCIMENTO, i.ENDERECO_RESUMO,
                   i.DOENCA_CRONICA, i.MOBILIDADE_REDUZIDA, i.CASA_SEM_VENTILACAO, i.AREA_RISCO,
                   i.CONSENTIMENTO_LGPD, i.DATA_CONSENTIMENTO, i.OBSERVACOES,
                   i.ID_REGIAO, r.NOME AS NOME_REGIAO, i.ATIVO, i.CRIADO_EM
              FROM IDOSO i
              JOIN REGIAO r ON r.ID_REGIAO = i.ID_REGIAO
            """;

    @Override
    public Idoso inserir(Idoso idoso) {
        String sql = """
                INSERT INTO IDOSO (
                    NOME, TELEFONE, ANO_NASCIMENTO, ENDERECO_RESUMO,
                    DOENCA_CRONICA, MOBILIDADE_REDUZIDA, CASA_SEM_VENTILACAO, AREA_RISCO,
                    CONSENTIMENTO_LGPD, OBSERVACOES, ID_REGIAO, ATIVO
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_IDOSO"})) {
            ps.setString(1, idoso.getNome());
            ps.setString(2, idoso.getTelefone());
            ps.setInt(3, idoso.getAnoNascimento());
            ps.setString(4, idoso.getEnderecoResumo());
            ps.setInt(5, idoso.isDoencaCronica() ? 1 : 0);
            ps.setInt(6, idoso.isMobilidadeReduzida() ? 1 : 0);
            ps.setInt(7, idoso.isCasaSemVentilacao() ? 1 : 0);
            ps.setInt(8, idoso.isAreaRisco() ? 1 : 0);
            ps.setInt(9, idoso.isConsentimentoLgpd() ? 1 : 0);
            ps.setString(10, idoso.getObservacoes());
            ps.setLong(11, idoso.getIdRegiao());
            ps.setInt(12, idoso.isAtivo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idoso.setId(rs.getLong(1));
                }
            }
            return buscarPorId(idoso.getId()).orElse(idoso);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("inserir idoso", e);
        }
    }

    @Override
    public List<Idoso> listarTodos() {
        String sql = BASE_SELECT + " ORDER BY i.ATIVO DESC, i.NOME";
        List<Idoso> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar idosos", e);
        }
    }

    public List<Idoso> listarAtivosPorRegiao(Long idRegiao) {
        String sql = idRegiao == null
                ? BASE_SELECT + " WHERE i.ATIVO = 1 ORDER BY i.NOME"
                : BASE_SELECT + " WHERE i.ATIVO = 1 AND i.ID_REGIAO = ? ORDER BY i.NOME";
        List<Idoso> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idRegiao != null) {
                ps.setLong(1, idRegiao);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("listar idosos por regiao", e);
        }
    }

    @Override
    public Optional<Idoso> buscarPorId(Long id) {
        String sql = BASE_SELECT + " WHERE i.ID_IDOSO = ?";
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
            throw DatabaseConfig.traduzirErroSql("buscar idoso", e);
        }
    }

    @Override
    public Idoso atualizar(Idoso idoso) {
        String sql = """
                UPDATE IDOSO
                   SET NOME = ?, TELEFONE = ?, ANO_NASCIMENTO = ?, ENDERECO_RESUMO = ?,
                       DOENCA_CRONICA = ?, MOBILIDADE_REDUZIDA = ?, CASA_SEM_VENTILACAO = ?,
                       AREA_RISCO = ?, CONSENTIMENTO_LGPD = ?, OBSERVACOES = ?, ID_REGIAO = ?, ATIVO = ?
                 WHERE ID_IDOSO = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idoso.getNome());
            ps.setString(2, idoso.getTelefone());
            ps.setInt(3, idoso.getAnoNascimento());
            ps.setString(4, idoso.getEnderecoResumo());
            ps.setInt(5, idoso.isDoencaCronica() ? 1 : 0);
            ps.setInt(6, idoso.isMobilidadeReduzida() ? 1 : 0);
            ps.setInt(7, idoso.isCasaSemVentilacao() ? 1 : 0);
            ps.setInt(8, idoso.isAreaRisco() ? 1 : 0);
            ps.setInt(9, idoso.isConsentimentoLgpd() ? 1 : 0);
            ps.setString(10, idoso.getObservacoes());
            ps.setLong(11, idoso.getIdRegiao());
            ps.setInt(12, idoso.isAtivo() ? 1 : 0);
            ps.setLong(13, idoso.getId());
            ps.executeUpdate();
            return buscarPorId(idoso.getId()).orElse(idoso);
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("atualizar idoso", e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "DELETE FROM IDOSO WHERE ID_IDOSO = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseConfig.traduzirErroSql("excluir idoso", e);
        }
    }

    private Idoso mapear(ResultSet rs) throws SQLException {
        Idoso i = new Idoso();
        i.setId(rs.getLong("ID_IDOSO"));
        i.setNome(rs.getString("NOME"));
        i.setTelefone(rs.getString("TELEFONE"));
        i.setAnoNascimento(rs.getInt("ANO_NASCIMENTO"));
        i.setEnderecoResumo(rs.getString("ENDERECO_RESUMO"));
        i.setDoencaCronica(rs.getInt("DOENCA_CRONICA") == 1);
        i.setMobilidadeReduzida(rs.getInt("MOBILIDADE_REDUZIDA") == 1);
        i.setCasaSemVentilacao(rs.getInt("CASA_SEM_VENTILACAO") == 1);
        i.setAreaRisco(rs.getInt("AREA_RISCO") == 1);
        i.setConsentimentoLgpd(rs.getInt("CONSENTIMENTO_LGPD") == 1);
        Timestamp dtCons = rs.getTimestamp("DATA_CONSENTIMENTO");
        if (dtCons != null) {
            i.setDataConsentimento(dtCons.toLocalDateTime());
        }
        i.setObservacoes(rs.getString("OBSERVACOES"));
        i.setIdRegiao(rs.getLong("ID_REGIAO"));
        i.setNomeRegiao(rs.getString("NOME_REGIAO"));
        i.setAtivo(rs.getInt("ATIVO") == 1);
        Timestamp criado = rs.getTimestamp("CRIADO_EM");
        if (criado != null) {
            i.setCriadoEm(criado.toLocalDateTime());
        }
        return i;
    }
}
