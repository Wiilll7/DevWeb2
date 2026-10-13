package controle;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import modelo.Conta;
import modelo.Sessao;
import persistencia.ContaDao;

/** CRUD de contas. Privado: sempre restrito à empresa da sessão. */
@WebServlet("/api/contas")
public class ContaServlet extends ApiServlet {

    private final ContaDao dao = new ContaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        try {
            List<Object> saida = new ArrayList<Object>();
            for (Conta c : dao.listar(s.getEmpresaId())) saida.add(c.toMap());
            json(resp, 200, saida);
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        String descricao = param(req, "descricao");
        String situacao = param(req, "situacao");
        BigDecimal valor;
        LocalDate vencimento;
        if (descricao.isEmpty() || descricao.length() > 150) {
            erro(resp, 400, "Informe uma descrição de até 150 caracteres.");
            return;
        }
        try {
            valor = new BigDecimal(param(req, "valor").replace(',', '.'));
            if (valor.signum() < 0 || valor.scale() > 2 || valor.precision() > 12) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            erro(resp, 400, "Valor inválido (use número positivo com até 2 casas decimais).");
            return;
        }
        try {
            vencimento = LocalDate.parse(param(req, "vencimento"));
        } catch (DateTimeParseException e) {
            erro(resp, 400, "Data de vencimento inválida.");
            return;
        }
        if (!Conta.PENDENTE.equals(situacao) && !Conta.PAGA.equals(situacao)) {
            erro(resp, 400, "Situação inválida.");
            return;
        }
        try {
            if (param(req, "id").isEmpty()) {
                dao.criar(s.getEmpresaId(), descricao, valor, vencimento, situacao);
                ok(resp);
            } else {
                Long id = idParam(req, "id");
                if (id == null || !dao.atualizar(id, s.getEmpresaId(), descricao, valor, vencimento, situacao)) {
                    erro(resp, 404, "Conta não encontrada na sua empresa.");
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
                erro(resp, 404, "Conta não encontrada na sua empresa.");
            } else {
                ok(resp);
            }
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }
}
