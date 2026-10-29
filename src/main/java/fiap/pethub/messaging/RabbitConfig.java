package fiap.pethub.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia da fila de lembretes: exchange direta -> routing key -> fila durável.
 *
 * O Java só produz; quem consome é o .NET. Mesmo assim o Java declara a fila: o RabbitMQ descarta
 * sem aviso uma mensagem publicada numa exchange sem fila ligada, e o Java pode publicar antes de
 * o .NET ter subido pela primeira vez. A declaração é idempotente, então o .NET pode repeti-la.
 */
@Configuration
public class RabbitConfig {

    public static final String LEMBRETES_EXCHANGE = "pethub.lembretes.exchange";
    public static final String LEMBRETES_ROUTING_KEY = "lembrete.criar";
    public static final String LEMBRETES_QUEUE = "pethub.lembretes.queue";

    @Bean
    DirectExchange lembretesExchange() {
        return new DirectExchange(LEMBRETES_EXCHANGE, true, false);
    }

    @Bean
    Queue lembretesQueue() {
        return QueueBuilder.durable(LEMBRETES_QUEUE).build();
    }

    @Bean
    Binding lembretesBinding(Queue lembretesQueue, DirectExchange lembretesExchange) {
        return BindingBuilder.bind(lembretesQueue).to(lembretesExchange).with(LEMBRETES_ROUTING_KEY);
    }
}
