package fiap.pethub.armazenamento;

import fiap.pethub.exception.ArmazenamentoIndisponivelException;
import fiap.pethub.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArmazenamentoServiceTest {

    private static ArmazenamentoProperties propriedades(String bucket) {
        return new ArmazenamentoProperties("gcs", "projeto", bucket, 15, "./arquivos-locais", "http://localhost:8080", "segredo");
    }

    private final ArmazenamentoService service =
            new ArmazenamentoGcsService(propriedades("vitalis-docs"));

    @Test
    void aceitaCaminhoDentroDaSubpastaDoRegistro() {
        assertThatCode(() -> service.exigirCaminhoDe(PastaDoBucket.EXAMES, "10", "exames/10/abc.pdf"))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaCaminhoDeOutroRegistro() {
        assertThatThrownBy(() -> service.exigirCaminhoDe(PastaDoBucket.EXAMES, "10", "exames/11/abc.pdf"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void recusaCaminhoDeOutraPastaOuComTruqueDeDiretorio() {
        assertThatThrownBy(() -> service.exigirCaminhoDe(PastaDoBucket.FOTOS, "3", "exames/3/abc.pdf"))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.exigirCaminhoDe(PastaDoBucket.FOTOS, "3", "fotos/3/../4/abc.jpg"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void recusaTipoDeArquivoQueAPastaNaoAceita() {
        assertThatThrownBy(() -> service.prepararEnvio(PastaDoBucket.EXAMES, "10", "image/png"))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.prepararEnvio(PastaDoBucket.FOTOS, "3", "application/pdf"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void semBucketConfiguradoOServicoFicaIndisponivel() {
        var semBucket = new ArmazenamentoGcsService(propriedades(""));

        assertThatThrownBy(() -> semBucket.prepararEnvio(PastaDoBucket.FOTOS, "3", "image/png"))
                .isInstanceOf(ArmazenamentoIndisponivelException.class);
    }
}
