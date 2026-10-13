package persistencia;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.Conta;

/** Todas as operações exigem empresaId: contas nunca são visíveis fora da própria empresa. */
public class ContaDao {

    public List<Conta> listar(long empresaId) throws SQLException {
        List<Conta> l = new ArrayList<Conta>();
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, descricao, valor, vencimento, situacao FROM conta WHERE empresa_id = ? "
                     + "ORDER BY vencimento, id")) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conta x = new Conta();
                    x.id = rs.getLong("id");
                    x.descricao = rs.getString("descricao");
                    x.valor = rs.getBigDecimal("valor").toPlainString();
                    x.vencimento = rs.getDate("vencimento").toLocalDate().toString();
                    x.situacao = rs.getString("situacao");
                    l.add(x);
                }
            }
        }
        return l;
    }

    public void criar(long empresaId, String descricao, BigDecimal valor, LocalDate vencimento, String situacao)
            throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO conta (empresa_id, descricao, valor, vencimento, situacao) VALUES (?,?,?,?,?)")) {
            ps.setLong(1, empresaId);
            ps.setString(2, descricao);
            ps.setBigDecimal(3, valor);
            ps.setDate(4, Date.valueOf(vencimento));
            ps.setString(5, situacao);
            ps.executeUpdate();
        }
    }

    public boolean atualizar(long id, long empresaId, String descricao, BigDecimal valor, LocalDate vencimento,
            String situacao) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE conta SET descricao = ?, valor = ?, vencimento = ?, situacao = ? "
                     + "WHERE id = ? AND empresa_id = ?")) {
            ps.setString(1, descricao);
            ps.setBigDecimal(2, valor);
            ps.setDate(3, Date.valueOf(vencimento));
            ps.setString(4, situacao);
            ps.setLong(5, id);
            ps.setLong(6, empresaId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean excluir(long id, long empresaId) throws SQLException {
        try (Connection c = Db.conexao();
             PreparedStatement ps = c.prepareStatement("DELETE FROM conta WHERE id = ? AND empresa_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, empresaId);
            return ps.executeUpdate() > 0;
        }
    }
}
