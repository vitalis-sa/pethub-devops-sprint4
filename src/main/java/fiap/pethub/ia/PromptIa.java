package fiap.pethub.ia;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cada função de IA do PetHub: o que o modelo deve fazer e em que formato deve responder.
 *
 * <ul>
 *   <li><b>instrução</b>: as regras ficam em {@code src/main/resources/prompts/<arquivo>.txt}, em texto
 *       puro, para serem revisadas e ajustadas sem mexer em Java;</li>
 *   <li><b>mensagem do usuário</b>: o que veio do app (transcrição, texto do PDF) vai separado, dentro de
 *       uma tag. Assim o modelo sabe que aquilo é material a processar, não ordem a obedecer;</li>
 *   <li><b>campos</b>: o modelo é obrigado a devolver JSON com exatamente estes campos.</li>
 * </ul>
 */
@Getter
public enum PromptIa {

    ANALISE_CONSULTA("analise-consulta", "transcricao", "anamnese", "medicamentos", "exames", "sugestao"),
    RESUMO_EXAME("resumo-exame", "documento", "resumo");

    private final String instrucao;
    private final String tag;
    private final List<String> campos;

    PromptIa(String arquivo, String tag, String... campos) {
        this.instrucao = lerInstrucao(arquivo);
        this.tag = tag;
        this.campos = List.of(campos);
    }

    /** Embrulha o conteúdo na tag. Se ele trouxer a tag de fechamento, ela é removida para não "fechar" o bloco antes da hora. */
    public String mensagemDoUsuario(String conteudo) {
        String fechamento = "</" + tag + ">";
        return "<%s>%n%s%n%s".formatted(tag, conteudo.replace(fechamento, ""), fechamento);
    }

    private static String lerInstrucao(String arquivo) {
        try {
            return new ClassPathResource("prompts/" + arquivo + ".txt").getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt não encontrado: " + arquivo, e);
        }
    }

}
