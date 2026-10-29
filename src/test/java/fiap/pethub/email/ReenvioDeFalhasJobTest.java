package fiap.pethub.email;

import fiap.pethub.entity.EmailConsulta;
import fiap.pethub.enums.StatusEmail;
import fiap.pethub.repository.EmailConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReenvioDeFalhasJobTest {

    @Mock private EmailConsultaRepository emailRepository;
    @Mock private ConfirmacaoDeConsultaService confirmacao;
    @InjectMocks private ReenvioDeFalhasJob job;

    @Test
    void reenviaApenasAsFalhasDeOntem() {
        when(emailRepository.findByStatusCriadosEntre(eq(StatusEmail.FALHOU), any(), any()))
                .thenReturn(List.of(EmailConsulta.para(10L), EmailConsulta.para(11L)));

        job.reenviarFalhasDeOntem();

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(emailRepository).findByStatusCriadosEntre(eq(StatusEmail.FALHOU), inicio.capture(), fim.capture());
        assertThat(fim.getValue()).isEqualTo(LocalDate.now().atStartOfDay());
        assertThat(inicio.getValue()).isEqualTo(LocalDate.now().minusDays(1).atStartOfDay());
        verify(confirmacao).enviar(10L);
        verify(confirmacao).enviar(11L);
    }
}
