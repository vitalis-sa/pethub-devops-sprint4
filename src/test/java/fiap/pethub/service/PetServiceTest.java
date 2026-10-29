package fiap.pethub.service;

import fiap.pethub.dto.request.PetRequest;
import fiap.pethub.entity.Pet;
import fiap.pethub.exception.AcessoNegadoException;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.integracao.ResponsavelClient;
import fiap.pethub.integracao.ResponsavelResumo;
import fiap.pethub.mapper.PetMapper;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.repository.VeterinarioRepository;
import fiap.pethub.security.EscopoDoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PetServiceTest {

    private static final ResponsavelResumo JOAO = new ResponsavelResumo(7L, "João", "joao@email.com", "12345678901");

    @Mock private PetRepository repository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private ResponsavelClient responsavelClient;
    @Mock private PetMapper mapper;
    @Mock private EscopoDoUsuario escopo;
    @Mock private RemocaoEmCascata remocao;
    @InjectMocks private PetService service;

    @BeforeEach
    void cenarioPadrao() {
        when(mapper.toEntity(any())).thenAnswer(i -> Pet.builder().build());
        when(repository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void excluirPorResponsavelApagaHistoricoEPetsDele() {
        Pet a = Pet.builder().id(1L).responsavelId(7L).build();
        Pet b = Pet.builder().id(2L).responsavelId(7L).build();
        when(repository.findByResponsavelId(7L)).thenReturn(java.util.List.of(a, b));

        service.deleteByResponsavel(7L);

        verify(escopo).exigirPosse(7L);
        verify(remocao).apagarHistoricoDoPet(1L);
        verify(remocao).apagarHistoricoDoPet(2L);
        verify(repository).delete(a);
        verify(repository).delete(b);
    }

    private Pet gravado() {
        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(repository).save(pet.capture());
        return pet.getValue();
    }

    @Test
    void veterinarioCadastraPetParaOResponsavelDoCpf() {
        when(escopo.ehVeterinario()).thenReturn(true);
        when(responsavelClient.buscarPorCpf("12345678901")).thenReturn(Optional.of(JOAO));

        service.create(new PetRequest("Rex", "Cão", null, null, null, null, "12345678901", null));

        assertThat(gravado().getResponsavelId()).isEqualTo(7L);
        assertThat(gravado().getNomeResponsavel()).isEqualTo("João");
    }

    @Test
    void veterinarioPrecisaInformarOCpf() {
        when(escopo.ehVeterinario()).thenReturn(true);

        assertThatThrownBy(() -> service.create(new PetRequest("Rex", "Cão", null, null, null, null, " ", null)))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void veterinarioComCpfDesconhecidoRecebe404() {
        when(escopo.ehVeterinario()).thenReturn(true);
        when(responsavelClient.buscarPorCpf("000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new PetRequest("Rex", "Cão", null, null, null, null, "000", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void responsavelCadastraSempreParaSiMesmoIgnorandoOCpfEnviado() {
        when(escopo.ehVeterinario()).thenReturn(false);
        when(escopo.idDoResponsavel()).thenReturn(7L);
        when(responsavelClient.buscarPorId(7L)).thenReturn(Optional.of(JOAO));

        service.create(new PetRequest("Rex", "Cão", null, null, null, null, "cpf-de-outra-pessoa", null));

        assertThat(gravado().getResponsavelId()).isEqualTo(7L);
        verify(responsavelClient, never()).buscarPorCpf(any());
    }

    @Test
    void responsavelNaoListaPetsPeloCpfDeOutro() {
        when(responsavelClient.buscarPorCpf("999")).thenReturn(Optional.of(new ResponsavelResumo(99L, "Maria", "m@email.com", "999")));
        doThrow(new AcessoNegadoException("Recurso não encontrado")).when(escopo).exigirPosse(99L);

        assertThatThrownBy(() -> service.findByResponsavelCpf("999", org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(AcessoNegadoException.class);

        verify(repository, never()).findByResponsavelId(any(Long.class), any(org.springframework.data.domain.Pageable.class));
    }
}
