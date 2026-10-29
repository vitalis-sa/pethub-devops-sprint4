package fiap.pethub.service;

import fiap.pethub.dto.request.LoginRequest;
import fiap.pethub.dto.request.VeterinarioRequest;
import fiap.pethub.dto.response.LoginResponse;
import fiap.pethub.dto.response.VeterinarioResponse;
import fiap.pethub.enums.Perfil;
import fiap.pethub.exception.EmailJaCadastradoException;
import fiap.pethub.repository.VeterinarioRepository;
import fiap.pethub.security.JwtService;
import fiap.pethub.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login e auto-cadastro do veterinário. O responsável autentica no serviço .NET.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final VeterinarioRepository veterinarioRepository;
    private final VeterinarioService veterinarioService;

    public LoginResponse autenticar(LoginRequest request) {
        var autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha()));
        var usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        return montarResposta(usuario.getEmail(), usuario.getNome(), usuario.getId());
    }

    @Transactional
    public LoginResponse registrarVeterinario(VeterinarioRequest request) {
        if (veterinarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailJaCadastradoException(request.getEmail());
        }
        VeterinarioResponse criado = veterinarioService.create(request);
        return montarResposta(request.getEmail(), criado.getNome(), criado.getId());
    }

    private LoginResponse montarResposta(String email, String nome, Long id) {
        return LoginResponse.builder()
                .token(jwtService.gerarToken(email, Perfil.VETERINARIO))
                .perfil(Perfil.VETERINARIO)
                .nome(nome)
                .id(id)
                .build();
    }
}
