package fiap.pethub.service;

import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.exception.RegraDeNegocioException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Remarcar e registrar queixas: só consulta ainda agendada, no futuro e com a agenda livre. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultaServiceRemarcacaoTest {

    private static final Long CONSULTA_ID = 10L;

    @Mock private ConsultaRepository repository;
    @Mock private PetRepository petRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private UnidadeVeterinarioRepository unidadeRepository;
    @Mock private ConsultaMapper mapper;
    @Mock private EscopoDoUsuario escopo;
    @Mock private ApplicationEventPublisher eventos;
    @Mock private AnaliseConsultaRepository analiseConsultaRepository;
    @Mock private EmailConsultaRepository emailConsultaRepository;
    @InjectMocks private ConsultaService service;

    private Consulta consulta;

    @BeforeEach
    void cenarioPadrao() {
        Pet pet = Pet.builder().id(5L).nome("Rex").responsavelId(7L).build();
        consulta = Consulta.builder().id(CONSULTA_ID).pet(pet).status(StatusConsulta.AGENDADA)
                .veterinario(Veterinario.builder().id(3L).build())
                .dataHora(LocalDateTime.now().plusDays(1)).build();
        when(repository.findById(CONSULTA_ID)).thenReturn(Optional.of(consulta));
        when(repository.save(any(Consulta.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(false);
    }

    @Test
    void remarcarMudaOHorarioEPublicaOLembreteDeNovo() {
        LocalDateTime novo = LocalDateTime.now().plusDays(3);

        service.remarcar(CONSULTA_ID, novo);

        assertThat(consulta.getDataHora()).isEqualTo(novo);
        verify(eventos).publishEvent(any(LembreteSolicitado.class));
    }

    @Test
    void remarcarParaOPassadoEhRecusado() {
        assertThatThrownBy(() -> service.remarcar(CONSULTA_ID, LocalDateTime.now().minusHours(1)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void remarcarParaHorarioOcupadoEhRecusado() {
        when(repository.existsByVeterinarioIdAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.remarcar(CONSULTA_ID, LocalDateTime.now().plusDays(3)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void consultaJaRealizadaNaoPodeSerRemarcadaNemReceberQueixas() {
        consulta.setStatus(StatusConsulta.REALIZADA);

        assertThatThrownBy(() -> service.remarcar(CONSULTA_ID, LocalDateTime.now().plusDays(3)))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.registrarQueixas(CONSULTA_ID, "coceira"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void queixasSaoGravadasSemEspacosNasPontas() {
        service.registrarQueixas(CONSULTA_ID, "  coçando a orelha  ");

        assertThat(consulta.getQueixas()).isEqualTo("coçando a orelha");
    }
}
