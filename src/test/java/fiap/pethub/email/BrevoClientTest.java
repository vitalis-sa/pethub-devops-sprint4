package fiap.pethub.email;

import fiap.pethub.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrevoClientTest {

    private static final BrevoProperties CONFIGURADO =
            new BrevoProperties("chave-brevo", "clinica@pethub.com", "PetHub", "https://brevo.teste", 5);

    private MockRestServiceServer servidor;
    private RestClient.Builder builder;

    @BeforeEach
    void preparar() {
        builder = RestClient.builder().baseUrl("https://brevo.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void enviaNoFormatoDaApiDaBrevo() {
        servidor.expect(requestTo("https://brevo.teste/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "chave-brevo"))
                .andExpect(requisicao -> assertThat(((MockClientHttpRequest) requisicao).getBodyAsString())
                        .contains("\"sender\":{\"name\":\"PetHub\",\"email\":\"clinica@pethub.com\"}")
                        .contains("\"to\":[{\"name\":\"João\",\"email\":\"joao@email.com\"}]")
                        .contains("\"subject\":\"Assunto\"")
                        .contains("\"textContent\":\"Texto\""))
                .andRespond(withStatus(HttpStatus.CREATED));

        new BrevoClient(builder.build(), CONFIGURADO).enviar("joao@email.com", "João", "Assunto", "Texto");

        servidor.verify();
    }

    @Test
    void recusaDaBrevoViraServicoIndisponivel() {
        servidor.expect(requestTo("https://brevo.teste/v3/smtp/email")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> new BrevoClient(builder.build(), CONFIGURADO).enviar("a@b.com", "A", "S", "T"))
                .isInstanceOf(ServicoIndisponivelException.class);
    }

    @Test
    void semConfiguracaoNemTentaEnviar() {
        var semChave = new BrevoProperties("", "clinica@pethub.com", "PetHub", "https://brevo.teste", 5);

        assertThatThrownBy(() -> new BrevoClient(builder.build(), semChave).enviar("a@b.com", "A", "S", "T"))
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }
}
