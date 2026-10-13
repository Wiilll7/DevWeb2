package controle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import modelo.Usuario;
import persistencia.Db;
import persistencia.UsuarioDao;

/**
 * Na subida da aplicação cria as tabelas e, se o banco estiver vazio, carrega dados de DEMONSTRAÇÃO.
 * Em produção, apague este bloco de dados e cadastre empresas direto no banco (tabela "empresa").
 */
@WebListener
public class Inicializador implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Db.criarEsquema();
            try (Connection c = Db.conexao(); Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM empresa")) {
                rs.next();
                if (rs.getLong(1) == 0) carregarDemonstracao(c);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar o banco de dados", e);
        }
    }

    private void carregarDemonstracao(Connection c) throws Exception {
        // Empresas e conflitos de interesse (campo "conflitos" = ids separados por vírgula)
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO empresa (id, nome, conflitos) VALUES (?,?,?)")) {
            Object[][] dados = {
                {1, "Banco Alfa", "2"},        // concorrente de Banco Beta
                {2, "Banco Beta", "1"},
                {3, "Tech Gama", ""},          // sem conflitos
                {4, "Petro Delta", "5"},       // concorrente de Petro Épsilon
                {5, "Petro Épsilon", "4"}
            };
            for (Object[] d : dados) {
                ps.setLong(1, ((Integer) d[0]).longValue());
                ps.setString(2, (String) d[1]);
                ps.setString(3, (String) d[2]);
                ps.executeUpdate();
            }
        }

        // Todos com a senha "123456"
        UsuarioDao u = new UsuarioDao();
        long admin = u.criar("admin", "Administrador Geral", "123456", mapa(1, Usuario.ADMIN, 3, Usuario.ADMIN, 4, Usuario.ADMIN));
        long ana = u.criar("ana", "Ana (admin Beta)", "123456", mapa(2, Usuario.ADMIN));
        long joao = u.criar("joao", "João", "123456", mapa(1, Usuario.COMUM));
        long maria = u.criar("maria", "Maria", "123456", mapa(2, Usuario.COMUM, 3, Usuario.COMUM));
        u.criar("carlos", "Carlos (admin Épsilon)", "123456", mapa(5, Usuario.ADMIN));

        try (PreparedStatement n = c.prepareStatement(
                "INSERT INTO noticia (empresa_id, autor_id, titulo, texto) VALUES (?,?,?,?)")) {
            noticia(n, 1, admin, "Banco Alfa anuncia nova linha de crédito", "A nova linha terá juros reduzidos para pequenas empresas.");
            noticia(n, 2, ana, "Banco Beta abre 20 novas agências", "A expansão cria 300 vagas de emprego em todo o país.");
            noticia(n, 3, maria, "Tech Gama lança plataforma de nuvem", "O serviço promete reduzir custos de infraestrutura em até 30%.");
            noticia(n, 4, admin, "Petro Delta amplia produção", "A empresa aumentou em 12% a produção no último trimestre.");
        }
        try (PreparedStatement k = c.prepareStatement(
                "INSERT INTO conta (empresa_id, descricao, valor, vencimento, situacao) VALUES (?,?,?,?,?)")) {
            conta(k, 1, "Aluguel da sede", "12500.00", "2026-11-05", "PENDENTE");
            conta(k, 1, "Energia elétrica", "3200.50", "2026-10-20", "PAGA");
            conta(k, 2, "Folha de pagamento", "98000.00", "2026-11-01", "PENDENTE");
            conta(k, 3, "Servidores em nuvem", "7450.90", "2026-11-10", "PENDENTE");
        }
    }

    private Map<Long, String> mapa(Object... kv) {
        Map<Long, String> m = new LinkedHashMap<Long, String>();
        for (int i = 0; i < kv.length; i += 2) m.put(((Integer) kv[i]).longValue(), (String) kv[i + 1]);
        return m;
    }

    private void noticia(PreparedStatement ps, long empresa, long autor, String titulo, String texto) throws Exception {
        ps.setLong(1, empresa);
        ps.setLong(2, autor);
        ps.setString(3, titulo);
        ps.setString(4, texto);
        ps.executeUpdate();
    }

    private void conta(PreparedStatement ps, long empresa, String desc, String valor, String venc, String sit) throws Exception {
        ps.setLong(1, empresa);
        ps.setString(2, desc);
        ps.setBigDecimal(3, new java.math.BigDecimal(valor));
        ps.setDate(4, java.sql.Date.valueOf(venc));
        ps.setString(5, sit);
        ps.executeUpdate();
    }
}
