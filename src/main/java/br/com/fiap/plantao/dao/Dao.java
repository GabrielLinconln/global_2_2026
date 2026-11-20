package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.PersistenciaException;
import br.com.fiap.plantao.exception.RegraNegocioException;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// CRUD padrao das entidades com id proprio
public interface Dao<T> {
    T inserir(T t);
    T buscarPorId(long id);
    List<T> listar();
    void atualizar(T t);
    void excluir(long id);

    static String sn(boolean b) {
        return b ? "S" : "N";
    }

    static boolean sn(String s) {
        return "S".equals(s);
    }

    static Date data(LocalDate d) {
        return d == null ? null : Date.valueOf(d);
    }

    // SQLState 23xxx = violacao de constraint (H2 e Oracle): e erro do usuario, nao do sistema
    static RuntimeException erro(String acao, SQLException e) {
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            return new RegraNegocioException("N\u00e3o foi poss\u00edvel " + acao + ": dado duplicado ou ligado a outro cadastro.");
        }
        return new PersistenciaException("Erro no banco ao " + acao + ": " + e.getMessage(), e);
    }

    // linhas de relatorio como mapas coluna -> valor (para virar JSON direto)
    static List<Map<String, Object>> linhas(ResultSet rs) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                Object v = rs.getObject(i);
                if (v instanceof Timestamp) v = ((Timestamp) v).toLocalDateTime();
                if (v instanceof Date) v = ((Date) v).toLocalDate();
                linha.put(md.getColumnLabel(i).toLowerCase(), v);
            }
            lista.add(linha);
        }
        return lista;
    }
}
