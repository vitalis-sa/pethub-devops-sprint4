package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Um medicamento da receita: o que é, quanto, como usar e, se preciso, uma observação só dele. */
public record MedicamentoPrescrito(
        @Schema(description = "Medicamento com concentração e forma", example = "Prednisolona 5 mg, comprimido")
        @NotBlank(message = "Nome do medicamento é obrigatório")
        @Size(max = 300, message = "Nome do medicamento deve ter no máximo 300 caracteres") String nome,
        @Schema(description = "Quantidade a dispensar", example = "1 caixa")
        @Size(max = 100, message = "Quantidade deve ter no máximo 100 caracteres") String quantidade,
        @Schema(description = "Como usar: dose, via, frequência e duração", example = "1 comprimido por via oral a cada 12 horas por 5 dias")
        @Size(max = 1000, message = "Posologia deve ter no máximo 1000 caracteres") String posologia,
        @Schema(description = "Observação deste medicamento", example = "Dar junto com a comida")
        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres") String observacoes,
        @Schema(description = "Doses por dia; com a duração, gera os lembretes de cada dose", example = "2")
        @Min(value = 1, message = "Doses por dia deve ser de 1 a 24") @Max(value = 24, message = "Doses por dia deve ser de 1 a 24") Integer vezesPorDia,
        @Schema(description = "Duração do tratamento em dias", example = "7")
        @Min(value = 1, message = "Duração deve ser de 1 a 365 dias") @Max(value = 365, message = "Duração deve ser de 1 a 365 dias") Integer duracaoDias) {

    public static final int MAXIMO_DOSES_POR_DIA = 24;
    public static final int MAXIMO_DIAS = 365;

    /** Medicamento sem frequência e duração definidas: não gera lembretes de dose. */
    public MedicamentoPrescrito(String nome, String quantidade, String posologia, String observacoes) {
        this(nome, quantidade, posologia, observacoes, null, null);
    }

    /** Só gera lembretes quando se sabe quantas vezes por dia e por quantos dias. */
    public boolean temAgenda() {
        return vezesPorDia != null && duracaoDias != null;
    }
}
