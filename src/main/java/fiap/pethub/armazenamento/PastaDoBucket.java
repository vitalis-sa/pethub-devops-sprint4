package fiap.pethub.armazenamento;

import java.util.Map;
import java.util.Set;

/** Subpastas do bucket e o que cada uma aceita receber. */
public enum PastaDoBucket {

    EXAMES("exames", Map.of("application/pdf", "pdf")),
    FOTOS("fotos", Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp"));

    private final String prefixo;
    private final Map<String, String> extensoesPorTipo;

    PastaDoBucket(String prefixo, Map<String, String> extensoesPorTipo) {
        this.prefixo = prefixo;
        this.extensoesPorTipo = extensoesPorTipo;
    }

    public String prefixo() {
        return prefixo;
    }

    public Set<String> tiposAceitos() {
        return extensoesPorTipo.keySet();
    }

    public boolean aceita(String contentType) {
        return extensoesPorTipo.containsKey(contentType);
    }

    public String extensaoDe(String contentType) {
        return extensoesPorTipo.get(contentType);
    }
}
