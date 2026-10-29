package fiap.pethub.armazenamento;

import fiap.pethub.dto.response.UrlDeDownloadResponse;
import fiap.pethub.dto.response.UrlDeEnvioResponse;
import fiap.pethub.exception.RegraDeNegocioException;

import java.util.UUID;

/**
 * Exames (PDF) e fotos de pets ficam fora do banco; o Java nunca recebe os bytes pelas rotas da API.
 * Ele só entrega URLs temporárias: o app envia e baixa direto do armazenamento, e o banco guarda apenas
 * o caminho do objeto. Quem sabe assinar a URL é cada implementação ({@link ArmazenamentoGcsService}
 * ou {@link ArmazenamentoLocalService}); as regras de caminho e de tipo de arquivo são comuns.
 */
public abstract class ArmazenamentoService {

    protected final ArmazenamentoProperties propriedades;

    protected ArmazenamentoService(ArmazenamentoProperties propriedades) {
        this.propriedades = propriedades;
    }

    /** Prepara um envio: gera o caminho novo (sempre único) e a URL para o PUT. */
    public UrlDeEnvioResponse prepararEnvio(PastaDoBucket pasta, String subpasta, String contentType) {
        if (!pasta.aceita(contentType)) {
            throw new RegraDeNegocioException(
                    "Tipo de arquivo não aceito. Use um destes: " + String.join(", ", pasta.tiposAceitos()));
        }
        String caminho = "%s/%s/%s.%s".formatted(pasta.prefixo(), subpasta, UUID.randomUUID(), pasta.extensaoDe(contentType));
        return new UrlDeEnvioResponse(caminho, urlDeEnvio(caminho, contentType), contentType,
                propriedades.validadeDaUrlEmMinutos());
    }

    public UrlDeDownloadResponse urlDeDownload(String caminho) {
        return new UrlDeDownloadResponse(urlDeLeitura(caminho), propriedades.validadeDaUrlEmMinutos());
    }

    /**
     * Confere que o caminho informado ao confirmar o envio pertence ao recurso certo. Sem isso, um
     * cliente poderia apontar o exame de um pet para o arquivo de outro.
     */
    public void exigirCaminhoDe(PastaDoBucket pasta, String subpasta, String caminho) {
        String prefixoEsperado = "%s/%s/".formatted(pasta.prefixo(), subpasta);
        if (!caminho.startsWith(prefixoEsperado) || caminho.contains("..")) {
            throw new RegraDeNegocioException("Caminho de arquivo inválido para este registro.");
        }
    }

    protected abstract String urlDeEnvio(String caminho, String contentType);

    protected abstract String urlDeLeitura(String caminho);
}
