package fiap.pethub;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

/**
 * O esquema de segurança abaixo é o que faz o Swagger UI mostrar o botão
 * "Authorize": o usuário cola ali o token obtido em /api/auth/login uma única
 * vez, e toda chamada de "Try it out" passa a enviar o cabeçalho Authorization
 * sozinha. security = @SecurityRequirement aplica essa exigência a todos os
 * endpoints por padrão; AuthController remove a exigência dos seus três
 * endpoints públicos, que não pedem token.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "PetHub API — Java Backend (Sprint 4 · Azure DevOps) - teste do vídeo",
        version = "1.3.0",
        description = "API responsável por Veterinários, Unidades Veterinárias, Pets, Consultas, Exames, Diagnósticos, Vacinas/Tratamentos e Pedidos Médicos.",
        contact = @Contact(
            name = "PetHub Team",
            url = "https://github.com/pethub"
        )
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token obtido em POST /api/auth/login. Cole aqui só o token, sem o prefixo 'Bearer '."
)
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableCaching
public class PethubApplication {

	/** Fuso do negócio: consultas e lembretes são horários de Brasília, não do servidor (que costuma ser UTC). */
	public static final String FUSO = "America/Sao_Paulo";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(FUSO));
		SpringApplication.run(PethubApplication.class, args);
	}

}
