package modelo;

import java.util.LinkedHashMap;
import java.util.Map;

public class Usuario {

    public static final String COMUM = "COMUM";
    public static final String ADMIN = "ADMIN";

    private long id;
    private String login;
    private String nome;
    private String senhaHash;
    /** empresaId -> perfil (COMUM ou ADMIN) */
    private Map<Long, String> vinculos = new LinkedHashMap<Long, String>();

    public Usuario(long id, String login, String nome, String senhaHash) {
        this.id = id;
        this.login = login;
        this.nome = nome;
        this.senhaHash = senhaHash;
    }

    public long getId() { return id; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public String getSenhaHash() { return senhaHash; }
    public Map<Long, String> getVinculos() { return vinculos; }
}
