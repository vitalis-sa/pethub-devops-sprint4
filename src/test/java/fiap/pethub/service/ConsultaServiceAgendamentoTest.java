package fiap.pethub.service;

import fiap.pethub.dto.request.ConsultaRequest;
import fiap.pethub.email.ConsultaAgendada;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.enums.TipoLembrete;
import fiap.pethub.exception.AcessoNegadoException;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.mapper.ConsultaMapper;
import fiap.pethub.messaging.LembreteSolicitado;
import fiap.pethub.repository.AnaliseConsultaRepository;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.EmailConsultaRepository;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.repository.UnidadeVeterinarioRepository;
import fiap.pethub.repository.VeterinarioRepository;
import fiap.pethub.security.EscopoDoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * As regras do agendamento: o que é recusado antes de gravar, quem pode agendar e o que acontece
 * depois da gravação (lembrete para o .NET e e-mail de confirmação).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultaServiceAgendamentoTest {

    private static final Long VETERINARIO_ID = 3L;
    private static final Long PET_ID = 5L;
    private static final Long RESPONSAVEL_ID = 7L;

    @Mock private ConsultaRepository repository;
    @Mock private PetRepository petRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UnidadeVeterinarioRepository unidadeRepository;
    @Mock private ConsultaMapper mapper;
    @Mock private EscopoDoUsuario escopo;
    @Mock private ApplicationEventPublisher eventos;
    @Mock private AnaliseConsultaRepository analiseConsultaRepository;
    @Mock private EmailConsultaRepository emailConsultaRepository;
    @InjectMocks private ConsultaService consultaService;

    @BeforeEach
    void cenarioPadrao() {
        Pet pet = Pet.builder().id(PET_ID).nome("Rex").responsavelId(RESPONSAVEL_ID).build();
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));
        when(veterinarioRepository.findById(VETERINARIO_ID)).thenReturn(Optional.of(Veterinario.builder().id(VETERINARIO_ID).build()));
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(false);
        when(mapper.toEntity(any())).thenAnswer(i ->
                Consulta.builder().dataHora(i.<ConsultaRequest>getArgument(0).getDataHora()).build());
        when(repository.save(any(Consulta.class))).thenAnswer(i -> {
            Consulta c = i.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(escopo.ehVeterinario()).thenReturn(true);
    }

    @Test
    @DisplayName("recusa consulta em horário já ocupado pelo veterinário")
    void recusaHorarioOcupado() {
        LocalDateTime amanha = LocalDateTime.now().plusDays(1);
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(
                eq(VETERINARIO_ID), eq(amanha), eq(StatusConsulta.CANCELADA))).thenReturn(true);

        assertThatThrownBy(() -> consultaService.create(agendamento(amanha, StatusConsulta.AGENDADA)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já tem uma consulta marcada neste horário");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recusa agendamento para uma data que já passou")
    void recusaDataNoPassado() {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> consultaService.create(agendamento(ontem, StatusConsulta.AGENDADA)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data que já passou");

        verify(repository, never()).save(any());
    }

    /**
     * Lançar no sistema uma consulta que já aconteceu é legítimo — por isso a
     * regra da data futura vale só para o que está sendo agendado. E uma consulta
     * já realizada não gera lembrete nem e-mail de confirmação.
     */
    @Test
    @DisplayName("veterinário registra no passado uma consulta já realizada, sem avisar o responsável")
    void aceitaRegistroRetroativoDeConsultaRealizada() {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);

        consultaService.create(agendamento(ontem, StatusConsulta.REALIZADA));

        verify(repository).save(any(Consulta.class));
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("depois de gravar, publica o lembrete e pede o e-mail de confirmação")
    void avisaOResponsavelDepoisDeGravar() {
        LocalDateTime amanha = LocalDateTime.now().plusDays(1);

        consultaService.create(agendamento(amanha, StatusConsulta.AGENDADA));

        ArgumentCaptor<Object> eventosPublicados = ArgumentCaptor.forClass(Object.class);
        verify(eventos, org.mockito.Mockito.times(2)).publishEvent(eventosPublicados.capture());
        assertThat(eventosPublicados.getAllValues()).containsExactly(
                new LembreteSolicitado(RESPONSAVEL_ID, PET_ID, TipoLembrete.CONSULTA, amanha.toLocalDate(),
                        "Consulta agendada para Rex em "
                                + amanha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")), 100L, "Consulta"),
                new ConsultaAgendada(100L));
    }

    @Test
    @DisplayName("responsável agenda para o próprio pet e o status vira AGENDADA mesmo que mande outro")
    void responsavelSoAgenda() {
        when(escopo.ehVeterinario()).thenReturn(false);
        LocalDateTime amanha = LocalDateTime.now().plusDays(1);

        consultaService.create(agendamento(amanha, StatusConsulta.REALIZADA));

        ArgumentCaptor<Consulta> gravada = ArgumentCaptor.forClass(Consulta.class);
        verify(repository).save(gravada.capture());
        assertThat(gravada.getValue().getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        verify(escopo).exigirPosse(RESPONSAVEL_ID);
    }

    @Test
    @DisplayName("responsável não agenda consulta para pet de outro")
    void responsavelNaoAgendaParaPetAlheio() {
        doThrow(new AcessoNegadoException("Recurso não encontrado")).when(escopo).exigirPosse(RESPONSAVEL_ID);

        assertThatThrownBy(() -> consultaService.create(agendamento(LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA)))
                .isInstanceOf(AcessoNegadoException.class);

        verify(repository, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("pet inexistente é 404")
    void petInexistente() {
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.create(agendamento(LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ConsultaRequest agendamento(LocalDateTime quando, StatusConsulta status) {
        return new ConsultaRequest(quando, "Check-up", status, PET_ID, VETERINARIO_ID, null);
    }
}
