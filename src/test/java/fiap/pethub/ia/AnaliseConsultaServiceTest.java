package fiap.pethub.ia;

import fiap.pethub.dto.request.AprovarAnaliseRequest;
import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import fiap.pethub.dto.response.AnaliseConsultaResponse;
import fiap.pethub.dto.response.RascunhoAnaliseResponse;
import fiap.pethub.entity.AnaliseConsulta;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Pet;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.exception.AcessoNegadoException;
import fiap.pethub.exception.IaIndisponivelException;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.ia.AnaliseConsultaService.SaidaAnalise;
import fiap.pethub.repository.AnaliseConsultaRepository;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.security.EscopoDoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaliseConsultaServiceTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private AnaliseConsultaRepository repository;
    @Mock private GroqClient groq;
    @Mock private ProntuarioDaConsulta prontuario;
    @Mock private EscopoDoUsuario escopo;
    private AnaliseConsultaService service;

    @BeforeEach
    void montar() {
        service = new AnaliseConsultaService(consultaRepository, repository,
                Mappers.getMapper(AnaliseConsultaMapper.class), groq, prontuario, escopo);
    }

    private void consultaExistente(StatusConsulta status) {
        Pet pet = Pet.builder().id(1L).responsavelId(7L).build();
        when(consultaRepository.findById(5L)).thenReturn(Optional.of(Consulta.builder().id(5L).pet(pet).status(status).build()));
    }

    private void modeloResponde(SaidaAnalise saida) {
        when(groq.gerar(any(PromptIa.class), any(), any())).thenReturn(saida);
    }

    @Test
    void rascunhoDevolveAnamneseMedicamentosExamesESugestaoSemGravarNada() {
        consultaExistente(StatusConsulta.AGENDADA);
        when(groq.modelo()).thenReturn("modelo-x");
        modeloResponde(new SaidaAnalise("A",
                List.of(new MedicamentoPrescrito(" Prednisolona 5 mg ", "1 caixa", "12/12h", " "), new MedicamentoPrescrito(" ", null, null, null)),
                List.of(new ExameSolicitado("Hemograma", null)), "S"));

        RascunhoAnaliseResponse rascunho = service.gerarRascunho(5L, "transcrição");

        assertThat(rascunho.consultaId()).isEqualTo(5L);
        assertThat(rascunho.anamnese()).isEqualTo("A");
        assertThat(rascunho.medicamentos()).containsExactly(new MedicamentoPrescrito("Prednisolona 5 mg", "1 caixa", "12/12h", null));
        assertThat(rascunho.exames()).containsExactly(new ExameSolicitado("Hemograma", null));
        assertThat(rascunho.sugestao()).isEqualTo("S");
        assertThat(rascunho.modelo()).isEqualTo("modelo-x");
        verify(repository, never()).save(any());
    }

    @Test
    void consultaCanceladaNaoChamaOModelo() {
        consultaExistente(StatusConsulta.CANCELADA);

        assertThatThrownBy(() -> service.gerarRascunho(5L, "texto")).isInstanceOf(RegraDeNegocioException.class);

        verify(groq, never()).gerar(any(PromptIa.class), any(), any());
    }

    @Test
    void consultaJaRealizadaNaoPodeSerAtendidaDeNovo() {
        consultaExistente(StatusConsulta.REALIZADA);

        assertThatThrownBy(() -> service.gerarRascunho(5L, "texto")).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.aprovar(5L, new AprovarAnaliseRequest("a", List.of(), List.of(), null)))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(groq, never()).gerar(any(PromptIa.class), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void aprovarMarcaAConsultaComoRealizada() {
        consultaExistente(StatusConsulta.AGENDADA);
        when(repository.findByConsultaId(5L)).thenReturn(Optional.empty());
        when(repository.save(any(AnaliseConsulta.class))).thenAnswer(i -> i.getArgument(0));

        service.aprovar(5L, new AprovarAnaliseRequest("a", List.of(), List.of(), null));

        ArgumentCaptor<Consulta> salva = ArgumentCaptor.forClass(Consulta.class);
        verify(consultaRepository).save(salva.capture());
        assertThat(salva.getValue().getStatus()).isEqualTo(StatusConsulta.REALIZADA);
    }

    @Test
    void semPosseNaoChamaOModelo() {
        consultaExistente(StatusConsulta.AGENDADA);
        doThrow(new AcessoNegadoException("negado")).when(escopo).exigirPosse(7L);

        assertThatThrownBy(() -> service.gerarRascunho(5L, "texto")).isInstanceOf(AcessoNegadoException.class);

        verify(groq, never()).gerar(any(PromptIa.class), any(), any());
    }

    @Test
    void respostaIncompletaDoModeloViraErroDeIa() {
        consultaExistente(StatusConsulta.AGENDADA);
        modeloResponde(new SaidaAnalise("a", List.of(), List.of(), " "));

        assertThatThrownBy(() -> service.gerarRascunho(5L, "texto")).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void nadaPrescritoNemPedidoEhValido() {
        consultaExistente(StatusConsulta.AGENDADA);
        when(groq.modelo()).thenReturn("modelo-x");
        modeloResponde(new SaidaAnalise("A", null, null, "S"));

        RascunhoAnaliseResponse rascunho = service.gerarRascunho(5L, "transcrição");

        assertThat(rascunho.medicamentos()).isEmpty();
        assertThat(rascunho.exames()).isEmpty();
    }

    @Test
    void aprovarGravaAnamneseReceitaEExamesCorrigidos() {
        consultaExistente(StatusConsulta.AGENDADA);
        when(repository.findByConsultaId(5L)).thenReturn(Optional.empty());
        when(repository.save(any(AnaliseConsulta.class))).thenAnswer(i -> i.getArgument(0));

        AnaliseConsultaResponse resposta = service.aprovar(5L, new AprovarAnaliseRequest("anamnese corrigida",
                List.of(new MedicamentoPrescrito("Dipirona", "1 frasco", "10 gotas", "com água")),
                List.of(new ExameSolicitado("Ultrassom", "jejum")), "sugestão"));

        assertThat(resposta.consultaId()).isEqualTo(5L);
        assertThat(resposta.anamnese()).isEqualTo("anamnese corrigida");
        assertThat(resposta.medicamentos()).containsExactly(new MedicamentoPrescrito("Dipirona", "1 frasco", "10 gotas", "com água"));
        assertThat(resposta.exames()).containsExactly(new ExameSolicitado("Ultrassom", "jejum"));
        assertThat(resposta.aprovadaEm()).isNotNull();
        assertThat(resposta.sugestao()).isEqualTo("sugestão");
        verify(prontuario).refletir(any(Consulta.class), eq(resposta.medicamentos()), eq(resposta.exames()));
    }

    @Test
    void aprovarDeNovoSubstituiEmVezDeCriarOutra() {
        // Como no banco (consulta_id NOT NULL), a análise gravada sempre traz a consulta.
        AnaliseConsulta antiga = AnaliseConsulta.builder().id(99L).consulta(Consulta.builder().id(5L).build())
                .anamnese("velha").receita("velha").build();
        consultaExistente(StatusConsulta.AGENDADA);
        when(repository.findByConsultaId(5L)).thenReturn(Optional.of(antiga));
        when(repository.save(any(AnaliseConsulta.class))).thenAnswer(i -> i.getArgument(0));

        service.aprovar(5L, new AprovarAnaliseRequest("nova", List.of(), List.of(), null));

        verify(repository).save(antiga);
        assertThat(antiga.getId()).isEqualTo(99L);
        assertThat(antiga.getAnamnese()).isEqualTo("nova");
        assertThat(antiga.getReceita()).isEqualTo("[]");
        assertThat(antiga.getExames()).isEqualTo("[]");
    }

    @Test
    void receitaSemAprovacaoDevolveNaoEncontrado() {
        consultaExistente(StatusConsulta.AGENDADA);
        when(repository.findByConsultaId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receita(5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
