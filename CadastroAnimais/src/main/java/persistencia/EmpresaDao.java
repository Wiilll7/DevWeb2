package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import modelo.Empresa;

/** Somente leitura: o cadastro de empresas é feito diretamente no banco. */
public class EmpresaDao {

    private static final String COLS = "id, nome, conflitos";

    private Empresa mapear(ResultSet rs) throws SQLException {
        return new Empresa(rs.getLong("id"), rs.getString("nome"), rs.getString("conflitos"));
    }

    public List<Empresa> listar() throws SQLException {
        List<Empresa> l = new ArrayList<Empresa>();
        try (Connection c = Db.conexao(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT " + COLS + " FROM empresa ORDER BY nome")) {
            while (rs.next()) l.add(mapear(rs));
        }
        return l;
    }

    public Empresa buscar(long id) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM empresa WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Empresa> buscar(Collection<Long> ids) throws SQLException {
        List<Empresa> l = new ArrayList<Empresa>();
        if (ids.isEmpty()) return l;
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM empresa WHERE id IN ("
                     + Db.marcadores(ids.size()) + ") ORDER BY nome")) {
            Db.definirIds(ps, 1, ids);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) l.add(mapear(rs));
            }
        }
        return l;
    }
}
