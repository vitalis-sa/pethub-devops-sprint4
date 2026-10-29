package fiap.pethub.security;

import fiap.pethub.enums.Perfil;
import fiap.pethub.integracao.JwtDeServico;
import fiap.pethub.integracao.ServicoProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static fiap.pethub.integracao.ChavesDeTeste.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** A credencial de serviço é a porta pela qual o .NET entra: ela só pode abrir quando tudo confere. */
class ServicoAuthenticationFilterTest {

    private final FilterChain cadeia = mock(FilterChain.class);
    private final MockHttpServletRequest requisicao = new MockHttpServletRequest();
    private final MockHttpServletResponse resposta = new MockHttpServletResponse();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void executar(String segredoConfigurado) throws Exception {
        var jwt = new JwtDeServico(new ServicoProperties(segredoConfigurado));
        new ServicoAuthenticationFilter(jwt).doFilter(requisicao, resposta, cadeia);
    }

    private UsuarioAutenticado autenticado() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao == null ? null : (UsuarioAutenticado) autenticacao.getPrincipal();
    }

    @Test
    void tokenAssinadoPeloVitalisAutenticaComoOResponsavelDoSubject() throws Exception {
        requisicao.addHeader("X-Service-Authorization", "Bearer " + tokenValidoDoVitalis(SEGREDO, "7"));

        executar(SEGREDO);

        assertThat(autenticado().getPerfil()).isEqualTo(Perfil.RESPONSAVEL);
        assertThat(autenticado().getId()).isEqualTo(7L);
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void tokenAssinadoComOutroSegredoNaoAutentica() throws Exception {
        requisicao.addHeader("X-Service-Authorization", "Bearer " + tokenValidoDoVitalis(OUTRO_SEGREDO, "7"));

        executar(SEGREDO);

        assertThat(autenticado()).isNull();
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void tokenExpiradoNaoAutentica() throws Exception {
        Instant agora = Instant.now();
        requisicao.addHeader("X-Service-Authorization", "Bearer "
                + tokenDoVitalis(SEGREDO, "vitalis", "pethub-java", "7", agora.minusSeconds(600), agora.minusSeconds(540)));

        executar(SEGREDO);

        assertThat(autenticado()).isNull();
    }

    @Test
    void semSegredoConfiguradoNinguemEntra() throws Exception {
        requisicao.addHeader("X-Service-Authorization", "Bearer " + tokenValidoDoVitalis(SEGREDO, "7"));

        executar("");

        assertThat(autenticado()).isNull();
    }

    @Test
    void semCabecalhoOuSemBearerNaoAutentica() throws Exception {
        executar(SEGREDO);
        assertThat(autenticado()).isNull();

        requisicao.addHeader("X-Service-Authorization", tokenValidoDoVitalis(SEGREDO, "7"));
        executar(SEGREDO);
        assertThat(autenticado()).isNull();
    }
}
