package fiap.pethub.exception;

/**
 * A requisição está bem formada, mas o domínio não permite o que ela pede —
 * fechar duas vezes o mesmo prontuário, marcar duas consultas no mesmo horário.
 *
 * Diferente de {@link ResourceNotFoundException}, que fala de algo que não
 * existe: aqui tudo existe, e ainda assim a operação não pode acontecer.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
