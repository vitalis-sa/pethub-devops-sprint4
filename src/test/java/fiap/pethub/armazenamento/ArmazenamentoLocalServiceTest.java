package fiap.pethub.armazenamento;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArmazenamentoLocalServiceTest {

    @TempDir Path pasta;
    private ArmazenamentoLocalService service;

    @BeforeEach
    void criar() {
        service = new ArmazenamentoLocalService(
                new ArmazenamentoProperties("local", "p", "b", 15, pasta.toString(), "http://localhost:8080", "segredo-de-teste"));
    }

    @Test
    void urlDeEnvioTrazAssinaturaQueSoValeParaOMetodoECaminhoEmitidos() {
        String url = service.prepararEnvio(PastaDoBucket.FOTOS, "3", "image/png").url();

        String caminho = url.substring(url.indexOf("/api/arquivos/") + 14, url.indexOf('?'));
        long expira = Long.parseLong(url.replaceAll(".*expira=(\\d+).*", "$1"));
        String assinatura = url.substring(url.indexOf("assinatura=") + 11);

        assertThat(service.assinaturaValida("PUT", caminho, expira, assinatura)).isTrue();
        assertThat(service.assinaturaValida("GET", caminho, expira, assinatura)).isFalse();
        assertThat(service.assinaturaValida("PUT", caminho + "x", expira, assinatura)).isFalse();
    }

    @Test
    void assinaturaExpiradaEhRecusada() {
        assertThat(service.assinaturaValida("GET", "fotos/3/a.png", Instant.now().getEpochSecond() - 1, "qualquer")).isFalse();
    }

    @Test
    void gravaELeOArquivo() throws Exception {
        service.gravar("exames/1/a.pdf", new ByteArrayInputStream("pdf".getBytes(StandardCharsets.UTF_8)));

        assertThat(service.ler("exames/1/a.pdf").getContentAsByteArray()).isEqualTo("pdf".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void caminhoQueFogeDaPastaEhRecusado() {
        assertThatThrownBy(() -> service.gravar("../fora.txt", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(fiap.pethub.exception.RegraDeNegocioException.class);
    }
}
