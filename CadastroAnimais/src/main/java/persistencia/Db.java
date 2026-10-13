package persistencia;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

/**
 * Acesso ao banco. Por padrão usa H2 em arquivo (~/devweb_multitenant.mv.db).
 * Para trocar de banco (MySQL, Postgres...), defina -Ddb.url, -Ddb.user e -Ddb.pass
 * (ou as variáveis de ambiente DB_URL, DB_USER e DB_PASS) e adicione o driver no pom.xml.
 */
public final class Db {

    private Db() { }

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String config(String prop, String env, String padrao) {
        String v = System.getProperty(prop);
        if (v == null) v = System.getenv(env);
        return v == null ? padrao : v;
    }

    public static Connection conexao() throws SQLException {
        return DriverManager.getConnection(
                config("db.url", "DB_URL", "jdbc:h2:~/devweb_multitenant;AUTO_SERVER=TRUE"),
                config("db.user", "DB_USER", "sa"),
                config("db.pass", "DB_PASS", ""));
    }

    /** Cria as tabelas (schema.sql) se ainda não existirem. */
    public static void criarEsquema() throws Exception {
        StringBuilder sql = new StringBuilder();
        try (InputStream in = Db.class.getResourceAsStream("/schema.sql");
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = r.readLine()) != null) {
                if (!linha.trim().startsWith("--")) sql.append(linha).append('\n');
            }
        }
        try (Connection c = conexao(); Statement st = c.createStatement()) {
            for (String cmd : sql.toString().split(";")) {
                if (!cmd.trim().isEmpty()) st.execute(cmd);
            }
        }
    }

    /** Gera "?,?,?" para cláusulas IN. */
    static String marcadores(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(i == 0 ? "?" : ",?");
        return sb.toString();
    }

    static void definirIds(java.sql.PreparedStatement ps, int inicio, Collection<Long> ids) throws SQLException {
        int i = inicio;
        for (Long id : ids) ps.setLong(i++, id);
    }
}
