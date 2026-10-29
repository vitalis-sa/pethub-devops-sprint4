package fiap.pethub.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Corpo da requisição e da resposta do {@code /openai/v1/chat/completions} do Groq, só com os campos que usamos. */
final class GroqApi {

    private GroqApi() {
    }

    record Requisicao(String model,
                      List<Mensagem> messages,
                      double temperature,
                      @JsonProperty("max_completion_tokens") int maxCompletionTokens,
                      @JsonProperty("response_format") Map<String, String> responseFormat) {

        /** Modo JSON: o modelo só devolve um objeto JSON válido (o prompt precisa citar "JSON" e os campos). */
        static Requisicao de(String modelo, PromptIa prompt, String conteudo, double temperatura, int maxTokens) {
            String regras = prompt.getInstrucao()
                    + "\n\nResponda SOMENTE com um objeto JSON válido (sem markdown, sem comentários) com exatamente estes campos: "
                    + String.join(", ", prompt.getCampos()) + ".";
            return new Requisicao(
                    modelo,
                    List.of(new Mensagem("system", regras), new Mensagem("user", prompt.mensagemDoUsuario(conteudo))),
                    temperatura,
                    maxTokens,
                    Map.of("type", "json_object"));
        }
    }

    record Mensagem(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(List<Escolha> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Escolha(@JsonProperty("finish_reason") String finishReason, Mensagem message) {

        boolean terminouNormalmente() {
            return finishReason == null || "stop".equals(finishReason);
        }
    }
}
