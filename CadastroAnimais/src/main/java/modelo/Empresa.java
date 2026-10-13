package modelo;

import java.util.HashSet;
import java.util.Set;

/** Tenant. A coluna "conflitos" guarda os ids das empresas em conflito de interesses (ex.: "2,5"). */
public class Empresa {

    private long id;
    private String nome;
    private String conflitos;

    public Empresa(long id, String nome, String conflitos) {
        this.id = id;
        this.nome = nome;
        this.conflitos = conflitos == null ? "" : conflitos;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getConflitos() { return conflitos; }

    /** Ids listados no campo "conflitos" desta empresa. */
    public Set<Long> idsConflito() {
        Set<Long> ids = new HashSet<Long>();
        for (String s : conflitos.split("[,;\\s]+")) {
            if (!s.isEmpty()) {
                try {
                    ids.add(Long.parseLong(s));
                } catch (NumberFormatException ignorado) {
                    // valor inválido digitado direto no banco: ignora
                }
            }
        }
        return ids;
    }
}
