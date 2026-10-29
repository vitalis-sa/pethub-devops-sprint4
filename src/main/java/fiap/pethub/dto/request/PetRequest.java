package fiap.pethub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PetRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Espécie é obrigatória")
    private String especie;

    private String raca;

    @Schema(description = "Data de nascimento (aproximada, se não for conhecida)", example = "2022-05-10")
    @PastOrPresent(message = "Data de nascimento não pode ser futura")
    private LocalDate dataNascimento;


    private Double peso;
    private String genero;

    @Schema(description = "CPF do responsável. Obrigatório para o veterinário; o responsável cadastra sempre para si e este campo é ignorado.",
            example = "12345678901")
    private String responsavelCpf;

    private Long veterinarioResponsavelId;
}
