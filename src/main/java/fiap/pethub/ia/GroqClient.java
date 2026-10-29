package fiap.pethub.ia;

import fiap.pethub.exception.IaIndisponivelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

/**
 * Fala com a API do Groq (compatível com a da OpenAI: {@code /openai/v1/chat/completions}).
 *
 * O caminho de uma chamada: o {@link PromptIa} diz o que pedir e em que formato; este cliente monta a
 * requisição, envia (chave no cabeçalho {@code Authorization}, nunca na URL), confere que o modelo
 * terminou a resposta e converte o JSON devolvido no tipo pedido pelo chamador.
 *
 * Toda falha vira {@link IaIndisponivelException}: quem chama não precisa conhecer HTTP, e a API
 * responde 503 em vez de 500.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqClient {

    /** Baixa, porque o texto é clínico: queremos fidelidade à transcrição, não criatividade. */
    private static final double TEMPERATURA = 0.2;

    /** Limite de pedidos (429) ou sobrecarga (503) costumam passar em segundos: 3 tentativas no total. */
    private static final long[] ESPERAS_ENTRE_TENTATIVAS_MS = {1000, 3000};

    @Qualifier("groqRestClient")
    private final RestClient restClient;
    private final GroqProperties propriedades;
    private final JsonMapper jsonMapper;

    /** Modelo configurado; gravado junto do texto gerado, para saber quem o escreveu. */
    public String modelo() {
        return propriedades.modelo();
    }

    public <T> T gerar(PromptIa prompt, String conteudo, Class<T> tipo) {
        if (!propriedades.configurado()) {
            throw new IaIndisponivelException("IA não configurada: defina GROQ_API_KEY e GROQ_MODEL.");
        }
        var requisicao = GroqApi.Requisicao.de(propriedades.modelo(), prompt, conteudo, TEMPERATURA, propriedades.maxTokens());
        log.info("Chamando o Groq (modelo {}, {} caracteres enviados)", propriedades.modelo(), conteudo.length());
        long inicio = System.nanoTime();
        String texto = textoDaResposta(enviar(requisicao));
        log.info("Groq respondeu em {} ms", (System.nanoTime() - inicio) / 1_000_000);
        return converter(texto, tipo);
    }

    private GroqApi.Resposta enviar(GroqApi.Requisicao requisicao) {
        for (int tentativa = 0; ; tentativa++) {
            try {
                return restClient.post()
                        .uri("/openai/v1/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + propriedades.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requisicao)
                        .retrieve()
                        .body(GroqApi.Resposta.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean passageiro = status == 429 || status == 503;
                if (passageiro && tentativa < ESPERAS_ENTRE_TENTATIVAS_MS.length) {
                    log.warn("Groq respondeu HTTP {}; nova tentativa em {} ms", status, ESPERAS_ENTRE_TENTATIVAS_MS[tentativa]);
                    esperar(ESPERAS_ENTRE_TENTATIVAS_MS[tentativa]);
                    continue;
                }
                // O corpo do erro não é repassado: pode citar trechos do que foi enviado.
                log.warn("Groq respondeu HTTP {}", status);
                String mensagem = switch (status) {
                    case 401 -> "O serviço de IA recusou a chave (HTTP 401). Confira GROQ_API_KEY.";
                    case 429 -> "Limite de uso do serviço de IA atingido (HTTP 429). Tente de novo em instantes.";
                    case 503 -> "O serviço de IA está sobrecarregado no momento (HTTP 503). Tente de novo em instantes.";
                    default -> "O serviço de IA recusou a solicitação (HTTP " + status + ").";
                };
                throw new IaIndisponivelException(mensagem, e);
            } catch (RestClientException e) {
                throw new IaIndisponivelException("Não foi possível falar com o serviço de IA.", e);
            }
        }
    }

    private void esperar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaIndisponivelException("A chamada ao serviço de IA foi interrompida.", e);
        }
    }

    private String textoDaResposta(GroqApi.Resposta resposta) {
        GroqApi.Escolha escolha = Optional.ofNullable(resposta)
                .map(GroqApi.Resposta::choices)
                .filter(escolhas -> !escolhas.isEmpty())
                .map(List::getFirst)
                .orElseThrow(() -> new IaIndisponivelException("O serviço de IA não devolveu resposta."));
        if (!escolha.terminouNormalmente()) {
            throw new IaIndisponivelException("O serviço de IA não concluiu a resposta (" + escolha.finishReason() + ").");
        }
        String texto = Optional.ofNullable(escolha.message()).map(GroqApi.Mensagem::content).orElse("");
        if (texto.isBlank()) {
            throw new IaIndisponivelException("O serviço de IA devolveu uma resposta sem texto.");
        }
        return texto;
    }

    private <T> T converter(String texto, Class<T> tipo) {
        try {
            return jsonMapper.readValue(texto, tipo);
        } catch (RuntimeException e) {
            throw new IaIndisponivelException("A resposta do serviço de IA veio fora do formato esperado.", e);
        }
    }
}
