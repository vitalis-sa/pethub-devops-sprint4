package fiap.pethub.security;

import fiap.pethub.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Resolve um email para o veterinário correspondente.
 *
 * O Java só autentica veterinários, que guardam email e senha no próprio cadastro. Responsáveis
 * entram pelo serviço .NET, que os identifica perante o Java com a credencial de serviço
 * (veja {@link ServicoAuthenticationFilter}).
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final VeterinarioRepository veterinarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return veterinarioRepository.findByEmail(email)
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Nenhum cadastro encontrado para o email informado"));
    }
}
