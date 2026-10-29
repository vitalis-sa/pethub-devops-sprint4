package fiap.pethub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResumoExameResponse(
        @Schema(description = "Exame resumido", example = "10") Long exameId,
        @Schema(description = "Resumo gerado por IA, gravado no exame") String resumo) {
}
