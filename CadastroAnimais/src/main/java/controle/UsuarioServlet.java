package controle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import modelo.Empresa;
import modelo.Sessao;
import modelo.Usuario;
import persistencia.EmpresaDao;
import persistencia.UsuarioDao;
import util.Cwp;

/**
 * Cadastro de usuários (somente ADMIN - garantido por AuthFilter e conferido de novo aqui).
 *
 * Regras de escopo e CWP:
 *  - o administrador só enxerga/altera vínculos das empresas que ELE administra (lidas do banco a cada chamada);
 *  - nome e senha de um usuário só podem ser alterados se TODOS os vínculos dele estão no escopo do administrador
 *    (senão um admin poderia assumir a conta de alguém que também pertence a outra empresa);
 *  - o conjunto final de empresas do usuário não pode conter empresas em conflito de interesses.
 */
@WebServlet("/api/admin/usuarios")
public class UsuarioServlet extends ApiServlet {

    private final UsuarioDao usuarios = new UsuarioDao();
    private final EmpresaDao empresas = new EmpresaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        try {
            Set<Long> escopo = usuarios.empresasAdministradas(s.getUsuarioId());
            List<Object> listaEmpresas = new ArrayList<Object>();
            for (Empresa e : empresas.buscar(escopo)) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("id", e.getId());
                m.put("nome", e.getNome());
                listaEmpresas.add(m);
            }
            List<Object> listaUsuarios = new ArrayList<Object>();
            for (Usuario u : usuarios.listarDoEscopo(escopo, s.getUsuarioId())) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("id", u.getId());
                m.put("login", u.getLogin());
                m.put("nome", u.getNome());
                List<Object> vs = new ArrayList<Object>();
                boolean externo = false;
                for (Map.Entry<Long, String> v : u.getVinculos().entrySet()) {
                    if (escopo.contains(v.getKey())) {
                        Map<String, Object> vm = new LinkedHashMap<String, Object>();
                        vm.put("empresaId", v.getKey());
                        vm.put("perfil", v.getValue());
                        vs.add(vm);
                    } else {
                        externo = true; // não revela quais/quantas
                    }
                }
                m.put("vinculos", vs);
                m.put("externo", externo);
                listaUsuarios.add(m);
            }
            Map<String, Object> saida = new LinkedHashMap<String, Object>();
            saida.put("empresas", listaEmpresas);
            saida.put("usuarios", listaUsuarios);
            json(resp, 200, saida);
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }

    /** Sem "id" cria; com "id" atualiza. Vínculos vêm como v_<empresaId>=COMUM|ADMIN|(vazio). */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        try {
            Set<Long> escopo = usuarios.empresasAdministradas(s.getUsuarioId());
            if (!escopo.contains(s.getEmpresaId())) { // perdeu o perfil ADMIN no meio da sessão
                erro(resp, 403, "Acesso restrito a administradores.");
                return;
            }

            // vínculos pedidos
            Map<Long, String> novos = new LinkedHashMap<Long, String>();
            for (Enumeration<String> nomes = req.getParameterNames(); nomes.hasMoreElements();) {
                String nome = nomes.nextElement();
                if (!nome.startsWith("v_")) continue;
                String perfil = param(req, nome);
                if (perfil.isEmpty()) continue;
                long empresaId;
                try {
                    empresaId = Long.parseLong(nome.substring(2));
                } catch (NumberFormatException e) {
                    erro(resp, 400, "Empresa inválida.");
                    return;
                }
                if (!escopo.contains(empresaId)) {
                    erro(resp, 403, "Você não administra uma das empresas selecionadas.");
                    return;
                }
                if (!Usuario.COMUM.equals(perfil) && !Usuario.ADMIN.equals(perfil)) {
                    erro(resp, 400, "Perfil inválido.");
                    return;
                }
                novos.put(empresaId, perfil);
            }
            if (novos.isEmpty()) {
                erro(resp, 400, "Selecione ao menos uma empresa (para remover o usuário, use o botão Remover).");
                return;
            }

            String nome = param(req, "nome");
            String senha = req.getParameter("senha") == null ? "" : req.getParameter("senha");
            if (nome.isEmpty() || nome.length() > 100) {
                erro(resp, 400, "Informe o nome (até 100 caracteres).");
                return;
            }

            if (param(req, "id").isEmpty()) {
                criar(req, resp, s, nome, senha, novos);
            } else {
                atualizar(req, resp, s, escopo, nome, senha, novos);
            }
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }

    private void criar(HttpServletRequest req, HttpServletResponse resp, Sessao s, String nome, String senha,
            Map<Long, String> novos) throws IOException, SQLException {
        String login = param(req, "login");
        if (!login.matches("[A-Za-z0-9._-]{3,50}")) {
            erro(resp, 400, "Login: 3 a 50 caracteres (letras, números, ponto, hífen ou sublinhado).");
            return;
        }
        if (senha.length() < 6) {
            erro(resp, 400, "A senha deve ter ao menos 6 caracteres.");
            return;
        }
        if (usuarios.buscarPorLogin(login) != null) {
            erro(resp, 409, "Este login já está em uso.");
            return;
        }
        String conflito = verificarCwp(novos.keySet(), novos.keySet());
        if (conflito != null) {
            erro(resp, 409, conflito);
            return;
        }
        usuarios.criar(login, nome, senha, novos);
        ok(resp);
    }

    private void atualizar(HttpServletRequest req, HttpServletResponse resp, Sessao s, Set<Long> escopo, String nome,
            String senha, Map<Long, String> novos) throws IOException, SQLException {
        Long id = idParam(req, "id");
        Usuario alvo = null;
        if (id != null && id != s.getUsuarioId()) {
            for (Usuario u : usuarios.listarDoEscopo(escopo, s.getUsuarioId())) {
                if (u.getId() == id) alvo = u;
            }
        }
        if (alvo == null) {
            erro(resp, 404, "Usuário não encontrado entre as suas empresas.");
            return;
        }
        Set<Long> externas = new LinkedHashSet<Long>();
        for (Long e : alvo.getVinculos().keySet()) {
            if (!escopo.contains(e)) externas.add(e);
        }
        boolean trocaSenha = !senha.isEmpty();
        if (trocaSenha && senha.length() < 6) {
            erro(resp, 400, "A senha deve ter ao menos 6 caracteres.");
            return;
        }
        if (!externas.isEmpty() && (trocaSenha || !nome.equals(alvo.getNome()))) {
            erro(resp, 403, "Este usuário também pertence a empresa(s) que você não administra: "
                    + "só é possível alterar os vínculos com as suas empresas (não nome/senha).");
            return;
        }
        Set<Long> finais = new LinkedHashSet<Long>(externas);
        finais.addAll(novos.keySet());
        String conflito = verificarCwp(finais, novos.keySet());
        if (conflito != null) {
            erro(resp, 409, conflito);
            return;
        }
        usuarios.atualizar(alvo.getId(), externas.isEmpty() ? nome : null, trocaSenha ? senha : null, escopo, novos);
        ok(resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Sessao s = sessao(req);
        try {
            Set<Long> escopo = usuarios.empresasAdministradas(s.getUsuarioId());
            Long id = idParam(req, "id");
            Usuario alvo = null;
            if (id != null && id != s.getUsuarioId()) {
                for (Usuario u : usuarios.listarDoEscopo(escopo, s.getUsuarioId())) {
                    if (u.getId() == id) alvo = u;
                }
            }
            if (alvo == null) {
                erro(resp, 404, "Usuário não encontrado entre as suas empresas.");
                return;
            }
            // remove só os vínculos do escopo; se não sobrar nenhum, o usuário é excluído
            usuarios.atualizar(alvo.getId(), null, null, escopo, Collections.<Long, String>emptyMap());
            ok(resp);
        } catch (SQLException e) {
            erroInterno(resp, e);
        }
    }

    /**
     * Valida o conjunto final de empresas do usuário. Devolve a mensagem de erro ou null se está ok.
     * A mensagem só cita nomes de empresas que o administrador administra (não vaza empresas de outros tenants).
     */
    private String verificarCwp(Set<Long> finais, Set<Long> escopoDoAdmin) throws SQLException {
        Empresa[] par = Cwp.primeiroConflito(empresas.buscar(finais));
        if (par == null) return null;
        Empresa visivel = escopoDoAdmin.contains(par[0].getId()) ? par[0] : par[1];
        Empresa outra = visivel == par[0] ? par[1] : par[0];
        if (escopoDoAdmin.contains(outra.getId())) {
            return "Conflito de interesses: '" + visivel.getNome() + "' e '" + outra.getNome()
                    + "' não podem ser atribuídas ao mesmo usuário.";
        }
        return "Conflito de interesses: o usuário já pertence a uma empresa que conflita com '"
                + visivel.getNome() + "'.";
    }
}
