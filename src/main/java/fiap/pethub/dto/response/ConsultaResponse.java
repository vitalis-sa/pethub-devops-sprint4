package fiap.pethub.dto.response;

import fiap.pethub.enums.StatusConsulta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaResponse {

    @Schema(description = "ID da consulta", example = "1")
    private Long id;

    @Schema(description = "Data e hora", example = "2026-06-01T10:00:00")
    private LocalDateTime dataHora;

    @Schema(description = "Observacoes")
    private String observacoes;

    @Schema(description = "Queixas relatadas pelo responsável antes da consulta")
    private String queixas;

    @Schema(description = "Status", example = "AGENDADA")
    private StatusConsulta status;

    @Schema(description = "Nome do pet", example = "Rex")
    private String nomePet;

    @Schema(description = "ID do pet", example = "1")
    private Long petId;

    @Schema(description = "Nome do veterinario", example = "Dr. Carlos Souza")
    private String nomeVeterinario;

    @Schema(description = "ID do veterinario", example = "3")
    private Long veterinarioId;

    @Schema(description = "Nome da unidade veterinaria", example = "Clinica PetVida")
    private String nomeUnidade;

}