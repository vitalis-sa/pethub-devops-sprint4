package fiap.pethub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UrlDeDownloadResponse(
        @Schema(description = "URL temporária para baixar o arquivo") String url,
        @Schema(description = "Minutos até a URL expirar", example = "15") int expiraEmMinutos) {
}
