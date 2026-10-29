package fiap.pethub.ia;

import fiap.pethub.exception.IaIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqClientTest {

    private static final String URL = "https://ia.teste/openai/v1/chat/completions";
    private static final GroqProperties CONFIGURADO = new GroqProperties("chave-secreta", "modelo-x", "https://ia.teste", 1000, 5);

    private final JsonMapper json = JsonMapper.builder().build();
    private MockRestServiceServer servidor;
    private RestClient.Builder builder;

    @BeforeEach
    void preparar() {
        builder = RestClient.builder().baseUrl("https://ia.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    private GroqClient client(GroqProperties propriedades) {
        return new GroqClient(builder.build(), propriedades, json);
    }

    private String resposta(String finishReason, String texto) {
        return """
                {"choices":[{"finish_reason":"%s","message":{"role":"assistant","content":%s}}]}
                """.formatted(finishReason, json.writeValueAsString(texto));
    }

    private ResumoExameService.SaidaResumo chamar() {
        return client(CONFIGURADO).gerar(PromptIa.RESUMO_EXAME, "texto do exame", ResumoExameService.SaidaResumo.class);
    }

    @Test
    void enviaChaveNoCabecalhoPromptEConteudoEConverteARespostaJson() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-secreta"))
                .andExpect(requisicao -> assertThat(((MockClientHttpRequest) requisicao).getBodyAsString())
                        .contains("\"model\":\"modelo-x\"")
                        .contains("\"role\":\"system\"")
                        .contains("<documento>")
                        .contains("texto do exame")
                        .contains("\"response_format\":{\"type\":\"json_object\"}")
                        .doesNotContain("chave-secreta"))
                .andRespond(withSuccess(resposta("stop", "{\"resumo\":\"ok\"}"), MediaType.APPLICATION_JSON));

        assertThat(chamar().resumo()).isEqualTo("ok");
        servidor.verify();
    }

    @Test
    void semChaveOuModeloNemTentaChamar() {
        var semChave = client(new GroqProperties("", "modelo-x", "https://ia.teste", 1000, 5));
        var semModelo = client(new GroqProperties("chave", " ", "https://ia.teste", 1000, 5));

        assertThatThrownBy(() -> semChave.gerar(PromptIa.RESUMO_EXAME, "x", String.class)).isInstanceOf(IaIndisponivelException.class);
        assertThatThrownBy(() -> semModelo.gerar(PromptIa.RESUMO_EXAME, "x", String.class)).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void respostaCortadaPorLimiteDeTokensEhRecusada() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(resposta("length", "{\"resumo\":\"par"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(this::chamar).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void jsonForaDoFormatoVira503EmVezDeErro500() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(resposta("stop", "isto não é json"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(this::chamar).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void erroHttpNaoVazaOCorpoDaRespostaNemAChave() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("trecho sensível do prompt"));

        assertThatThrownBy(this::chamar)
                .isInstanceOf(IaIndisponivelException.class)
                .hasMessageContaining("400")
                .hasMessageNotContaining("sensível")
                .hasMessageNotContaining("chave-secreta");
    }

    @Test
    void tentaDeNovoQuandoOServicoEstaSobrecarregadoOuLimitado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(resposta("stop", "{\"resumo\":\"ok\"}"), MediaType.APPLICATION_JSON));

        assertThat(chamar().resumo()).isEqualTo("ok");
        servidor.verify();
    }

    @Test
    void semEscolhasEhRecusado() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(this::chamar).isInstanceOf(IaIndisponivelException.class);
    }
}
