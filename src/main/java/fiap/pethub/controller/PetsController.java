package fiap.pethub.controller;

import fiap.pethub.dto.request.PetRequest;
import fiap.pethub.dto.response.DeleteResponse;
import fiap.pethub.dto.response.PetResponse;
import fiap.pethub.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import fiap.pethub.dto.request.ArquivoEnviadoRequest;
import fiap.pethub.dto.request.EnvioDeArquivoRequest;
import fiap.pethub.dto.response.UrlDeDownloadResponse;
import fiap.pethub.dto.response.UrlDeEnvioResponse;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Gerenciamento de pets")
public class PetsController {

    private final PetService service;

    @Operation(summary = "Listar pets", description = "Retorna lista paginada, filtrável por nome ou veterinário responsável")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")})
    @GetMapping
    public ResponseEntity<Page<PetResponse>> findAll(
            @Parameter(description = "Filtrar por nome") @RequestParam(required = false) String nome,
            @Parameter(description = "Filtrar por veterinário responsável") @RequestParam(required = false) Long veterinarioId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.findAll(nome, veterinarioId, pageable));
    }

    @Operation(summary = "Listar pets por CPF do responsável")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Responsável não encontrado")
    })
    @GetMapping("/responsavel")
    public ResponseEntity<Page<PetResponse>> findByResponsavelCpf(
            @Parameter(description = "CPF do responsável", required = true) @RequestParam String cpf,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.findByResponsavelCpf(cpf, pageable));
    }

    @Operation(summary = "Buscar pet por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet encontrado"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Cadastrar pet", description = "Busca o responsável pelo CPF e vincula ao pet")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pet criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Responsável ou veterinário não encontrado")
    })
    @PostMapping
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Atualizar pet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PetResponse> update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @Operation(summary = "Remover todos os pets de um responsável",
            description = "Chamado pelo serviço .NET ao excluir o responsável. Apaga também consultas, exames, pedidos, vacinas e a análise de cada pet.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Removidos (ou não havia pets)")})
    @DeleteMapping("/responsavel/{responsavelId}")
    public ResponseEntity<DeleteResponse> deleteByResponsavel(@PathVariable Long responsavelId) {
        return ResponseEntity.ok(service.deleteByResponsavel(responsavelId));
    }

    @Operation(summary = "Remover pet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }

    @Operation(summary = "Passo 1 da foto: URL assinada para enviar a imagem ao bucket",
            description = "Aceita image/jpeg, image/png e image/webp. Envie o arquivo com PUT na URL devolvida e confirme com PUT /api/pets/{id}/foto.")
    @PostMapping("/{id}/foto/envio")
    public ResponseEntity<UrlDeEnvioResponse> prepararEnvioDaFoto(@PathVariable Long id,
                                                                  @Valid @RequestBody EnvioDeArquivoRequest request) {
        return ResponseEntity.ok(service.prepararEnvioDaFoto(id, request.contentType()));
    }

    @Operation(summary = "Passo 2 da foto: confirma o envio e vincula a foto ao pet")
    @PutMapping("/{id}/foto")
    public ResponseEntity<PetResponse> confirmarFoto(@PathVariable Long id,
                                                     @Valid @RequestBody ArquivoEnviadoRequest request) {
        return ResponseEntity.ok(service.confirmarFoto(id, request.caminho()));
    }

    @Operation(summary = "URL temporária para baixar a foto do pet")
    @ApiResponses({@ApiResponse(responseCode = "404", description = "Pet sem foto")})
    @GetMapping("/{id}/foto")
    public ResponseEntity<UrlDeDownloadResponse> urlDaFoto(@PathVariable Long id) {
        return ResponseEntity.ok(service.urlDaFoto(id));
    }
}
