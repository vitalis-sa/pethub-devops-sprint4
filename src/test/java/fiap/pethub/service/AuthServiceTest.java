package fiap.pethub.service;

import fiap.pethub.dto.request.LoginRequest;
import fiap.pethub.dto.request.VeterinarioRequest;
import fiap.pethub.dto.response.LoginResponse;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.Perfil;
import fiap.pethub.exception.EmailJaCadastradoException;
import fiap.pethub.repository.VeterinarioRepository;
import fiap.pethub.security.JwtService;
import fiap.pethub.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private VeterinarioService veterinarioService;
    @InjectMocks private AuthService service;

    @Test
    void naoRegistraVeterinarioComEmailJaCadastrado() {
        when(veterinarioRepository.findByEmail("ana@pethub.com"))
                .thenReturn(Optional.of(Veterinario.builder().id(1L).email("ana@pethub.com").build()));
        var pedido = new VeterinarioRequest("Ana", "CRMV1", "Clínica geral",
                "ana@pethub.com", "11999999999", "senha123", true, null);

        assertThatThrownBy(() -> service.registrarVeterinario(pedido))
                .isInstanceOf(EmailJaCadastradoException.class);

        verify(veterinarioService, never()).create(any());
    }

    @Test
    void loginDevolveTokenDeVeterinario() {
        var autenticado = UsuarioAutenticado.de(Veterinario.builder()
                .id(1L).nome("Dra. Ana").email("ana@pethub.com").senha("hash").ativo(true).build());
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(autenticado, null, autenticado.getAuthorities()));
        when(jwtService.gerarToken("ana@pethub.com", Perfil.VETERINARIO)).thenReturn("token-abc");

        LoginResponse resposta = service.autenticar(new LoginRequest("ana@pethub.com", "senha123"));

        assertThat(resposta.getToken()).isEqualTo("token-abc");
        assertThat(resposta.getPerfil()).isEqualTo(Perfil.VETERINARIO);
        assertThat(resposta.getNome()).isEqualTo("Dra. Ana");
        assertThat(resposta.getId()).isEqualTo(1L);
    }
}
