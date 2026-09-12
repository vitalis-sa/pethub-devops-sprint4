package fiap.pethub.service;

import fiap.pethub.dto.request.ConsultaRequest;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.enums.TipoConsulta;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.mapper.ConsultaMapper;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.repository.UnidadeVeterinarioRepository;
import fiap.pethub.repository.VeterinarioRepository;
import fiap.pethub.security.EscopoDoUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * As duas regras que o agendamento precisa recusar antes de gravar qualquer coisa.
 *
 * Ficam num teste separado do resto do ConsultaService porque descrevem o fluxo
 * de agendar, e não o CRUD de consulta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultaServiceAgendamentoTest {

    private static final Long VETERINARIO_ID = 3L;
    private static final Long PET_ID = 5L;

    @Mock private ConsultaRepository repository;
    @Mock private PetRepository petRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UnidadeVeterinarioRepository unidadeRepository;
    @Mock private ConsultaMapper mapper;
    @Mock private EscopoDoUsuario escopo;
    @Mock private LembreteService lembreteService;

    @InjectMocks private ConsultaService consultaService;

    @Test
    @DisplayName("recusa consulta em horário já ocupado pelo veterinário")
    void recusaHorarioOcupado() {
        LocalDateTime amanha = LocalDateTime.now().plusDays(1);
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(
                eq(VETERINARIO_ID), eq(amanha), eq(StatusConsulta.CANCELADA))).thenReturn(true);

        assertThatThrownBy(() -> consultaService.create(agendamento(amanha)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já tem uma consulta marcada neste horário");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recusa agendamento para uma data que já passou")
    void recusaDataNoPassado() {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(any(), any(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> consultaService.create(agendamento(ontem)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data que já passou");

        verify(repository, never()).save(any());
    }

    /**
     * Lançar no sistema uma consulta que já aconteceu é legítimo — por isso a
     * regra da data futura vale só para o que está sendo agendado.
     */
    @Test
    @DisplayName("aceita registrar no passado uma consulta já realizada")
    void aceitaRegistroRetroativoDeConsultaRealizada() {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(any(), any(), any()))
                .thenReturn(false);
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        // Passa das duas validações de agendamento e só então esbarra no pet
        // inexistente, o que prova que nenhuma delas barrou o registro retroativo.
        assertThatThrownBy(() -> consultaService.create(
                new ConsultaRequest(ontem, TipoConsulta.PRESENCIAL, "Retroativa",
                        StatusConsulta.REALIZADA, PET_ID, VETERINARIO_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ConsultaRequest agendamento(LocalDateTime quando) {
        return new ConsultaRequest(quando, TipoConsulta.PRESENCIAL, "Check-up",
                StatusConsulta.AGENDADA, PET_ID, VETERINARIO_ID, null);
    }
}
