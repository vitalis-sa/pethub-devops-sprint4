package fiap.pethub.integracao;

import fiap.pethub.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;

import static fiap.pethub.integracao.ChavesDeTeste.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResponsavelClientTest {

    private MockRestServiceServer servidor;
    private ResponsavelClient client;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dotnet.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new ResponsavelClient(builder.build(), new JwtDeServico(new ServicoProperties(SEGREDO)));
    }

    @Test
    void buscaPorIdEnviandoTokenDeServicoAssinadoEIgnoraCamposExtras() {
        servidor.expect(requestTo("https://dotnet.teste/api/responsavel/7"))
                .andExpect(request -> {
                    String cabecalho = request.getHeaders().getFirst("X-Service-Authorization");
                    assertThat(cabecalho).startsWith("Bearer ");
                    var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8))).requireIssuer("pethub-java")
                            .requireAudience("vitalis").build()
                            .parseSignedClaims(cabecalho.substring("Bearer ".length())).getPayload();
                    assertThat(claims.getSubject()).isEqualTo("pethub-java");
                })
                .andRespond(withSuccess("""
                        {"id":7,"nome":"João","email":"joao@email.com","cpf":"123","ativo":true,"enderecos":[]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.buscarPorId(7L)).contains(new ResponsavelResumo(7L, "João", "joao@email.com", "123"));
    }

    @Test
    void buscaPorCpf() {
        servidor.expect(requestTo("https://dotnet.teste/api/responsavel/buscar?cpf=123"))
                .andRespond(withSuccess("""
                        {"id":7,"nome":"João","cpf":"123","email":"joao@email.com","ativo":true}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.buscarPorCpf("123")).map(ResponsavelResumo::id).contains(7L);
    }

    @Test
    void cadastroInexistenteEhVazioEnaoErro() {
        servidor.expect(requestTo("https://dotnet.teste/api/responsavel/9")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.buscarPorId(9L)).isEmpty();
    }

    @Test
    void qualquerOutraFalhaEhServicoIndisponivel() {
        servidor.expect(requestTo("https://dotnet.teste/api/responsavel/7")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.buscarPorId(7L)).isInstanceOf(ServicoIndisponivelException.class);
    }
}
