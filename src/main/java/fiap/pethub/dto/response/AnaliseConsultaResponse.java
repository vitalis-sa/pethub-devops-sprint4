package fiap.pethub.dto.response;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Anamnese, receita, pedido de exames e sugestão aprovados pelo veterinário. Visível só a ele. */
public record AnaliseConsultaResponse(
        @Schema(description = "Consulta", example = "1") Long consultaId,
        @Schema(description = "Anamnese aprovada") String anamnese,
        @Schema(description = "Medicamentos da receita (vazio = sem receita)") List<MedicamentoPrescrito> medicamentos,
        @Schema(description = "Exames solicitados (vazio = sem pedido)") List<ExameSolicitado> exames,
        @Schema(description = "Sugestão de acompanhamento da IA aprovada (pode ser vazia)") String sugestao,
        @Schema(description = "Quando o veterinário salvou", example = "2026-06-01T10:30:00") LocalDateTime aprovadaEm) {
}
