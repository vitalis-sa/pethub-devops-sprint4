package fiap.pethub.integracao;

import fiap.pethub.exception.ServicoIndisponivelException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite e confere os JWTs de serviço trocados com o .NET.
 *
 * <ul>
 *   <li>Saída (Java → .NET): HS256 de 60 s, emissor {@code pethub-java}, público {@code vitalis}.</li>
 *   <li>Entrada (.NET → Java): HS256 do emissor {@code vitalis}, público {@code pethub-java}, cujo
 *       {@code sub} é o responsável em nome de quem o .NET age. Assinatura, emissor, público e validade
 *       são conferidos; token sem expiração ou com vida longa demais é recusado.</li>
 * </ul>
 *
 * Sem segredo configurado nada é emitido nem aceito.
 */
@Component
public class JwtDeServico {

    static final Duration VALIDADE = Duration.ofSeconds(60);
    private static final Duration VALIDADE_MAXIMA_ACEITA = Duration.ofMinutes(5);
    private static final long TOLERANCIA_DO_RELOGIO_SEGUNDOS = 30;
    private static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    private final SecretKey chave;
    private final Clock relogio;

    @Autowired
    public JwtDeServico(ServicoProperties propriedades) {
        this(propriedades, Clock.systemUTC());
    }

    JwtDeServico(ServicoProperties propriedades, Clock relogio) {
        this.chave = lerChave(propriedades.segredo());
        this.relogio = relogio;
    }

    public boolean configurado() {
        return chave != null;
    }

    /** Token curto para uma chamada ao .NET; cada chamada leva o seu. */
    public String emitirParaVitalis() {
        if (chave == null) {
            throw new ServicoIndisponivelException("Segredo de serviço não configurado (SERVICE_JWT_SECRET).");
        }
        Instant agora = relogio.instant();
        return Jwts.builder()
                .issuer(ServicoProperties.EMISSOR_JAVA)
                .audience().add(ServicoProperties.EMISSOR_VITALIS).and()
                .subject(ServicoProperties.EMISSOR_JAVA)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(VALIDADE)))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    /** Id do responsável em nome de quem o .NET age, se o token for legítimo; vazio em qualquer outro caso. */
    public Optional<Long> responsavelDoToken(String token) {
        if (chave == null || token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            var claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(ServicoProperties.EMISSOR_VITALIS)
                    .requireAudience(ServicoProperties.EMISSOR_JAVA)
                    .clockSkewSeconds(TOLERANCIA_DO_RELOGIO_SEGUNDOS)
                    .clock(() -> Date.from(relogio.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date emitido = claims.getIssuedAt();
            Date expira = claims.getExpiration();
            if (emitido == null || expira == null
                    || Duration.between(emitido.toInstant(), expira.toInstant()).compareTo(VALIDADE_MAXIMA_ACEITA) > 0) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static SecretKey lerChave(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            return null;
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                    "SERVICE_JWT_SECRET precisa ter ao menos " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
