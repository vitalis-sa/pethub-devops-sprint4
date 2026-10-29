package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ArquivoEnviadoRequest(
        @Schema(description = "Caminho devolvido no passo de envio", example = "exames/10/3f1c.pdf")
        @NotBlank(message = "Caminho é obrigatório")
        String caminho) {
}
