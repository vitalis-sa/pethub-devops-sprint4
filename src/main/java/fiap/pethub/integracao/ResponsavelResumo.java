package fiap.pethub.integracao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** O pouco que o Java precisa saber de um responsável; o resto fica no .NET. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponsavelResumo(Long id, String nome, String email, String cpf) {
}
