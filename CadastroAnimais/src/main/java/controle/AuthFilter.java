package controle;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import modelo.Sessao;
import persistencia.UsuarioDao;
import util.Json;

/**
 * Porta de entrada de /api/* e /app/*:
 *  - exige login (exceto endpoints públicos);
 *  - revalida o vínculo usuário x empresa no banco a cada requisição (se o vínculo foi removido, derruba a sessão);
 *  - exige perfil ADMIN em /api/admin/* e /app/usuarios.html;
 *  - proteção CSRF: requisições que alteram dados precisam do cabeçalho X-Requested-With.
 */
@WebFilter(urlPatterns = {"/api/*", "/app/*"})
public class AuthFilter implements Filter {

    private final UsuarioDao usuarios = new UsuarioDao();

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;

        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");

        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
        String metodo = req.getMethod();
        boolean leitura = metodo.equals("GET") || metodo.equals("HEAD") || metodo.equals("OPTIONS");

        if (path.startsWith("/app/")) {
            Sessao s = validarSessao(req);
            if (s == null) {
                resp.sendRedirect(req.getContextPath() + "/login.html");
            } else if (path.equals("/app/usuarios.html") && !s.isAdmin()) {
                resp.sendRedirect(req.getContextPath() + "/app/noticias.html");
            } else {
                chain.doFilter(rq, rs);
            }
            return;
        }

        if (!leitura && !"fetch".equals(req.getHeader("X-Requested-With"))) {
            negar(resp, 403, "Requisição não permitida.");
            return;
        }

        boolean publico = path.equals("/api/login") || path.equals("/api/sessao") || path.equals("/api/logout")
                || path.equals("/api/empresas") || (path.equals("/api/noticias") && leitura);
        if (publico) {
            chain.doFilter(rq, rs);
            return;
        }

        Sessao s = validarSessao(req);
        if (s == null) {
            negar(resp, 401, "Sessão expirada ou não autenticada.");
        } else if (path.startsWith("/api/admin/") && !s.isAdmin()) {
            negar(resp, 403, "Acesso restrito a administradores.");
        } else {
            chain.doFilter(rq, rs);
        }
    }

    /** Retorna a sessão se o usuário ainda pertence à empresa em que logou; senão invalida e retorna null. */
    private Sessao validarSessao(HttpServletRequest req) {
        HttpSession http = req.getSession(false);
        if (http == null) return null;
        Sessao s = (Sessao) http.getAttribute(ApiServlet.ATTR_SESSAO);
        if (s == null) return null;
        try {
            String perfil = usuarios.perfil(s.getUsuarioId(), s.getEmpresaId());
            if (perfil == null) {
                http.invalidate();
                return null;
            }
            s.setPerfil(perfil);
            return s;
        } catch (SQLException e) {
            return null;
        }
    }

    private void negar(HttpServletResponse resp, int status, String msg) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(Json.escrever(java.util.Collections.singletonMap("erro", msg)));
    }
}
