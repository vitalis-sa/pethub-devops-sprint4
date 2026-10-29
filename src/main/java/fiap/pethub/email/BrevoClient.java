package fiap.pethub.email;

import fiap.pethub.exception.ServicoIndisponivelException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Envia e-mail transacional pela API HTTP da Brevo ({@code POST /v3/smtp/email}). */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrevoClient {

    @Qualifier("brevoRestClient")
    private final RestClient restClient;
    private final BrevoProperties brevo;

    /** Sem chave ou remetente todo envio falha em silêncio (fica só marcado para reenvio): avisa já na subida. */
    @PostConstruct
    void avisarSeNaoConfigurado() {
        if (!brevo.configurado()) {
            log.warn("E-mail DESLIGADO: defina BREVO_API_KEY e BREVO_REMETENTE_EMAIL (remetente verificado na Brevo). "
                    + "Enquanto isso nenhuma confirmação de consulta é enviada.");
        }
    }

    public void enviar(String destinatarioEmail, String destinatarioNome, String assunto, String texto) {
        if (!brevo.configurado()) {
            throw new ServicoIndisponivelException("E-mail não configurado: defina BREVO_API_KEY e BREVO_REMETENTE_EMAIL.");
        }
        var envio = new Envio(
                new Contato(brevo.remetenteNome(), brevo.remetenteEmail()),
                List.of(new Contato(destinatarioNome, destinatarioEmail)),
                assunto,
                texto);
        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", brevo.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envio)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicoIndisponivelException("A Brevo recusou ou não recebeu o envio do e-mail.", e);
        }
    }

    record Contato(String name, String email) {
    }

    record Envio(Contato sender, List<Contato> to, String subject, String textContent) {
    }
}
