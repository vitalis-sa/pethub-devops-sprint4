package fiap.pethub.ia;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescricaoTest {

    @Test
    void medicamentosVoltamIguaisDepoisDeGravados() {
        var itens = List.of(new MedicamentoPrescrito("A 5 mg", "1 caixa", "12/12h", "com comida"), new MedicamentoPrescrito("B", null, null, null));

        assertThat(Prescricao.medicamentos(Prescricao.escrever(itens))).isEqualTo(itens);
    }

    @Test
    void frequenciaEDuracaoSobrevivemAoGravarEForaDoLimiteViraNulo() {
        var itens = Prescricao.limparMedicamentos(List.of(
                new MedicamentoPrescrito("A", null, null, null, 2, 7),
                new MedicamentoPrescrito("B", null, null, null, 0, 400)));

        assertThat(itens.get(0).temAgenda()).isTrue();
        assertThat(Prescricao.medicamentos(Prescricao.escrever(itens)).getFirst().vezesPorDia()).isEqualTo(2);
        assertThat(itens.get(1).vezesPorDia()).isNull();
        assertThat(itens.get(1).duracaoDias()).isNull();
    }

    @Test
    void examesVoltamIguaisDepoisDeGravados() {
        var itens = List.of(new ExameSolicitado("Hemograma", "jejum"));

        assertThat(Prescricao.exames(Prescricao.escrever(itens))).isEqualTo(itens);
    }

    @Test
    void receitaAntigaEmTextoLivreViraUmItem() {
        var itens = Prescricao.medicamentos("Prednisolona 5 mg, duas vezes ao dia;");

        assertThat(itens).hasSize(1);
        assertThat(itens.getFirst().posologia()).isEqualTo("Prednisolona 5 mg, duas vezes ao dia;");
    }

    @Test
    void vazioOuNuloViraListaVazia() {
        assertThat(Prescricao.medicamentos(null)).isEmpty();
        assertThat(Prescricao.medicamentos(" ")).isEmpty();
        assertThat(Prescricao.exames(null)).isEmpty();
        assertThat(Prescricao.exames("[]")).isEmpty();
    }
}
