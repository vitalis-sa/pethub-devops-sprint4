package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranscricaoRequest(
        @Schema(description = "Transcrição da consulta, já convertida em texto pelo aplicativo",
                example = "Veterinário: Bom dia, o que o Rex apresentou? Responsável: Está sem apetite há dois dias...")
        @NotBlank(message = "Transcrição é obrigatória")
        @Size(max = 20000, message = "Transcrição deve ter no máximo 20000 caracteres")
        String transcricao) {
}
