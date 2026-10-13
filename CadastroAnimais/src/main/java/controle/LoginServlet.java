package controle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import modelo.Empresa;
import modelo.Sessao;
import modelo.Usuario;
import persistencia.EmpresaDao;
import persistencia.UsuarioDao;
import util.Cwp;

/** POST /api/login (empresa, usuario, senha) | GET /api/sessao | POST /api/logout */
@WebServlet(urlPatterns = {"/api/login", "/api/sessao", "/api/logout"})
public class LoginServlet extends ApiServlet {

    private final UsuarioDao usuarios = new UsuarioDao();
    private final EmpresaDao empresas = new EmpresaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().equals("/api/sessao")) {
            resp.sendError(405);
            return;
        }
        Sessao s = sessao(req);
        if (s == null) {
            json(resp, 200, Collections.singletonMap("autenticado", false));
        } else {
            json(resp, 200, s.toMap());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/api/logout")) {
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            ok(resp);
            return;
        }
        if (!req.getServletPath().equals("/api/login")) {
            resp.sendError(405);
            return;
        }
        try {
            Long empresaId = idParam(req, "empresa");
            String login = param(req, "usuario");
            String senha = req.getParameter("senha") == null ? "" : req.getParameter("senha");

            Usuario u = usuarios.autenticar(login, senha);
            Empresa empresa = empresaId == null ? null : empresas.buscar(empresaId);
            String perfil = (u != null && empresa != null) ? usuarios.perfil(u.getId(), empresa.getId()) : null;
            if (perfil == null) {
                // mesma mensagem para qualquer falha: não revela se o usuário existe
                erro(resp, 401, "Empresa, usuário ou senha inválidos.");
                return;
            }

            // CWP: se, por alteração direta no banco, o usuário passou a ter vínculo com empresas em conflito, bloqueia.
            List<Empresa> todas = empresas.buscar(usuarios.vinculos(u.getId()).keySet());
            if (Cwp.primeiroConflito(todas) != null) {
                erro(resp, 403, "Acesso bloqueado: o usuário está vinculado a empresas com conflito de interesses. "
                        + "Procure o administrador.");
                return;
            }

            // nova sessão a cada login (evita session fixation)
            HttpSession antiga = req.getSession(false);
            if (antiga != null) antiga.invalidate();
            HttpSession http = req.getSession(true);
            Sessao s = new Sessao(u.getId(), u.getLogin(), u.getNome(), empresa.getId(), empresa.getNome(), perfil);
            http.setAttribute(ATTR_SESSAO, s);
            json(resp, 200, s.toMap());
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }
}
