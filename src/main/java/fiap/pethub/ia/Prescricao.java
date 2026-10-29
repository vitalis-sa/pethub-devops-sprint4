package fiap.pethub.ia;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Receita e exames são gravados como JSON (uma lista cada) nas colunas de texto longo da análise, sem tabelas novas.
 * Registros antigos, em que a receita era um texto livre, são lidos como um único item.
 */
final class Prescricao {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Prescricao() {
    }

    static String escrever(List<?> itens) {
        return JSON.writeValueAsString(itens == null ? List.of() : itens);
    }

    static List<MedicamentoPrescrito> medicamentos(String gravado) {
        if (gravado == null || gravado.isBlank()) {
            return List.of();
        }
        if (!gravado.stripLeading().startsWith("[")) {
            return List.of(new MedicamentoPrescrito("Prescrição", null, gravado.strip(), null));
        }
        return JSON.readValue(gravado, new TypeReference<List<MedicamentoPrescrito>>() { });
    }

    static List<ExameSolicitado> exames(String gravado) {
        if (gravado == null || gravado.isBlank()) {
            return List.of();
        }
        return JSON.readValue(gravado, new TypeReference<List<ExameSolicitado>>() { });
    }

    /** Tira espaços e descarta itens sem nome: o que o modelo devolve nem sempre vem limpo. */
    static List<MedicamentoPrescrito> limparMedicamentos(List<MedicamentoPrescrito> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
                .filter(m -> m != null && !vazio(m.nome()))
                .map(m -> new MedicamentoPrescrito(m.nome().strip(), aparar(m.quantidade()), aparar(m.posologia()), aparar(m.observacoes()),
                        dentroDoLimite(m.vezesPorDia(), MedicamentoPrescrito.MAXIMO_DOSES_POR_DIA),
                        dentroDoLimite(m.duracaoDias(), MedicamentoPrescrito.MAXIMO_DIAS)))
                .toList();
    }

    static List<ExameSolicitado> limparExames(List<ExameSolicitado> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
                .filter(e -> e != null && !vazio(e.nome()))
                .map(e -> new ExameSolicitado(e.nome().strip(), aparar(e.observacoes())))
                .toList();
    }

    /** O modelo pode devolver 0, negativo ou um absurdo: nesses casos a agenda fica em branco para o veterinário preencher. */
    private static Integer dentroDoLimite(Integer valor, int maximo) {
        return valor != null && valor >= 1 && valor <= maximo ? valor : null;
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String aparar(String texto) {
        return vazio(texto) ? null : texto.strip();
    }
}
