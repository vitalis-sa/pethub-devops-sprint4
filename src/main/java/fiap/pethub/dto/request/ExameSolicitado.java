package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Um exame pedido pelo veterinário, com observação opcional (preparo, indicação clínica). */
public record ExameSolicitado(
        @Schema(description = "Nome do exame", example = "Hemograma completo")
        @NotBlank(message = "Nome do exame é obrigatório")
        @Size(max = 300, message = "Nome do exame deve ter no máximo 300 caracteres") String nome,
        @Schema(description = "Observação deste exame", example = "Jejum de 8 horas")
        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres") String observacoes) {
}
