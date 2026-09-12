package fiap.pethub.service;

import fiap.pethub.dto.request.DiagnosticoRequest;
import fiap.pethub.dto.request.PedidoMedicoRequest;
import fiap.pethub.dto.request.ProntuarioRequest;
import fiap.pethub.dto.response.ConsultaResponse;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.enums.TipoPedidoMedico;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.repository.DiagnosticoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O fechamento de uma consulta é o fluxo mais caro de errar: grava em três
 * lugares e não pode acontecer duas vezes.
 */
@ExtendWith(MockitoExtension.class)
class ProntuarioServiceTest {

    private static final Long CONSULTA_ID = 10L;
    private static final Long PET_ID = 7L;

    @Mock private ConsultaService consultaService;
    @Mock private DiagnosticoService diagnosticoService;
    @Mock private PedidoMedicoService pedidoMedicoService;
    @Mock private DiagnosticoRepository diagnosticoRepository;

    @InjectMocks private ProntuarioService prontuarioService;

    @Test
    @DisplayName("registra o diagnóstico e conclui a consulta")
    void registraDiagnosticoEConcluiConsulta() {
        consultaAberta();
        when(diagnosticoRepository.existsByConsultaId(CONSULTA_ID)).thenReturn(false);

        prontuarioService.registrar(CONSULTA_ID, prontuarioSemPedido());

        ArgumentCaptor<DiagnosticoRequest> diagnostico = ArgumentCaptor.forClass(DiagnosticoRequest.class);
        verify(diagnosticoService).create(diagnostico.capture());
        assertThat(diagnostico.getValue().getSintoma1()).isEqualTo("Letargia");
        assertThat(diagnostico.getValue().getPetId()).isEqualTo(PET_ID);
        verify(consultaService).concluir(CONSULTA_ID);
    }

    @Test
    @DisplayName("não emite pedido médico quando o veterinário não solicitou nada")
    void naoEmitePedidoQuandoNaoSolicitado() {
        consultaAberta();
        when(diagnosticoRepository.existsByConsultaId(CONSULTA_ID)).thenReturn(false);

        prontuarioService.registrar(CONSULTA_ID, prontuarioSemPedido());

        verify(pedidoMedicoService, never()).create(any());
    }

    @Test
    @DisplayName("emite o pedido médico quando tipo e descrição vieram preenchidos")
    void emitePedidoQuandoSolicitado() {
        consultaAberta();
        when(diagnosticoRepository.existsByConsultaId(CONSULTA_ID)).thenReturn(false);

        ProntuarioRequest request = prontuarioSemPedido();
        request.setTipo(TipoPedidoMedico.EXAME);
        request.setDescricao("Hemograma completo");

        prontuarioService.registrar(CONSULTA_ID, request);

        ArgumentCaptor<PedidoMedicoRequest> pedido = ArgumentCaptor.forClass(PedidoMedicoRequest.class);
        verify(pedidoMedicoService).create(pedido.capture());
        assertThat(pedido.getValue().getDescricao()).isEqualTo("Hemograma completo");
        assertThat(pedido.getValue().getConsultaId()).isEqualTo(CONSULTA_ID);
    }

    @Test
    @DisplayName("recusa um segundo prontuário na mesma consulta")
    void recusaProntuarioDuplicado() {
        consultaAberta();
        when(diagnosticoRepository.existsByConsultaId(CONSULTA_ID)).thenReturn(true);

        assertThatThrownBy(() -> prontuarioService.registrar(CONSULTA_ID, prontuarioSemPedido()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já foi registrado");

        verify(diagnosticoService, never()).create(any());
        verify(consultaService, never()).concluir(any());
    }

    @Test
    @DisplayName("recusa prontuário em consulta que já está realizada")
    void recusaConsultaJaRealizada() {
        when(consultaService.findById(CONSULTA_ID)).thenReturn(consulta(StatusConsulta.REALIZADA));

        assertThatThrownBy(() -> prontuarioService.registrar(CONSULTA_ID, prontuarioSemPedido()))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(diagnosticoService, never()).create(any());
    }

    private void consultaAberta() {
        when(consultaService.findById(CONSULTA_ID)).thenReturn(consulta(StatusConsulta.AGENDADA));
    }

    private ConsultaResponse consulta(StatusConsulta status) {
        return ConsultaResponse.builder()
                .id(CONSULTA_ID)
                .petId(PET_ID)
                .status(status)
                .build();
    }

    private ProntuarioRequest prontuarioSemPedido() {
        ProntuarioRequest request = new ProntuarioRequest();
        request.setSintoma1("Letargia");
        request.setDoencaPredita("Anemia");
        return request;
    }
}
