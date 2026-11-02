package fiap.pethub.email;

import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.EmailConsulta;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.UnidadeVeterinario;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.enums.StatusEmail;
import fiap.pethub.exception.ServicoIndisponivelException;
import fiap.pethub.integracao.ResponsavelClient;
import fiap.pethub.integracao.ResponsavelResumo;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.EmailConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoDeConsultaServiceTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private EmailConsultaRepository emailRepository;
    @Mock private ResponsavelClient responsavelClient;
    @Mock private BrevoClient brevo;
    @InjectMocks private ConfirmacaoDeConsultaService service;

    private void consultaAgendada(StatusConsulta status) {
        Consulta consulta = Consulta.builder()
                .id(100L).status(status)
                .dataHora(LocalDateTime.of(2026, 11, 1, 14, 30))
                .pet(Pet.builder().id(5L).nome("Rex").responsavelId(7L).build())
                .veterinario(Veterinario.builder().nome("Dra. Ana").build())
                .unidade(UnidadeVeterinario.builder().nome("Clínica Centro").build())
                .build();
        when(consultaRepository.findComDetalhesById(100L)).thenReturn(Optional.of(consulta));
    }

    private void responsavelExiste() {
        when(responsavelClient.buscarPorId(7L)).thenReturn(Optional.of(new ResponsavelResumo(7L, "João", "joao@email.com", "123")));
    }

    private EmailConsulta gravado() {
        ArgumentCaptor<EmailConsulta> registro = ArgumentCaptor.forClass(EmailConsulta.class);
        verify(emailRepository).save(registro.capture());
        return registro.getValue();
    }

    @Test
    void enviaOEmailEMarcaComoEnviado() {
        consultaAgendada(StatusConsulta.AGENDADA);
        responsavelExiste();
        when(emailRepository.findByConsultaId(100L)).thenReturn(Optional.empty());

        service.enviar(100L);

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(brevo).enviar(eq("joao@email.com"), eq("João"), contains("Rex"), texto.capture());
        assertThat(texto.getValue()).contains("Olá, João!", "Rex", "01/11/2026 às 14:30", "Dra. Ana", "Clínica Centro");
        assertThat(gravado().getStatus()).isEqualTo(StatusEmail.ENVIADO);
    }

    @Test
    void falhaDoEnvioFicaMarcadaParaOReenvioDiario() {
        consultaAgendada(StatusConsulta.AGENDADA);
        responsavelExiste();
        when(emailRepository.findByConsultaId(100L)).thenReturn(Optional.empty());
        doThrow(new ServicoIndisponivelException("Brevo fora do ar")).when(brevo).enviar(anyString(), anyString(), anyString(), anyString());

        assertThatCode(() -> service.enviar(100L)).doesNotThrowAnyException();

        EmailConsulta registro = gravado();
        assertThat(registro.getStatus()).isEqualTo(StatusEmail.FALHOU);
        assertThat(registro.getTentativas()).isEqualTo(1);
        assertThat(registro.getUltimoErro()).isEqualTo("Brevo fora do ar");
    }

    @Test
    void servicoDeResponsaveisForaDoArTambemViraFalha() {
        consultaAgendada(StatusConsulta.AGENDADA);
        when(emailRepository.findByConsultaId(100L)).thenReturn(Optional.empty());
        when(responsavelClient.buscarPorId(7L)).thenThrow(new ServicoIndisponivelException("sem resposta"));

        service.enviar(100L);

        assertThat(gravado().getStatus()).isEqualTo(StatusEmail.FALHOU);
        verify(brevo, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void emailJaEnviadoNaoEhEnviadoDeNovo() {
        EmailConsulta jaEnviado = EmailConsulta.para(100L);
        jaEnviado.registrarEnvio();
        when(emailRepository.findByConsultaId(100L)).thenReturn(Optional.of(jaEnviado));

        service.enviar(100L);

        verify(brevo, never()).enviar(anyString(), anyString(), anyString(), anyString());
        verify(emailRepository, never()).save(any());
    }

    @Test
    void reenvioDeUmaFalhaAnteriorSomaTentativaEMarcaEnviado() {
        EmailConsulta falhou = EmailConsulta.para(100L);
        falhou.registrarFalha("erro antigo");
        consultaAgendada(StatusConsulta.AGENDADA);
        responsavelExiste();
        when(emailRepository.findByConsultaId(100L)).thenReturn(Optional.of(falhou));

        service.enviar(100L);

        assertThat(falhou.getStatus()).isEqualTo(StatusEmail.ENVIADO);
        assertThat(falhou.getTentativas()).isEqualTo(2);
        assertThat(falhou.getUltimoErro()).isNull();
    }

    @Test
    void consultaCanceladaNaoRecebeConfirmacao() {
        consultaAgendada(StatusConsulta.CANCELADA);
        when(emailRepository.findByConsultaId(100L)).thenReturn(Optional.empty());

        service.enviar(100L);

        verify(brevo, never()).enviar(anyString(), anyString(), anyString(), anyString());
        assertThat(gravado().getStatus()).isEqualTo(StatusEmail.FALHOU);
    }
}
