package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextoDocumentoRequest(
        @Schema(description = "Texto extraído do PDF do exame pelo aplicativo",
                example = "Hemograma completo. Hemácias 6,2 milhões/mm3 (ref. 5,5 a 8,5)...")
        @NotBlank(message = "Texto do documento é obrigatório")
        @Size(max = 30000, message = "Texto do documento deve ter no máximo 30000 caracteres")
        String texto) {
}
