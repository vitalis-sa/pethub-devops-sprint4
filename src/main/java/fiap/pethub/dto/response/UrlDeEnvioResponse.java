package fiap.pethub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UrlDeEnvioResponse(
        @Schema(description = "Caminho do objeto no bucket; devolva-o ao confirmar o envio") String caminho,
        @Schema(description = "URL para enviar o arquivo com PUT, usando o mesmo Content-Type") String url,
        @Schema(description = "Content-Type que o PUT deve usar", example = "application/pdf") String contentType,
        @Schema(description = "Minutos até a URL expirar", example = "15") int expiraEmMinutos) {
}
