package fiap.pethub.email;

import fiap.pethub.entity.EmailConsulta;
import fiap.pethub.enums.StatusEmail;
import fiap.pethub.repository.EmailConsultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Uma vez por dia, tenta de novo os e-mails de confirmação que falharam no dia anterior. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReenvioDeFalhasJob {

    private final EmailConsultaRepository emailRepository;
    private final ConfirmacaoDeConsultaService confirmacao;

    @Scheduled(cron = "${pethub.email.reenvio-cron:0 0 6 * * *}")
    public void reenviarFalhasDeOntem() {
        LocalDate hoje = LocalDate.now();
        List<EmailConsulta> falhas = emailRepository.findByStatusCriadosEntre(
                StatusEmail.FALHOU, hoje.minusDays(1).atStartOfDay(), hoje.atStartOfDay());
        log.info("Reenvio diário: {} e-mail(s) com falha ontem", falhas.size());
        falhas.forEach(falha -> confirmacao.enviar(falha.getConsultaId()));
    }
}
