package fiap.pethub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExameResponse {

    @Schema(description = "Identificador do exame", example = "1")
    private Long id;

    @Schema(description = "Tipo do exame", example = "Hemograma")
    private String tipo;

    @Schema(description = "Data do exame", example = "2026-06-01")
    private LocalDate data;

    @Schema(description = "Resultado do exame")
    private String resultado;

    @Schema(description = "Se o PDF do resultado já foi enviado; o link vem de GET /api/exames/{id}/arquivo")
    private boolean temArquivo;

    @Schema(description = "Resumo do resultado gerado por IA; vazio até ser gerado")
    private String resumoIa;

    @Schema(description = "Nome do pet", example = "Rex")
    private String nomePet;

    @Schema(description = "Consulta vinculada (de onde vem o pedido de exames)", example = "1")
    private Long consultaId;

    @Schema(description = "Data e hora da consulta vinculada", example = "2026-06-01T10:00:00")
    private LocalDateTime dataConsulta;
}


