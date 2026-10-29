package fiap.pethub.armazenamento;

import fiap.pethub.exception.ArmazenamentoIndisponivelException;
import fiap.pethub.exception.RegraDeNegocioException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Modo de desenvolvimento ({@code pethub.armazenamento.modo=local}): grava em disco e imita as URLs
 * assinadas do bucket com um HMAC. A rota {@code /api/arquivos/**} só aceita pedidos cuja assinatura
 * confere e não expirou, então o app usa o mesmo fluxo de produção sem conta do Google.
 */
@Service
@ConditionalOnProperty(name = "pethub.armazenamento.modo", havingValue = "local")
public class ArmazenamentoLocalService extends ArmazenamentoService {

    public static final String ROTA = "/api/arquivos/";
    private static final String ALGORITMO = "HmacSHA256";

    private final Path raiz;

    public ArmazenamentoLocalService(ArmazenamentoProperties propriedades) {
        super(propriedades);
        if (propriedades.segredoLocal() == null || propriedades.segredoLocal().isBlank()) {
            throw new IllegalStateException("Defina JWT_SECRET (ou ARMAZENAMENTO_SEGREDO) para assinar as URLs locais.");
        }
        this.raiz = Path.of(propriedades.pastaLocal()).toAbsolutePath().normalize();
    }

    @Override
    protected String urlDeEnvio(String caminho, String contentType) {
        return urlAssinada("PUT", caminho);
    }

    @Override
    protected String urlDeLeitura(String caminho) {
        return urlAssinada("GET", caminho);
    }

    /** A assinatura vale para um método e um caminho; trocar qualquer um dos dois invalida. */
    public boolean assinaturaValida(String metodo, String caminho, long expira, String assinatura) {
        if (expira < Instant.now().getEpochSecond() || assinatura == null) {
            return false;
        }
        return MessageDigest.isEqual(
                assinar(metodo, caminho, expira).getBytes(StandardCharsets.UTF_8),
                assinatura.getBytes(StandardCharsets.UTF_8));
    }

    public void gravar(String caminho, InputStream conteudo) {
        Path destino = resolver(caminho);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ArmazenamentoIndisponivelException("Não foi possível gravar o arquivo em disco.", e);
        }
    }

    public Resource ler(String caminho) {
        Path origem = resolver(caminho);
        if (!Files.isRegularFile(origem)) {
            throw new fiap.pethub.exception.ResourceNotFoundException("Arquivo não encontrado.");
        }
        return new FileSystemResource(origem);
    }

    private String urlAssinada(String metodo, String caminho) {
        long expira = Instant.now().plusSeconds(propriedades.validadeDaUrlEmMinutos() * 60L).getEpochSecond();
        return "%s%s%s?expira=%d&assinatura=%s".formatted(
                propriedades.urlPublica(), ROTA, caminho, expira, assinar(metodo, caminho, expira));
    }

    private String assinar(String metodo, String caminho, long expira) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(propriedades.segredoLocal().getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] assinatura = mac.doFinal("%s\n%s\n%d".formatted(metodo, caminho, expira).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(assinatura);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponível", e);
        }
    }

    /** Impede que um caminho com {@code ..} escape da pasta de arquivos. */
    private Path resolver(String caminho) {
        Path resolvido = raiz.resolve(caminho).normalize();
        if (!resolvido.startsWith(raiz)) {
            throw new RegraDeNegocioException("Caminho de arquivo inválido.");
        }
        return resolvido;
    }
}
