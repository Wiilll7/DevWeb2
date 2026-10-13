package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import modelo.Usuario;
import util.Senha;

public class UsuarioDao {

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(rs.getLong("id"), rs.getString("login"), rs.getString("nome"), rs.getString("senha_hash"));
    }

    public Usuario buscarPorLogin(String login) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM usuario WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Retorna o usuário se login e senha conferem; senão null. Sempre gasta o mesmo tempo de hash. */
    public Usuario autenticar(String login, String senha) throws SQLException {
        Usuario u = buscarPorLogin(login);
        boolean ok = Senha.conferir(senha, u == null ? null : u.getSenhaHash());
        return (u != null && ok) ? u : null;
    }

    /** Perfil do usuário na empresa (COMUM/ADMIN) ou null se não pertence a ela. */
    public String perfil(long usuarioId, long empresaId) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT perfil FROM usuario_empresa WHERE usuario_id = ? AND empresa_id = ?")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** empresaId -> perfil de todos os vínculos do usuário. */
    public Map<Long, String> vinculos(long usuarioId) throws SQLException {
        Map<Long, String> m = new LinkedHashMap<Long, String>();
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT empresa_id, perfil FROM usuario_empresa WHERE usuario_id = ? ORDER BY empresa_id")) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getLong(1), rs.getString(2));
            }
        }
        return m;
    }

    /** Empresas que o usuário administra (perfil ADMIN). */
    public Set<Long> empresasAdministradas(long usuarioId) throws SQLException {
        Set<Long> ids = new LinkedHashSet<Long>();
        for (Map.Entry<Long, String> e : vinculos(usuarioId).entrySet()) {
            if (Usuario.ADMIN.equals(e.getValue())) ids.add(e.getKey());
        }
        return ids;
    }

    /** Usuários com algum vínculo nas empresas do escopo (exceto o próprio administrador), com TODOS os vínculos. */
    public List<Usuario> listarDoEscopo(Collection<Long> escopo, long exceto) throws SQLException {
        List<Usuario> l = new ArrayList<Usuario>();
        if (escopo.isEmpty()) return l;
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT DISTINCT u.* FROM usuario u JOIN usuario_empresa ue ON ue.usuario_id = u.id "
                     + "WHERE ue.empresa_id IN (" + Db.marcadores(escopo.size()) + ") AND u.id <> ? ORDER BY u.nome")) {
            Db.definirIds(ps, 1, escopo);
            ps.setLong(escopo.size() + 1, exceto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) l.add(mapear(rs));
            }
        }
        for (Usuario u : l) u.getVinculos().putAll(vinculos(u.getId()));
        return l;
    }

    /** Cria o usuário e seus vínculos na mesma transação. */
    public long criar(String login, String nome, String senha, Map<Long, String> vinculos) throws SQLException {
        try (Connection c = Db.conexao()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO usuario (login, nome, senha_hash) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, login);
                    ps.setString(2, nome);
                    ps.setString(3, Senha.gerar(senha));
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        id = k.getLong(1);
                    }
                }
                inserirVinculos(c, id, vinculos);
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Substitui os vínculos do usuário DENTRO do escopo (os de fora do escopo não são tocados).
     * Se nome/senha forem não-nulos, também são atualizados. Se o usuário ficar sem nenhuma empresa, é excluído.
     */
    public void atualizar(long usuarioId, String nome, String senha, Collection<Long> escopo,
            Map<Long, String> novosVinculos) throws SQLException {
        try (Connection c = Db.conexao()) {
            c.setAutoCommit(false);
            try {
                if (!escopo.isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM usuario_empresa WHERE usuario_id = ? AND empresa_id IN ("
                            + Db.marcadores(escopo.size()) + ")")) {
                        ps.setLong(1, usuarioId);
                        Db.definirIds(ps, 2, escopo);
                        ps.executeUpdate();
                    }
                }
                inserirVinculos(c, usuarioId, novosVinculos);
                if (nome != null) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET nome = ? WHERE id = ?")) {
                        ps.setString(1, nome);
                        ps.setLong(2, usuarioId);
                        ps.executeUpdate();
                    }
                }
                if (senha != null) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET senha_hash = ? WHERE id = ?")) {
                        ps.setString(1, Senha.gerar(senha));
                        ps.setLong(2, usuarioId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM usuario WHERE id = ? AND NOT EXISTS "
                        + "(SELECT 1 FROM usuario_empresa WHERE usuario_id = ?) "
                        + "AND NOT EXISTS (SELECT 1 FROM noticia WHERE autor_id = ?)")) {
                    ps.setLong(1, usuarioId);
                    ps.setLong(2, usuarioId);
                    ps.setLong(3, usuarioId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void inserirVinculos(Connection c, long usuarioId, Map<Long, String> vinculos) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO usuario_empresa (usuario_id, empresa_id, perfil) VALUES (?,?,?)")) {
            for (Map.Entry<Long, String> e : vinculos.entrySet()) {
                ps.setLong(1, usuarioId);
                ps.setLong(2, e.getKey());
                ps.setString(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
