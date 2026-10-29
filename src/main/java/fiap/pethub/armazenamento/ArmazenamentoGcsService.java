package fiap.pethub.armazenamento;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import fiap.pethub.exception.ArmazenamentoIndisponivelException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/** Assina URLs do bucket do Google Cloud Storage (V4). É o modo padrão. */
@Service
@ConditionalOnProperty(name = "pethub.armazenamento.modo", havingValue = "gcs", matchIfMissing = true)
public class ArmazenamentoGcsService extends ArmazenamentoService {

    private volatile Storage storage;

    public ArmazenamentoGcsService(ArmazenamentoProperties propriedades) {
        super(propriedades);
    }

    @Override
    protected String urlDeEnvio(String caminho, String contentType) {
        BlobInfo blob = BlobInfo.newBuilder(BlobId.of(bucket(), caminho)).setContentType(contentType).build();
        return assinar(blob, HttpMethod.PUT, Storage.SignUrlOption.withContentType());
    }

    @Override
    protected String urlDeLeitura(String caminho) {
        return assinar(BlobInfo.newBuilder(BlobId.of(bucket(), caminho)).build(), HttpMethod.GET);
    }

    /** Criado só no primeiro uso: a aplicação sobe, e os testes rodam, sem credenciais do Google. */
    private Storage storage() {
        if (storage == null) {
            synchronized (this) {
                if (storage == null) {
                    storage = StorageOptions.newBuilder().setProjectId(propriedades.projeto()).build().getService();
                }
            }
        }
        return storage;
    }

    private String assinar(BlobInfo blob, HttpMethod metodo, Storage.SignUrlOption... extras) {
        try {
            var opcoes = new Storage.SignUrlOption[extras.length + 2];
            opcoes[0] = Storage.SignUrlOption.httpMethod(metodo);
            opcoes[1] = Storage.SignUrlOption.withV4Signature();
            System.arraycopy(extras, 0, opcoes, 2, extras.length);
            return storage().signUrl(blob, propriedades.validadeDaUrlEmMinutos(), TimeUnit.MINUTES, opcoes).toString();
        } catch (StorageException | IllegalStateException e) {
            throw new ArmazenamentoIndisponivelException(
                    "Não foi possível assinar a URL do arquivo. Confira as credenciais do Google Cloud.", e);
        }
    }

    private String bucket() {
        if (!propriedades.configurado()) {
            throw new ArmazenamentoIndisponivelException("Armazenamento não configurado: defina GCS_BUCKET.");
        }
        return propriedades.bucket();
    }
}
