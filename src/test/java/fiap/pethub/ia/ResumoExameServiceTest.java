package fiap.pethub.ia;

import fiap.pethub.dto.response.ResumoExameResponse;
import fiap.pethub.entity.Exame;
import fiap.pethub.entity.Pet;
import fiap.pethub.exception.AcessoNegadoException;
import fiap.pethub.exception.IaIndisponivelException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.ia.ResumoExameService.SaidaResumo;
import fiap.pethub.repository.ExameRepository;
import fiap.pethub.security.EscopoDoUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumoExameServiceTest {

    @Mock private ExameRepository repository;
    @Mock private GroqClient groq;
    @Mock private EscopoDoUsuario escopo;
    @InjectMocks private ResumoExameService service;

    private Exame exame() {
        Pet pet = Pet.builder().id(1L).responsavelId(7L).build();
        return Exame.builder().id(10L).pet(pet).build();
    }

    private void modeloResponde(String resumo) {
        when(groq.gerar(any(PromptIa.class), any(), any())).thenReturn(new SaidaResumo(resumo));
    }

    @Test
    void gravaOResumoNoExame() {
        Exame exame = exame();
        when(repository.findById(10L)).thenReturn(Optional.of(exame));
        modeloResponde("  Hemograma normal.  ");

        ResumoExameResponse resposta = service.gerar(10L, "texto do pdf");

        assertThat(resposta.exameId()).isEqualTo(10L);
        assertThat(resposta.resumo()).isEqualTo("Hemograma normal.");
        assertThat(exame.getResumoIa()).isEqualTo("Hemograma normal.");
        verify(repository).save(exame);
    }

    @Test
    void resumoLongoDemaisEhCortadoNoLimiteDaColuna() {
        when(repository.findById(10L)).thenReturn(Optional.of(exame()));
        modeloResponde("x".repeat(ResumoExameService.TAMANHO_MAXIMO + 500));

        assertThat(service.gerar(10L, "texto").resumo()).hasSize(ResumoExameService.TAMANHO_MAXIMO);
    }

    @Test
    void semPosseNaoChamaOModelo() {
        when(repository.findById(10L)).thenReturn(Optional.of(exame()));
        doThrow(new AcessoNegadoException("negado")).when(escopo).exigirPosse(7L);

        assertThatThrownBy(() -> service.gerar(10L, "texto")).isInstanceOf(AcessoNegadoException.class);

        verify(groq, never()).gerar(any(PromptIa.class), any(), any());
    }

    @Test
    void exameInexistente() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerar(10L, "texto")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resumoVazioNaoEGravado() {
        when(repository.findById(10L)).thenReturn(Optional.of(exame()));
        modeloResponde("  ");

        assertThatThrownBy(() -> service.gerar(10L, "texto")).isInstanceOf(IaIndisponivelException.class);

        verify(repository, never()).save(any());
    }
}
