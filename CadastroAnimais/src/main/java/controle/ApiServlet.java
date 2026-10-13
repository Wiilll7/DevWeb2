package controle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import modelo.Sessao;
import util.Json;

/** Base dos servlets da API: respostas JSON e acesso ao contexto (empresa) da sessão. */
public abstract class ApiServlet extends HttpServlet {

    protected static final String ATTR_SESSAO = "sessao";

    protected Sessao sessao(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (Sessao) s.getAttribute(ATTR_SESSAO);
    }

    protected void json(HttpServletResponse resp, int status, Object corpo) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(Json.escrever(corpo));
    }

    protected void erro(HttpServletResponse resp, int status, String mensagem) throws IOException {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("erro", mensagem);
        json(resp, status, m);
    }

    protected void erroInterno(HttpServletResponse resp, SQLException e) throws IOException {
        log("Erro de banco de dados", e);
        erro(resp, 500, "Erro interno ao acessar o banco de dados.");
    }

    protected String param(HttpServletRequest req, String nome) {
        String v = req.getParameter(nome);
        return v == null ? "" : v.trim();
    }

    /** Lê um id numérico; retorna null se ausente/ inválido. */
    protected Long idParam(HttpServletRequest req, String nome) {
        try {
            return Long.valueOf(param(req, nome));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected void ok(HttpServletResponse resp) throws IOException {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("ok", true);
        json(resp, 200, m);
    }
}
