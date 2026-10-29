package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EnvioDeArquivoRequest(
        @Schema(description = "Tipo do arquivo que será enviado", example = "application/pdf")
        @NotBlank(message = "Tipo do arquivo é obrigatório")
        String contentType) {
}
