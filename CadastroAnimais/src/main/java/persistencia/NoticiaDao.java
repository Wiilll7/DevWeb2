package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelo.Noticia;

public class NoticiaDao {

    /** Lista pública. Se empresaId != null, filtra pela empresa. */
    public List<Noticia> listar(Long empresaId) throws SQLException {
        String sql = "SELECT n.id, n.empresa_id, e.nome AS empresa, u.nome AS autor, n.titulo, n.texto, n.data_publicacao "
                + "FROM noticia n JOIN empresa e ON e.id = n.empresa_id JOIN usuario u ON u.id = n.autor_id "
                + (empresaId != null ? "WHERE n.empresa_id = ? " : "")
                + "ORDER BY n.data_publicacao DESC, n.id DESC";
        List<Noticia> l = new ArrayList<Noticia>();
        try (Connection c = Db.conexao(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (empresaId != null) ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Noticia n = new Noticia();
                    n.id = rs.getLong("id");
                    n.empresaId = rs.getLong("empresa_id");
                    n.empresaNome = rs.getString("empresa");
                    n.autor = rs.getString("autor");
                    n.titulo = rs.getString("titulo");
                    n.texto = rs.getString("texto");
                    n.data = rs.getTimestamp("data_publicacao").toLocalDateTime().toString();
                    l.add(n);
                }
            }
        }
        return l;
    }

    public void criar(long empresaId, long autorId, String titulo, String texto) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO noticia (empresa_id, autor_id, titulo, texto) VALUES (?,?,?,?)")) {
            ps.setLong(1, empresaId);
            ps.setLong(2, autorId);
            ps.setString(3, titulo);
            ps.setString(4, texto);
            ps.executeUpdate();
        }
    }

    /** O filtro por empresa_id garante o isolamento: não altera notícia de outra empresa. */
    public boolean atualizar(long id, long empresaId, String titulo, String texto) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE noticia SET titulo = ?, texto = ? WHERE id = ? AND empresa_id = ?")) {
            ps.setString(1, titulo);
            ps.setString(2, texto);
            ps.setLong(3, id);
            ps.setLong(4, empresaId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean excluir(long id, long empresaId) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement("DELETE FROM noticia WHERE id = ? AND empresa_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, empresaId);
            return ps.executeUpdate() > 0;
        }
    }
}
