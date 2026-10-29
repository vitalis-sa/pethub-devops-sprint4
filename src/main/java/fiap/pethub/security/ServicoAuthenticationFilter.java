package fiap.pethub.security;

import fiap.pethub.integracao.JwtDeServico;
import fiap.pethub.integracao.ServicoProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Deixa o serviço .NET agir em nome de um responsável.
 *
 * O .NET autentica o responsável e chama o Java com {@code X-Service-Authorization: Bearer <jwt>}, um
 * token HS256 assinado com o segredo de serviço, de vida curta, cujo {@code sub} é o responsável.
 * O Java confere com o mesmo segredo. Com o token válido, a
 * requisição passa a valer como a desse responsável, e todas as regras de posse continuam as mesmas.
 *
 * Token ausente, forjado, expirado ou de outro emissor: o filtro não autentica ninguém.
 */
@Component
@RequiredArgsConstructor
public class ServicoAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtDeServico jwt;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String cabecalho = request.getHeader(ServicoProperties.CABECALHO);
            if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
                jwt.responsavelDoToken(cabecalho.substring(PREFIXO.length()).trim()).ifPresent(this::autenticarComo);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void autenticarComo(Long responsavelId) {
        UsuarioAutenticado usuario = UsuarioAutenticado.deResponsavel(responsavelId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }
}
