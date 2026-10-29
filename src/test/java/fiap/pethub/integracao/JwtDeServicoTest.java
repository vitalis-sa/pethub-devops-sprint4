package fiap.pethub.integracao;

import fiap.pethub.exception.ServicoIndisponivelException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static fiap.pethub.integracao.ChavesDeTeste.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtDeServicoTest {

    private final JwtDeServico jwt = new JwtDeServico(new ServicoProperties(SEGREDO));

    @Test
    void tokenEmitidoParaOVitalisVerificaComOSegredoEtemVidaCurta() {
        String token = jwt.emitirParaVitalis();

        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer("pethub-java").requireAudience("vitalis")
                .build().parseSignedClaims(token).getPayload();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(60_000L);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void aceitaTokenDoVitalisEDevolveOResponsavel() {
        assertThat(jwt.responsavelDoToken(tokenValidoDoVitalis(SEGREDO, "7"))).contains(7L);
    }

    @Test
    void recusaTokenAssinadoComOutroSegredo() {
        assertThat(jwt.responsavelDoToken(tokenValidoDoVitalis(OUTRO_SEGREDO, "7"))).isEmpty();
    }

    @Test
    void recusaEmissorOuPublicoErrado() {
        Instant agora = Instant.now();
        assertThat(jwt.responsavelDoToken(tokenDoVitalis(SEGREDO, "outro", "pethub-java", "7", agora, agora.plusSeconds(60)))).isEmpty();
        assertThat(jwt.responsavelDoToken(tokenDoVitalis(SEGREDO, "vitalis", "outro", "7", agora, agora.plusSeconds(60)))).isEmpty();
    }

    @Test
    void naoAceitaOProprioTokenQueOJavaEmite() {
        assertThat(jwt.responsavelDoToken(jwt.emitirParaVitalis())).isEmpty();
    }

    @Test
    void recusaTokenExpirado() {
        Instant agora = Instant.now();
        assertThat(jwt.responsavelDoToken(tokenDoVitalis(SEGREDO, "vitalis", "pethub-java", "7",
                agora.minusSeconds(600), agora.minusSeconds(540)))).isEmpty();
    }

    @Test
    void recusaTokenDeVidaLongaDemais() {
        Instant agora = Instant.now();
        assertThat(jwt.responsavelDoToken(tokenDoVitalis(SEGREDO, "vitalis", "pethub-java", "7",
                agora, agora.plusSeconds(3600)))).isEmpty();
    }

    @Test
    void recusaSubjectQueNaoEhNumero() {
        assertThat(jwt.responsavelDoToken(tokenValidoDoVitalis(SEGREDO, "abc"))).isEmpty();
    }

    @Test
    void recusaLixoENulo() {
        assertThat(jwt.responsavelDoToken("isso.nao.eh-jwt")).isEmpty();
        assertThat(jwt.responsavelDoToken("")).isEmpty();
        assertThat(jwt.responsavelDoToken(null)).isEmpty();
    }

    @Test
    void semSegredoNadaEEmitidoNemAceito() {
        var semSegredo = new JwtDeServico(new ServicoProperties(""));

        assertThat(semSegredo.configurado()).isFalse();
        assertThat(semSegredo.responsavelDoToken(tokenValidoDoVitalis(SEGREDO, "7"))).isEmpty();
        assertThatThrownBy(semSegredo::emitirParaVitalis).isInstanceOf(ServicoIndisponivelException.class);
    }

    @Test
    void segredoCurtoDemaisImpedeASubida() {
        assertThatThrownBy(() -> new JwtDeServico(new ServicoProperties("curto")))
                .isInstanceOf(IllegalStateException.class);
    }
}
