package fiap.pethub.controller;

import fiap.pethub.dto.request.AprovarAnaliseRequest;
import fiap.pethub.dto.request.TextoDocumentoRequest;
import fiap.pethub.dto.request.TranscricaoRequest;
import fiap.pethub.dto.response.AnaliseConsultaResponse;
import fiap.pethub.dto.response.RascunhoAnaliseResponse;
import fiap.pethub.dto.response.ReceitaResponse;
import fiap.pethub.dto.response.ResumoExameResponse;
import fiap.pethub.ia.AnaliseConsultaService;
import fiap.pethub.ia.ResumoExameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Funções de IA. O aplicativo converte voz (reconhecimento do próprio aparelho/navegador) e PDF em texto; aqui chega só texto, nunca áudio.
 *
 * As rotas ficam sob {@code /api/consultas} e {@code /api/exames}. A análise
 * ({@code analise-ia}) é exclusiva do veterinário, regra declarada no {@code SecurityConfig};
 * o responsável só lê a receita já aprovada.
 */
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Inteligência Artificial", description = "Rascunho de anamnese e receita (só veterinário); receita aprovada; resumo de exames")
public class IaController {

    private final AnaliseConsultaService analiseService;
    private final ResumoExameService resumoService;

    @Operation(summary = "Gerar rascunho de anamnese, receita e sugestão a partir da transcrição",
            description = "Só veterinário. Nada é gravado: o veterinário revisa e salva com PUT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rascunho gerado"),
        @ApiResponse(responseCode = "400", description = "Transcrição ausente ou grande demais"),
        @ApiResponse(responseCode = "404", description = "Consulta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Consulta cancelada"),
        @ApiResponse(responseCode = "503", description = "Serviço de IA indisponível ou não configurado")
    })
    @PostMapping("/consultas/{id}/analise-ia")
    public ResponseEntity<RascunhoAnaliseResponse> gerarRascunho(@PathVariable Long id,
                                                                 @Valid @RequestBody TranscricaoRequest request) {
        return ResponseEntity.ok(analiseService.gerarRascunho(id, request.transcricao()));
    }

    @Operation(summary = "Salvar a anamnese e a receita aceitas ou corrigidas pelo veterinário",
            description = "Só veterinário. Grava anamnese, receita, exames e sugestão, reflete no prontuário do pet e marca a consulta como realizada. Consulta já realizada ou cancelada é recusada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anamnese e receita salvas"),
        @ApiResponse(responseCode = "400", description = "Texto ausente ou grande demais"),
        @ApiResponse(responseCode = "404", description = "Consulta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Consulta cancelada")
    })
    @PutMapping("/consultas/{id}/analise-ia")
    public ResponseEntity<AnaliseConsultaResponse> aprovarAnalise(@PathVariable Long id,
                                                                  @Valid @RequestBody AprovarAnaliseRequest request) {
        return ResponseEntity.ok(analiseService.aprovar(id, request));
    }

    @Operation(summary = "Buscar a anamnese e a receita salvas", description = "Só veterinário.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontradas"),
        @ApiResponse(responseCode = "404", description = "Consulta ou análise não encontrada")
    })
    @GetMapping("/consultas/{id}/analise-ia")
    public ResponseEntity<AnaliseConsultaResponse> buscarAnalise(@PathVariable Long id) {
        return ResponseEntity.ok(analiseService.buscar(id));
    }

    @Operation(summary = "Receita aprovada da consulta",
            description = "Veterinário e responsável do pet. Só existe depois que o veterinário salvou. O PDF é gerado no aplicativo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Receita encontrada"),
        @ApiResponse(responseCode = "204", description = "A consulta existe, mas a receita ainda não foi aprovada"),
        @ApiResponse(responseCode = "404", description = "Consulta não encontrada")
    })
    @GetMapping("/consultas/{id}/receita")
    public ResponseEntity<ReceitaResponse> buscarReceita(@PathVariable Long id) {
        return analiseService.receitaSeExistir(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Resumir o resultado de um exame a partir do texto do PDF",
            description = "O resumo fica gravado no exame e aparece em GET /api/exames como resumoIa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumo gerado e gravado"),
        @ApiResponse(responseCode = "400", description = "Texto ausente ou grande demais"),
        @ApiResponse(responseCode = "404", description = "Exame não encontrado"),
        @ApiResponse(responseCode = "503", description = "Serviço de IA indisponível ou não configurado")
    })
    @PostMapping("/exames/{id}/resumo-ia")
    public ResponseEntity<ResumoExameResponse> resumirExame(@PathVariable Long id,
                                                            @Valid @RequestBody TextoDocumentoRequest request) {
        return ResponseEntity.ok(resumoService.gerar(id, request.texto()));
    }
}
