package fiap.pethub.email;

import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.EmailConsulta;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.integracao.ResponsavelClient;
import fiap.pethub.integracao.ResponsavelResumo;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.EmailConsultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

/**
 * Envia ao responsável o e-mail de confirmação da consulta, depois que ela foi gravada.
 *
 * Não há fila nem consumidor: o envio acontece direto, no fluxo normal. Se falhar, a falha fica
 * marcada em {@code TB_EMAIL_CONSULTA} e o {@link ReenvioDeFalhasJob} tenta de novo no dia seguinte.
 * A consulta nunca é desfeita por causa de e-mail.
 *
 * O envio fica fora de transação de propósito: o pool do banco tem só duas conexões e a Brevo
 * pode demorar segundos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmacaoDeConsultaService {

    private static final DateTimeFormatter DATA_E_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ConsultaRepository consultaRepository;
    private final EmailConsultaRepository emailRepository;
    private final ResponsavelClient responsavelClient;
    private final BrevoClient brevo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAgendar(ConsultaAgendada evento) {
        try {
            enviar(evento.consultaId());
        } catch (RuntimeException e) {
            // A consulta já está gravada: nenhuma falha daqui pode virar erro para quem agendou.
            log.error("Não foi possível registrar o envio do e-mail da consulta {}", evento.consultaId(), e);
        }
    }

    /** Envia (ou reenvia) a confirmação e grava o resultado. Nunca lança exceção. */
    public void enviar(Long consultaId) {
        EmailConsulta registro = emailRepository.findByConsultaId(consultaId)
                .orElseGet(() -> EmailConsulta.para(consultaId));
        if (registro.foiEnviado()) {
            return;
        }
        try {
            enviarConfirmacao(consultaId);
            registro.registrarEnvio();
            log.info("E-mail de confirmação enviado (consulta {})", consultaId);
        } catch (RuntimeException e) {
            registro.registrarFalha(e.getMessage());
            log.warn("E-mail de confirmação falhou (consulta {}): {}", consultaId, e.getMessage());
        }
        emailRepository.save(registro);
    }

    private void enviarConfirmacao(Long consultaId) {
        Consulta consulta = consultaRepository.findComDetalhesById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com id: " + consultaId));
        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new RegraDeNegocioException("Consulta cancelada: a confirmação não foi enviada.");
        }
        ResponsavelResumo responsavel = responsavelClient.buscarPorId(consulta.getPet().getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsável da consulta não encontrado"));
        brevo.enviar(responsavel.email(), responsavel.nome(), assunto(consulta), texto(consulta, responsavel));
    }

    private String assunto(Consulta consulta) {
        return "Consulta de %s agendada para %s".formatted(
                consulta.getPet().getNome(), consulta.getDataHora().format(DATA_E_HORA));
    }

    private String texto(Consulta consulta, ResponsavelResumo responsavel) {
        String local = consulta.getUnidade() == null ? "" : "\nLocal: " + consulta.getUnidade().getNome();
        return """
                Olá, %s!

                A consulta de %s foi agendada.

                Data: %s
                Veterinário: %s%s

                Se precisar remarcar ou cancelar, fale com a clínica.

                Equipe PetHub
                """.formatted(
                responsavel.nome(),
                consulta.getPet().getNome(),
                consulta.getDataHora().format(DATA_E_HORA),
                consulta.getVeterinario().getNome(),
                local);
    }
}
