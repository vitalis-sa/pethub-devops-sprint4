package fiap.pethub.ia;

import fiap.pethub.dto.response.ResumoExameResponse;
import fiap.pethub.entity.Exame;
import fiap.pethub.exception.IaIndisponivelException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.repository.ExameRepository;
import fiap.pethub.security.EscopoDoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Resume o texto de um exame em poucas frases e guarda o resumo no próprio exame, para ninguém
 * precisar abrir o PDF a cada consulta. Como a análise da consulta, não é transacional.
 */
@Service
@RequiredArgsConstructor
public class ResumoExameService {

    /** Tamanho da coluna {@code resumo_ia}. */
    static final int TAMANHO_MAXIMO = 2000;

    private final ExameRepository repository;
    private final GroqClient groq;
    private final EscopoDoUsuario escopo;

    /** Formato que o modelo devolve, conforme o esquema de {@link PromptIa#RESUMO_EXAME}. */
    public record SaidaResumo(String resumo) {
    }

    @CacheEvict(value = "exames", allEntries = true)
    public ResumoExameResponse gerar(Long exameId, String texto) {
        Exame exame = repository.findById(exameId)
                .orElseThrow(() -> new ResourceNotFoundException("Exame não encontrado com id: " + exameId));
        escopo.exigirPosse(exame.getPet().getResponsavelId());

        SaidaResumo saida = groq.gerar(PromptIa.RESUMO_EXAME, texto, SaidaResumo.class);
        if (saida.resumo() == null || saida.resumo().isBlank()) {
            throw new IaIndisponivelException("A resposta do serviço de IA veio incompleta.");
        }

        exame.setResumoIa(limitar(saida.resumo().strip()));
        repository.save(exame);
        return new ResumoExameResponse(exameId, exame.getResumoIa());
    }

    private String limitar(String resumo) {
        return resumo.length() <= TAMANHO_MAXIMO ? resumo : resumo.substring(0, TAMANHO_MAXIMO - 1) + "…";
    }
}
