package fiap.pethub.config;

import fiap.pethub.integracao.ResponsaveisProperties;
import fiap.pethub.ia.GroqProperties;
import fiap.pethub.email.BrevoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Um {@link RestClient} por serviço externo, cada um com a URL base e o tempo limite próprios.
 *
 * O tempo limite de leitura é obrigatório: sem ele, um serviço travado seguraria a thread da
 * requisição (e a conexão do banco, se houvesse transação aberta) indefinidamente.
 */
@Configuration
public class ClientesHttpConfig {

    private static final Duration TEMPO_PARA_CONECTAR = Duration.ofSeconds(5);

    @Bean
    RestClient groqRestClient(GroqProperties groq) {
        return criar(groq.urlBase(), groq.timeoutSegundos());
    }

    @Bean
    RestClient brevoRestClient(BrevoProperties brevo) {
        return criar(brevo.urlBase(), brevo.timeoutSegundos());
    }

    @Bean
    RestClient responsaveisRestClient(ResponsaveisProperties responsaveis) {
        return criar(responsaveis.urlBase(), responsaveis.timeoutSegundos());
    }

    private RestClient criar(String urlBase, int timeoutSegundos) {
        var fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TEMPO_PARA_CONECTAR).build());
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }
}
