package controle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import modelo.Noticia;
import modelo.Sessao;
import persistencia.NoticiaDao;

/**
 * GET    /api/noticias[?empresa=id]  -> PÚBLICO: notícias de todas as empresas (com o nome da empresa)
 * POST   /api/noticias               -> cadastra (sem "id") ou edita (com "id") notícia da empresa da sessão
 * DELETE /api/noticias?id=           -> exclui notícia da empresa da sessão
 * A empresa NUNCA vem do cliente: é sempre a da sessão.
 */
@WebServlet("/api/noticias")
public class NoticiaServlet extends ApiServlet {

    private final NoticiaDao dao = new NoticiaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Object> saida = new ArrayList<Object>();
            for (Noticia n : dao.listar(idParam(req, "empresa"))) saida.add(n.toMap());
            json(resp, 200, saida);
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        String titulo = param(req, "titulo");
        String texto = param(req, "texto");
        if (titulo.isEmpty() || titulo.length() > 150) {
            erro(resp, 400, "Informe um título de até 150 caracteres.");
            return;
        }
        if (texto.isEmpty() || texto.length() > 4000) {
            erro(resp, 400, "Informe um texto de até 4000 caracteres.");
            return;
        }
        try {
            if (param(req, "id").isEmpty()) {
                dao.criar(s.getEmpresaId(), s.getUsuarioId(), titulo, texto);
                ok(resp);
            } else {
                Long id = idParam(req, "id");
                if (id == null || !dao.atualizar(id, s.getEmpresaId(), titulo, texto)) {
                    erro(resp, 404, "Notícia não encontrada na sua empresa.");
                } else {
                    ok(resp);
                }
            }
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        Long id = idParam(req, "id");
        try {
            if (id == null || !dao.excluir(id, s.getEmpresaId())) {
                erro(resp, 404, "Notícia não encontrada na sua empresa.");
            } else {
                ok(resp);
            }
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }
}
