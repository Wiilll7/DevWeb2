package modelo;

import java.util.LinkedHashMap;
import java.util.Map;

public class Conta {

    public static final String PENDENTE = "PENDENTE";
    public static final String PAGA = "PAGA";

    public long id;
    public String descricao;
    public String valor;
    public String vencimento;
    public String situacao;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("descricao", descricao);
        m.put("valor", valor);
        m.put("vencimento", vencimento);
        m.put("situacao", situacao);
        return m;
    }
}
