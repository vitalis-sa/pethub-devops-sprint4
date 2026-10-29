package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RemarcarConsultaRequest(
        @Schema(description = "Nova data e hora", example = "2026-11-10T14:30:00")
        @NotNull(message = "Nova data e hora são obrigatórias")
        LocalDateTime dataHora) {
}
