package fiap.pethub.dto.response;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Texto sugerido pela IA. Não é gravado: o veterinário revisa e só então salva anamnese, receita e exames. */
public record RascunhoAnaliseResponse(
        @Schema(description = "Consulta analisada", example = "1") Long consultaId,
        @Schema(description = "Anamnese sugerida") String anamnese,
        @Schema(description = "Medicamentos que o veterinário prescreveu na consulta (vazio se nenhum)") List<MedicamentoPrescrito> medicamentos,
        @Schema(description = "Exames que o veterinário pediu na consulta (vazio se nenhum)") List<ExameSolicitado> exames,
        @Schema(description = "Sugestão de acompanhamento. Só aparece aqui: nunca é gravada") String sugestao,
        @Schema(description = "Modelo de IA que gerou o texto") String modelo) {
}
