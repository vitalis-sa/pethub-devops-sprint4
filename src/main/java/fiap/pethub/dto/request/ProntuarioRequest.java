package fiap.pethub.dto.request;

import fiap.pethub.enums.TipoPedidoMedico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * O que o veterinário preenche ao fechar uma consulta.
 *
 * Junta diagnóstico e pedido médico porque, na mesa do consultório, são um
 * gesto só. O pedido é opcional: nem toda consulta termina pedindo exame.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProntuarioRequest {

    @NotBlank(message = "Informe ao menos o sintoma principal")
    @Size(max = 255, message = "Sintoma deve ter no máximo 255 caracteres")
    private String sintoma1;

    @Size(max = 255, message = "Doença deve ter no máximo 255 caracteres")
    private String doencaPredita;

    private TipoPedidoMedico tipo;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    @Size(max = 255, message = "Instruções devem ter no máximo 255 caracteres")
    private String instrucoes;

    /**
     * O pedido médico só existe quando o veterinário escolheu um tipo e disse
     * o que está pedindo. Ter só um dos dois é formulário pela metade, e quem
     * trata isso é a validação em {@link #pedidoIncompleto()}.
     */
    public boolean temPedidoMedico() {
        return tipo != null && descricao != null && !descricao.isBlank();
    }

    public boolean pedidoIncompleto() {
        boolean temTipo = tipo != null;
        boolean temDescricao = descricao != null && !descricao.isBlank();
        return temTipo != temDescricao;
    }
}
