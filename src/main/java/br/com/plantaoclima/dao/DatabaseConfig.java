package br.com.plantaoclima.dao;

import br.com.plantaoclima.exception.PersistenciaException;
import org.h2.tools.RunScript;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerencia a conexao JDBC com o banco H2 em modo Oracle e inicializa o schema/carga inicial.
 */
public final class DatabaseConfig {

    private static final String DEFAULT_JDBC_URL =
            "jdbc:h2:file:./data/plantaoclima;MODE=Oracle;DATABASE_TO_UPPER=TRUE;AUTO_SERVER=FALSE";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "";

    private static String jdbcUrl = System.getProperty("plantaoclima.db.url", DEFAULT_JDBC_URL);
    private static String user = System.getProperty("plantaoclima.db.user", DEFAULT_USER);
    private static String password = System.getProperty("plantaoclima.db.password", DEFAULT_PASSWORD);

    private DatabaseConfig() {
    }

    public static void configurar(String novaUrl, String novoUsuario, String novaSenha) {
        jdbcUrl = novaUrl;
        user = novoUsuario;
        password = novaSenha;
    }

    public static String getJdbcUrl() {
        return jdbcUrl;
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl, user, password);
        } catch (SQLException e) {
            throw new PersistenciaException("Nao foi possivel conectar ao banco de dados H2: " + e.getMessage(), e);
        }
    }

    /**
     * Garante que as tabelas e a carga inicial existam, executando sql/plantao_clima.sql se necessario.
     */
    public static void inicializarBanco(Path caminhoScriptSql) {
        try (Connection conn = getConnection()) {
            if (!tabelaExiste(conn, "REGIAO")) {
                if (!Files.exists(caminhoScriptSql)) {
                    throw new PersistenciaException(
                            "Script SQL nao encontrado em: " + caminhoScriptSql.toAbsolutePath(), null);
                }
                try (FileReader reader = new FileReader(caminhoScriptSql.toFile(), StandardCharsets.UTF_8)) {
                    RunScript.execute(conn, reader);
                }
            }
        } catch (SQLException | IOException e) {
            throw new PersistenciaException("Falha ao inicializar o banco de dados: " + e.getMessage(), e);
        }
    }

    private static boolean tabelaExiste(Connection conn, String nomeTabela) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + nomeTabela.toUpperCase() + "'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public static PersistenciaException traduzirErroSql(String operacao, SQLException e) {
        String state = e.getSQLState() != null ? e.getSQLState() : "";
        if (state.startsWith("23")) {
            String msg = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
            if (msg.contains("UK_VOLUNTARIO_TEL")) {
                return new PersistenciaException("Ja existe um voluntario cadastrado com este telefone.", e, true);
            }
            if (msg.contains("UK_VINCULO_IDOSO_PAP")) {
                return new PersistenciaException("Este idoso ja possui um voluntario cadastrado para esse papel (TITULAR ou RESERVA).", e, true);
            }
            if (msg.contains("UK_VINCULO_IDOSO_VOL")) {
                return new PersistenciaException("Este voluntario ja esta vinculado a este idoso.", e, true);
            }
            if (msg.contains("UK_REGIAO_NOME")) {
                return new PersistenciaException("Ja existe uma regiao cadastrada com este nome.", e, true);
            }
            if (msg.contains("FK_")) {
                return new PersistenciaException("Nao foi possivel concluir " + operacao + ": registro referenciado nao existe ou possui vinculos ativos.", e, true);
            }
            if (msg.contains("CK_")) {
                return new PersistenciaException("Dados fora das regras de validacao do banco ao " + operacao + ".", e, true);
            }
            return new PersistenciaException("Conflito de integridade ao " + operacao + ".", e, true);
        }
        return new PersistenciaException("Erro de banco de dados ao " + operacao + ": " + e.getMessage(), e, false);
    }
}
