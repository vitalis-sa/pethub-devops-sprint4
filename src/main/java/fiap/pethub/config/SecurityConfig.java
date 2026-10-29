package fiap.pethub.config;

import fiap.pethub.enums.Perfil;
import fiap.pethub.security.JwtAuthenticationFilter;
import fiap.pethub.security.ServicoAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Regras de acesso da API.
 *
 * A autorização é declarada por rota, num lugar só, porque a pergunta que
 * importa — quem pode fazer o quê — deve ter uma resposta que se lê de cima a
 * baixo, em vez de estar espalhada em anotações pelos onze controllers.
 *
 * A separação de perfis segue o domínio: <strong>o veterinário produz o
 * prontuário, o responsável o consulta</strong> — e também agenda consultas e
 * cadastra pets para si. O veterinário entra com JWT; o responsável chega pelo
 * serviço .NET, com a credencial de serviço (veja ServicoAuthenticationFilter).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String VET = Perfil.VETERINARIO.name();
    private static final String RESPONSAVEL = Perfil.RESPONSAVEL.name();

    /** Rotas clínicas: só o veterinário escreve; ambos os perfis leem. */
    private static final String[] ROTAS_CLINICAS = {
            "/api/consultas/**",
            "/api/diagnosticos/**",
            "/api/exames/**",
            "/api/pedidos-medicos/**",
            "/api/vacinas-tratamentos/**"
    };

    /** Cadastros que só o veterinário administra. */
    private static final String[] ROTAS_ADMINISTRATIVAS = {
            "/api/pets/**",
            "/api/veterinarios/**",
            "/api/unidades/**"
    };

    /** O responsável agenda a própria consulta e cadastra os próprios pets; o resto do cadastro é do veterinário. */
    private static final String[] ROTAS_QUE_O_RESPONSAVEL_CRIA = {
            "/api/consultas",
            "/api/pets"
    };

    private final ServicoAuthenticationFilter servicoAuthenticationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Modo local de armazenamento: a assinatura da URL é a autorização.
                        .requestMatchers("/api/arquivos/**").permitAll()
                        .requestMatchers("/api/consultas/*/analise-ia").hasRole(VET)
                        .requestMatchers(HttpMethod.DELETE, "/api/pets/responsavel/*").hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(HttpMethod.PATCH, "/api/consultas/*/remarcar").hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(HttpMethod.PUT, "/api/consultas/*/queixas").hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(HttpMethod.POST, "/api/pets/*/foto/envio").hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(HttpMethod.PUT, "/api/pets/*/foto").hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(HttpMethod.POST, ROTAS_QUE_O_RESPONSAVEL_CRIA).hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(HttpMethod.GET, ROTAS_CLINICAS).hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(ROTAS_CLINICAS).hasRole(VET)
                        .requestMatchers(HttpMethod.GET, ROTAS_ADMINISTRATIVAS).hasAnyRole(VET, RESPONSAVEL)
                        .requestMatchers(ROTAS_ADMINISTRATIVAS).hasRole(VET)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, erro) ->
                                responder(res, HttpStatus.UNAUTHORIZED, "Autenticação necessária"))
                        .accessDeniedHandler((req, res, erro) ->
                                responder(res, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta operação")))
                .addFilterBefore(servicoAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Tudo fora de /api: só a documentação do Swagger e a rota de erro ficam
     * abertas. O PetHub não serve mais telas; o único cliente é o aplicativo.
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain demaisRotasFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    /** Escreve o mesmo formato de erro que o GlobalExceptionHandler usa. */
    private void responder(HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":%d,\"message\":\"%s\"}".formatted(status.value(), mensagem));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
