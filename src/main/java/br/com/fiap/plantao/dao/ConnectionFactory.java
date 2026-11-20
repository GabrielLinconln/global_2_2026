package br.com.fiap.plantao.dao;

import br.com.fiap.plantao.exception.PersistenciaException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Padrao: H2 em arquivo no modo Oracle (roda sem instalar nada).
// Oracle FIAP: rodar sql/script.sql no banco, colocar o ojdbc no classpath e iniciar com
// -Ddb.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL -Ddb.user=RMxxxxx -Ddb.pass=...
public final class ConnectionFactory {
    private static String url = System.getProperty("db.url", "jdbc:h2:./data/plantao;MODE=Oracle;AUTO_SERVER=TRUE");
    private static final String USER = System.getProperty("db.user", "sa");
    private static final String PASS = System.getProperty("db.pass", "");

    private ConnectionFactory() { }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, USER, PASS);
        } catch (SQLException e) {
            throw new PersistenciaException("N\u00e3o foi poss\u00edvel conectar ao banco: " + e.getMessage(), e);
        }
    }

    // usado pela Verificacao para rodar num banco em memoria
    public static void usarUrl(String novaUrl) {
        url = novaUrl;
    }

    // no H2, cria as tabelas e o seed na primeira execucao
    public static void inicializar() {
        if (!url.startsWith("jdbc:h2:")) return;
        try (Connection c = getConnection();
             ResultSet rs = c.getMetaData().getTables(null, null, "REGIAO", null)) {
            if (!rs.next()) {
                try (Statement st = c.createStatement()) {
                    st.execute("RUNSCRIPT FROM 'sql/script.sql'");
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao criar o banco a partir de sql/script.sql", e);
        }
    }
}
