package fiap.pethub.ia;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Exame;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.VacinaTratamento;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.TipoVacinaTratamento;
import fiap.pethub.repository.ExameRepository;
import fiap.pethub.repository.VacinaTratamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import fiap.pethub.messaging.LembreteSolicitado;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioDaConsultaTest {

    @Mock private VacinaTratamentoRepository tratamentos;
    @Mock private ExameRepository exames;
    @Mock private ApplicationEventPublisher eventos;
    private ProntuarioDaConsulta prontuario;
    private Consulta consulta;

    @BeforeEach
    void montar() {
        prontuario = new ProntuarioDaConsulta(tratamentos, exames, eventos);
        consulta = Consulta.builder().id(5L).pet(Pet.builder().id(1L).build()).veterinario(Veterinario.builder().id(2L).build()).build();
    }

    @Test
    void cadaMedicamentoViraUmTratamentoEcadaExameViraUmExameSemResultado() {
        when(tratamentos.findAllByConsultaIdAndTipo(5L, TipoVacinaTratamento.MEDICAMENTO)).thenReturn(List.of());
        when(exames.findAllByConsultaId(5L)).thenReturn(List.of());

        prontuario.refletir(consulta,
                List.of(new MedicamentoPrescrito("Prednisolona 5 mg", "1 caixa", "12/12h", "com comida")),
                List.of(new ExameSolicitado("Hemograma", "jejum")));

        ArgumentCaptor<VacinaTratamento> tratamento = ArgumentCaptor.forClass(VacinaTratamento.class);
        verify(tratamentos).save(tratamento.capture());
        assertThat(tratamento.getValue().getTipo()).isEqualTo(TipoVacinaTratamento.MEDICAMENTO);
        assertThat(tratamento.getValue().getNome()).isEqualTo("Prednisolona 5 mg");
        assertThat(tratamento.getValue().getDose()).isEqualTo("12/12h");
        assertThat(tratamento.getValue().getObservacoes()).isEqualTo("Qtd.: 1 caixa · com comida");

        ArgumentCaptor<Exame> exame = ArgumentCaptor.forClass(Exame.class);
        verify(exames).save(exame.capture());
        assertThat(exame.getValue().getTipo()).isEqualTo("Hemograma");
        assertThat(exame.getValue().getResultado()).isNull();
        assertThat(exame.getValue().getConsulta()).isSameAs(consulta);
    }

    @Test
    void medicamentoComFrequenciaEDuracaoPedeOsLembretesDeDose() {
        when(tratamentos.findAllByConsultaIdAndTipo(5L, TipoVacinaTratamento.MEDICAMENTO)).thenReturn(List.of());
        when(exames.findAllByConsultaId(5L)).thenReturn(List.of());
        consulta.getPet().setResponsavelId(9L);
        when(tratamentos.save(any(VacinaTratamento.class))).thenAnswer(i -> {
            VacinaTratamento t = i.getArgument(0);
            t.setId(30L);
            return t;
        });

        prontuario.refletir(consulta, List.of(new MedicamentoPrescrito("Prednisolona", null, "1 comp.", null, 2, 7)), List.of());

        ArgumentCaptor<LembreteSolicitado> pedido = ArgumentCaptor.forClass(LembreteSolicitado.class);
        verify(eventos).publishEvent(pedido.capture());
        assertThat(pedido.getValue().responsavelId()).isEqualTo(9L);
        assertThat(pedido.getValue().vezesPorDia()).isEqualTo(2);
        assertThat(pedido.getValue().duracaoDias()).isEqualTo(7);
        assertThat(pedido.getValue().referenciaTipo()).isEqualTo("VacinaTratamento");
        assertThat(pedido.getValue().referenciaId()).isEqualTo(30L);
        assertThat(pedido.getValue().inicio()).isNotNull();
    }

    @Test
    void medicamentoSemFrequenciaOuDuracaoNaoGeraLembrete() {
        when(tratamentos.findAllByConsultaIdAndTipo(5L, TipoVacinaTratamento.MEDICAMENTO)).thenReturn(List.of());
        when(exames.findAllByConsultaId(5L)).thenReturn(List.of());

        prontuario.refletir(consulta, List.of(new MedicamentoPrescrito("A", null, null, null, 2, null),
                new MedicamentoPrescrito("B", null, null, null)), List.of());

        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void salvarDeNovoNaoDuplicaEntraTiraOMedicamentoRemovido() {
        VacinaTratamento mantido = VacinaTratamento.builder().id(1L).nome("Dipirona").dose("velha").build();
        VacinaTratamento removido = VacinaTratamento.builder().id(2L).nome("Tramadol").build();
        when(tratamentos.findAllByConsultaIdAndTipo(5L, TipoVacinaTratamento.MEDICAMENTO)).thenReturn(List.of(mantido, removido));
        when(exames.findAllByConsultaId(5L)).thenReturn(List.of(Exame.builder().tipo("HEMOGRAMA").build()));

        prontuario.refletir(consulta, List.of(new MedicamentoPrescrito("dipirona", null, "nova", null)), List.of(new ExameSolicitado("Hemograma", null)));

        verify(tratamentos).deleteAll(List.of(removido));
        verify(tratamentos).save(mantido);
        assertThat(mantido.getDose()).isEqualTo("nova");
        verify(exames, never()).save(any());
    }
}
