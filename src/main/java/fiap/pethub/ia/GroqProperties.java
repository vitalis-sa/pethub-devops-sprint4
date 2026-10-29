package fiap.pethub.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do Groq (API compatível com a da OpenAI). A chave vem do ambiente e nunca é versionada;
 * o modelo também, porque os ids mudam com o tempo.
 */
@ConfigurationProperties("pethub.ia.groq")
public record GroqProperties(String apiKey, String modelo, String urlBase, int maxTokens, int timeoutSegundos) {

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank() && modelo != null && !modelo.isBlank();
    }
}
