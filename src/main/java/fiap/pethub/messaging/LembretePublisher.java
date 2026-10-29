package fiap.pethub.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica na fila de lembretes, mas só depois que a gravação foi confirmada no banco.
 *
 * Publicar dentro da transação mandaria ao .NET um lembrete de uma consulta que um rollback
 * poderia desfazer. Se a publicação falhar, a consulta continua gravada e o erro vai para o log:
 * o pedido do usuário já foi atendido e não deve virar 500.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LembretePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicar(LembreteSolicitado lembrete) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.LEMBRETES_EXCHANGE,
                    RabbitConfig.LEMBRETES_ROUTING_KEY,
                    jsonMapper.writeValueAsString(lembrete),
                    this::marcarComoJson);
            log.info("Lembrete publicado: {} {}", lembrete.referenciaTipo(), lembrete.referenciaId());
        } catch (RuntimeException e) {
            log.error("Lembrete NÃO publicado ({} {}): {}", lembrete.referenciaTipo(), lembrete.referenciaId(), e.getMessage());
        }
    }

    private Message marcarComoJson(Message mensagem) {
        mensagem.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return mensagem;
    }
}
