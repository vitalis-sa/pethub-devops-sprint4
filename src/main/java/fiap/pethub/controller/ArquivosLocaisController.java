package fiap.pethub.controller;

import fiap.pethub.armazenamento.ArmazenamentoLocalService;
import fiap.pethub.exception.AcessoNegadoException;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Faz o papel do bucket no modo local. Não usa o login: quem autoriza é a assinatura da URL, que o
 * pethub-java só emite depois de conferir quem é o usuário (veja {@code ArmazenamentoLocalService}).
 */
@Hidden
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pethub.armazenamento.modo", havingValue = "local")
public class ArquivosLocaisController {

    private final ArmazenamentoLocalService armazenamento;

    @PutMapping("/api/arquivos/**")
    public ResponseEntity<Void> receber(HttpServletRequest request,
                                        @RequestParam long expira,
                                        @RequestParam String assinatura) throws IOException {
        String caminho = caminhoDe(request);
        exigirAssinatura("PUT", caminho, expira, assinatura);
        armazenamento.gravar(caminho, request.getInputStream());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/arquivos/**")
    public ResponseEntity<Resource> enviar(HttpServletRequest request,
                                           @RequestParam long expira,
                                           @RequestParam String assinatura) {
        String caminho = caminhoDe(request);
        exigirAssinatura("GET", caminho, expira, assinatura);
        return ResponseEntity.ok().contentType(tipoDe(caminho)).body(armazenamento.ler(caminho));
    }

    private String caminhoDe(HttpServletRequest request) {
        return request.getRequestURI().substring(ArmazenamentoLocalService.ROTA.length());
    }

    private void exigirAssinatura(String metodo, String caminho, long expira, String assinatura) {
        if (!armazenamento.assinaturaValida(metodo, caminho, expira, assinatura)) {
            throw new AcessoNegadoException("Link de arquivo inválido ou expirado.");
        }
    }

    private MediaType tipoDe(String caminho) {
        String minusculo = caminho.toLowerCase();
        if (minusculo.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (minusculo.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (minusculo.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (minusculo.endsWith(".jpg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
