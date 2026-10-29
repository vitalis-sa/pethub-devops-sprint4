package fiap.pethub.security;

import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.Perfil;
import fiap.pethub.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/**
 * O Java só autentica veterinários, e a identidade vem do próprio cadastro deles.
 * Responsáveis entram pelo .NET, com a credencial de serviço.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock private VeterinarioRepository veterinarioRepository;
    @InjectMocks private UsuarioDetailsService service;

    private void semVeterinarios() {
        lenient().when(veterinarioRepository.findByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void veterinarioCadastradoAutenticaComPerfilVeterinario() {
        Veterinario vet = Veterinario.builder()
                .id(1L).nome("Dra. Ana").email("ana@pethub.com").senha("hash").ativo(true).build();
        lenient().when(veterinarioRepository.findByEmail("ana@pethub.com")).thenReturn(Optional.of(vet));

        UsuarioAutenticado usuario = (UsuarioAutenticado) service.loadUserByUsername("ana@pethub.com");

        assertThat(usuario.getPerfil()).isEqualTo(Perfil.VETERINARIO);
        assertThat(usuario.getId()).isEqualTo(1L);
        assertThat(usuario.getUsername()).isEqualTo("ana@pethub.com");
    }

    @Test
    void emailDesconhecidoNaoAutentica() {
        semVeterinarios();

        assertThatThrownBy(() -> service.loadUserByUsername("ninguem@pethub.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void veterinarioInativoNaoAutentica() {
        Veterinario desligado = Veterinario.builder()
                .id(2L).nome("Dr. Antigo").email("antigo@pethub.com").senha("hash").ativo(false).build();
        lenient().when(veterinarioRepository.findByEmail("antigo@pethub.com"))
                .thenReturn(Optional.of(desligado));

        UsuarioAutenticado usuario = (UsuarioAutenticado) service.loadUserByUsername("antigo@pethub.com");

        assertThat(usuario.isEnabled()).isFalse();
    }

    @Test
    void perfilViraAuthorityComPrefixoRole() {
        Veterinario vet = Veterinario.builder()
                .id(1L).nome("Dra. Ana").email("ana@pethub.com").senha("hash").ativo(true).build();
        lenient().when(veterinarioRepository.findByEmail("ana@pethub.com")).thenReturn(Optional.of(vet));

        var usuario = service.loadUserByUsername("ana@pethub.com");

        assertThat(usuario.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_VETERINARIO");
    }
}
