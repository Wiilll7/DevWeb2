package modelo;

import java.util.LinkedHashMap;
import java.util.Map;

public class Noticia {

    public long id;
    public long empresaId;
    public String empresaNome;
    public String autor;
    public String titulo;
    public String texto;
    public String data;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("empresaId", empresaId);
        m.put("empresa", empresaNome);
        m.put("autor", autor);
        m.put("titulo", titulo);
        m.put("texto", texto);
        m.put("data", data);
        return m;
    }
}
