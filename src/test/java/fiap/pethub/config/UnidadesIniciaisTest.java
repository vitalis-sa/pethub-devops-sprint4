package fiap.pethub.config;

import fiap.pethub.entity.UnidadeVeterinario;
import fiap.pethub.repository.UnidadeVeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadesIniciaisTest {

    @Mock UnidadeVeterinarioRepository repository;
    @InjectMocks UnidadesIniciais unidadesIniciais;

    @Test
    void cadastraAsDuasUnidadesQuandoNaoExistem() {
        when(repository.existsByCep(any())).thenReturn(false);

        unidadesIniciais.run(null);

        ArgumentCaptor<UnidadeVeterinario> salvas = ArgumentCaptor.forClass(UnidadeVeterinario.class);
        verify(repository, org.mockito.Mockito.times(2)).save(salvas.capture());
        assertThat(salvas.getAllValues()).extracting(UnidadeVeterinario::getCep)
                .containsExactly("01311000", "01538001");
    }

    @Test
    void naoDuplicaUnidadesJaCadastradas() {
        when(repository.existsByCep(any())).thenReturn(true);

        unidadesIniciais.run(null);

        verify(repository, never()).save(any());
    }
}
