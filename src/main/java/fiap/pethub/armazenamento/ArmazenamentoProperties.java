package fiap.pethub.armazenamento;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Onde ficam exames e fotos. {@code modo=gcs} usa o bucket do Google Cloud Storage (credenciais padrão
 * do ambiente); {@code modo=local} grava em disco, para desenvolver sem conta do Google.
 */
@ConfigurationProperties("pethub.armazenamento")
public record ArmazenamentoProperties(
        String modo,
        String projeto,
        String bucket,
        int validadeDaUrlEmMinutos,
        String pastaLocal,
        String urlPublica,
        String segredoLocal) {

    public boolean configurado() {
        return bucket != null && !bucket.isBlank();
    }
}
