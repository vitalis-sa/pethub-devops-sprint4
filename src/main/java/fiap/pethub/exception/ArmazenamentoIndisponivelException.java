package fiap.pethub.exception;

/** O bucket não está configurado ou o Google Cloud Storage falhou. */
public class ArmazenamentoIndisponivelException extends ServicoIndisponivelException {

    public ArmazenamentoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ArmazenamentoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
