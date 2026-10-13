package modelo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Contexto do usuário logado: quem é e em QUAL empresa (tenant) ele entrou. */
public class Sessao implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long usuarioId;
    private final String login;
    private final String nome;
    private final long empresaId;
    private final String empresaNome;
    private volatile String perfil;

    public Sessao(long usuarioId, String login, String nome, long empresaId, String empresaNome, String perfil) {
        this.usuarioId = usuarioId;
        this.login = login;
        this.nome = nome;
        this.empresaId = empresaId;
        this.empresaNome = empresaNome;
        this.perfil = perfil;
    }

    public long getUsuarioId() { return usuarioId; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public long getEmpresaId() { return empresaId; }
    public String getEmpresaNome() { return empresaNome; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public boolean isAdmin() { return Usuario.ADMIN.equals(perfil); }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("autenticado", true);
        m.put("usuarioId", usuarioId);
        m.put("login", login);
        m.put("nome", nome);
        m.put("empresaId", empresaId);
        m.put("empresaNome", empresaNome);
        m.put("perfil", perfil);
        return m;
    }
}
