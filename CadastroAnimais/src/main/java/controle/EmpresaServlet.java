package controle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import modelo.Empresa;
import persistencia.EmpresaDao;

/** GET /api/empresas (público): só id e nome, para a tela de login e filtro de notícias. Conflitos NÃO são expostos. */
@WebServlet("/api/empresas")
public class EmpresaServlet extends ApiServlet {

    private final EmpresaDao empresas = new EmpresaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Object> saida = new ArrayList<Object>();
            for (Empresa e : empresas.listar()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("id", e.getId());
                m.put("nome", e.getNome());
                saida.add(m);
            }
            json(resp, 200, saida);
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }
}
