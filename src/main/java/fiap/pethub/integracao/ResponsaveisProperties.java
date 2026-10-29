package fiap.pethub.integracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Onde o serviço .NET de responsáveis responde. */
@ConfigurationProperties("pethub.responsaveis")
public record ResponsaveisProperties(String urlBase, int timeoutSegundos) {
}
