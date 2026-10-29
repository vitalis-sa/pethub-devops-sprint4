package fiap.pethub.integracao;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Segredos e tokens para os testes de autenticação entre serviços. */
public final class ChavesDeTeste {

    public static final String SEGREDO = "segredo-de-servico-de-teste-com-mais-de-32-caracteres";
    public static final String OUTRO_SEGREDO = "outro-segredo-de-servico-de-teste-bem-diferente-do-primeiro";

    private ChavesDeTeste() {
    }

    /** Token como o .NET o emitiria. */
    public static String tokenDoVitalis(String segredo, String emissor, String publico, String sub, Instant emitido, Instant expira) {
        return Jwts.builder()
                .issuer(emissor)
                .audience().add(publico).and()
                .subject(sub)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(emitido))
                .expiration(Date.from(expira))
                .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    public static String tokenValidoDoVitalis(String segredo, String sub) {
        Instant agora = Instant.now();
        return tokenDoVitalis(segredo, "vitalis", "pethub-java", sub, agora, agora.plusSeconds(60));
    }
}
