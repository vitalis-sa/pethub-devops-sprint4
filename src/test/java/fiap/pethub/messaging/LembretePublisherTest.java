package fiap.pethub.messaging;

import fiap.pethub.enums.TipoLembrete;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LembretePublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final LembreteSolicitado lembrete = new LembreteSolicitado(
            7L, 5L, TipoLembrete.CONSULTA, LocalDate.of(2026, 11, 1), "Consulta agendada", 100L, "Consulta");

    private LembretePublisher publisher() {
        return new LembretePublisher(rabbitTemplate, jsonMapper);
    }

    @Test
    void publicaNaExchangeDeLembretesUmJsonComOsCamposDoDtoDoDotNet() {
        publisher().publicar(lembrete);

        ArgumentCaptor<Object> corpo = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.LEMBRETES_EXCHANGE), eq(RabbitConfig.LEMBRETES_ROUTING_KEY),
                corpo.capture(), any(MessagePostProcessor.class));
        assertThat(corpo.getValue().toString())
                .contains("\"responsavelId\":7", "\"petId\":5", "\"tipo\":\"CONSULTA\"",
                        "\"dataAgendada\":\"2026-11-01\"", "\"mensagem\":\"Consulta agendada\"",
                        "\"referenciaId\":100", "\"referenciaTipo\":\"Consulta\"");
    }

    @Test
    void marcaAMensagemComoJson() throws IOException {
        publisher().publicar(lembrete);

        ArgumentCaptor<MessagePostProcessor> processador = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class), processador.capture());
        Message mensagem = new Message(new byte[0], new MessageProperties());

        processador.getValue().postProcessMessage(mensagem);

        assertThat(mensagem.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @Test
    void falhaNoRabbitNaoDerrubaQuemGravouAConsulta() {
        doThrow(new AmqpConnectException(new IOException("fora do ar")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher().publicar(lembrete)).doesNotThrowAnyException();
    }
}
