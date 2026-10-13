package util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de senha com PBKDF2 (JDK puro, sem dependências). Formato: pbkdf2$iteracoes$salt$hash */
public final class Senha {

    private static final int ITERACOES = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Usado para gastar o mesmo tempo quando o login não existe (evita enumeração por tempo). */
    private static final String HASH_FALSO = gerar("senha-falsa-" + System.nanoTime());

    private Senha() { }

    public static String gerar(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] h = pbkdf2(senha, salt, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(h);
    }

    public static boolean conferir(String senha, String armazenado) {
        if (armazenado == null) {
            armazenado = HASH_FALSO;
            senha = senha + "x";
        }
        try {
            String[] p = armazenado.split("\\$");
            int it = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] esperado = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(esperado, pbkdf2(senha, salt, it));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String senha, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
