package fiap.pethub.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração da API HTTP da Brevo. O remetente precisa estar verificado na conta Brevo. */
@ConfigurationProperties("pethub.email.brevo")
public record BrevoProperties(String apiKey,
                              String remetenteEmail,
                              String remetenteNome,
                              String urlBase,
                              int timeoutSegundos) {

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank() && remetenteEmail != null && !remetenteEmail.isBlank();
    }
}
