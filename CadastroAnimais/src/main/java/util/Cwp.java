package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import modelo.Empresa;

/**
 * Chinese Wall Policy (Brewer-Nash): um mesmo usuário não pode ter acesso a dados de
 * empresas que tenham conflito de interesses entre si.
 * O conflito é tratado como simétrico: basta UMA das duas empresas listar a outra no campo "conflitos".
 */
public final class Cwp {

    private Cwp() { }

    public static boolean emConflito(Empresa a, Empresa b) {
        if (a.getId() == b.getId()) {
            return false;
        }
        return a.idsConflito().contains(b.getId()) || b.idsConflito().contains(a.getId());
    }

    /** Retorna o primeiro par em conflito dentro do conjunto, ou null se o conjunto é válido. */
    public static Empresa[] primeiroConflito(Collection<Empresa> empresas) {
        List<Empresa> l = new ArrayList<Empresa>(empresas);
        for (int i = 0; i < l.size(); i++) {
            for (int j = i + 1; j < l.size(); j++) {
                if (emConflito(l.get(i), l.get(j))) {
                    return new Empresa[]{l.get(i), l.get(j)};
                }
            }
        }
        return null;
    }
}
