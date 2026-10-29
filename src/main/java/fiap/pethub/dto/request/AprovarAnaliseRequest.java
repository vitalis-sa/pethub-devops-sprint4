package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Anamnese, receita de medicamentos e pedido de exames já revisados (aceitos ou corrigidos) pelo veterinário.
 * As duas listas são opcionais: lista vazia (ou ausente) significa que aquele documento não existe.
 */
public record AprovarAnaliseRequest(
        @Schema(description = "Anamnese final") @NotBlank(message = "Anamnese é obrigatória")
        @Size(max = 20000, message = "Anamnese deve ter no máximo 20000 caracteres") String anamnese,
        @Schema(description = "Medicamentos da receita; vazio = sem receita")
        @Size(max = 30, message = "A receita aceita no máximo 30 medicamentos") List<@Valid MedicamentoPrescrito> medicamentos,
        @Schema(description = "Exames solicitados; vazio = sem pedido de exames")
        @Size(max = 30, message = "O pedido aceita no máximo 30 exames") List<@Valid ExameSolicitado> exames,
        @Schema(description = "Sugestão de acompanhamento da IA (opcional)")
        @Size(max = 20000, message = "Sugestão deve ter no máximo 20000 caracteres") String sugestao) {

    public AprovarAnaliseRequest {
        medicamentos = medicamentos == null ? List.of() : List.copyOf(medicamentos);
        exames = exames == null ? List.of() : List.copyOf(exames);
    }
}
