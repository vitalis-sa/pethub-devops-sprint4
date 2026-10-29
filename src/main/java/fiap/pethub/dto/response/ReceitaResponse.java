package fiap.pethub.dto.response;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Receita e pedido de exames aprovados, com os dados do paciente, do tutor, do veterinário e da clínica
 * que aparecem no PDF. O PDF é montado no aplicativo; cada documento só é gerado se tiver itens.
 */
public record ReceitaResponse(
        @Schema(description = "Consulta", example = "1") Long consultaId,
        @Schema(description = "Nome do pet") String petNome,
        @Schema(description = "Espécie do pet") String petEspecie,
        @Schema(description = "Raça do pet") String petRaca,
        @Schema(description = "Gênero do pet") String petGenero,
        @Schema(description = "Nascimento do pet") LocalDate petDataNascimento,
        @Schema(description = "Peso do pet em kg") Double petPeso,
        @Schema(description = "Id do responsável no serviço de responsáveis") Long responsavelId,
        @Schema(description = "Nome do responsável (tutor)") String tutorNome,
        @Schema(description = "Nome do veterinário") String veterinarioNome,
        @Schema(description = "CRMV do veterinário") String veterinarioCrmv,
        @Schema(description = "Especialidade do veterinário") String veterinarioEspecialidade,
        @Schema(description = "Telefone do veterinário") String veterinarioTelefone,
        @Schema(description = "Nome da clínica/unidade") String unidadeNome,
        @Schema(description = "Endereço da clínica/unidade, em uma linha") String unidadeEndereco,
        @Schema(description = "Medicamentos da receita (vazio = sem receita)") List<MedicamentoPrescrito> medicamentos,
        @Schema(description = "Exames solicitados (vazio = sem pedido)") List<ExameSolicitado> exames,
        @Schema(description = "Quando o veterinário aprovou") LocalDateTime aprovadaEm) {
}
