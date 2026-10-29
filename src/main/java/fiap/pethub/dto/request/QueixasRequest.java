package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueixasRequest(
        @Schema(description = "O que o responsável observou no pet", example = "Está coçando a orelha esquerda")
        @NotBlank(message = "Queixas são obrigatórias")
        @Size(max = 2000, message = "Queixas devem ter no máximo 2000 caracteres")
        String queixas) {
}
