package fiap.pethub.exception;

/**
 * Uma dependência externa (IA, e-mail, serviço de responsáveis) não respondeu como esperado.
 *
 * A mensagem vai para o cliente, então nunca pode conter chave, token nem texto clínico.
 */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
