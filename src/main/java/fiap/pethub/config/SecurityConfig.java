package fiap.pethub.config;

import fiap.pethub.enums.Perfil;
import fiap.pethub.security.JwtAuthenticationFilter;
import fiap.pethub.security.PaginaInicial;
import fiap.pethub.security.UsuarioAutenticado;
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
 * prontuário, o responsável o consulta.</strong>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String VET = Perfil.VETERINARIO.name();
    private static final String TUTOR = Perfil.RESPONSAVEL.name();

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
                        .requestMatchers(HttpMethod.GET, ROTAS_CLINICAS).hasAnyRole(VET, TUTOR)
                        .requestMatchers(ROTAS_CLINICAS).hasRole(VET)
                        .requestMatchers(HttpMethod.GET, ROTAS_ADMINISTRATIVAS).hasAnyRole(VET, TUTOR)
                        .requestMatchers(ROTAS_ADMINISTRATIVAS).hasRole(VET)
                        .requestMatchers(HttpMethod.GET, "/api/leituras-wearable/**").hasAnyRole(VET, TUTOR)
                        .requestMatchers("/api/leituras-wearable/**").hasRole(VET)
                        .requestMatchers("/api/lembretes/**").hasAnyRole(VET, TUTOR)
                        .requestMatchers("/api/responsaveis/**").hasAnyRole(VET, TUTOR)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, erro) ->
                                responder(res, HttpStatus.UNAUTHORIZED, "Autenticação necessária"))
                        .accessDeniedHandler((req, res, erro) ->
                                responder(res, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta operação")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/login", "/registrar/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        // A pagina de erro precisa abrir para quem ainda nao entrou,
                        // senao um 404 de visitante vira um redirecionamento ao login.
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/tutor/**").hasRole(TUTOR)
                        .requestMatchers("/vet/**").hasRole(VET)

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            UsuarioAutenticado usuario = (UsuarioAutenticado) authentication.getPrincipal();
                            response.sendRedirect(PaginaInicial.de(usuario.getPerfil()));
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
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
