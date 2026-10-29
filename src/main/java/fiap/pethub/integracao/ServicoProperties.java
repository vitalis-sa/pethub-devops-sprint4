package fiap.pethub.integracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autenticação entre os serviços: JWT (HS256) assinado com um segredo que só os dois backends conhecem.
 * O segredo é independente do JWT do usuário e precisa ter ao menos 32 caracteres.
 *
 * @param segredo mesmo valor no Java ({@code SERVICE_JWT_SECRET}) e no .NET ({@code ServicoAuth__Segredo})
 */
@ConfigurationProperties("pethub.servico")
public record ServicoProperties(String segredo) {

    /** Fica separado de {@code Authorization}, que carrega o token do usuário final. */
    public static final String CABECALHO = "X-Service-Authorization";

    public static final String EMISSOR_JAVA = "pethub-java";
    public static final String EMISSOR_VITALIS = "vitalis";
}
