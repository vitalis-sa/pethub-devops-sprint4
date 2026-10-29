package fiap.pethub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponse {

    @Schema(description = "ID do pet", example = "1")
    private Long id;

    @Schema(description = "Nome do pet", example = "Rex")
    private String nome;

    @Schema(description = "Espécie", example = "Cão")
    private String especie;

    @Schema(description = "Raça", example = "Labrador")
    private String raca;

    @Schema(description = "Data de nascimento", example = "2022-05-10")
    private LocalDate dataNascimento;

    @Schema(description = "Idade em anos completos, calculada a partir da data de nascimento", example = "3")
    private Integer idade;

    @Schema(description = "Peso em kg", example = "25.5")
    private Double peso;

    @Schema(description = "Gênero", example = "Macho")
    private String genero;

    @Schema(description = "Id do responsável no serviço de responsáveis", example = "7")
    private Long responsavelId;

    @Schema(description = "Nome do responsável (copiado no cadastro do pet)", example = "João Silva")
    private String nomeResponsavel;

    @Schema(description = "Se o pet já tem foto; o link vem de GET /api/pets/{id}/foto")
    private boolean temFoto;

    @Schema(description = "Nome do veterinário responsável", example = "Dr. Carlos Souza")
    private String nomeVeterinarioResponsavel;
}

