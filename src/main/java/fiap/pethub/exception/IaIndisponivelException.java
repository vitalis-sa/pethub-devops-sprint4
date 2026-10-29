package fiap.pethub.exception;

/** O serviço de IA falhou, não está configurado ou devolveu algo inutilizável. */
public class IaIndisponivelException extends ServicoIndisponivelException {

    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
