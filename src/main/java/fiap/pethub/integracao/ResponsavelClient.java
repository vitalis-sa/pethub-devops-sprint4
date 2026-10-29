package fiap.pethub.integracao;

import fiap.pethub.exception.ServicoIndisponivelException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Consulta o cadastro de responsáveis, que pertence ao serviço .NET.
 *
 * Cadastro inexistente (HTTP 404) vira {@link Optional#empty()}; qualquer outra falha vira
 * {@link ServicoIndisponivelException}, para o chamador não confundir "não existe" com "fora do ar".
 */
@Component
@RequiredArgsConstructor
public class ResponsavelClient {

    @Qualifier("responsaveisRestClient")
    private final RestClient restClient;
    private final JwtDeServico jwt;

    public Optional<ResponsavelResumo> buscarPorId(Long id) {
        return executar(restClient.get().uri("/api/responsavel/{id}", id));
    }

    public Optional<ResponsavelResumo> buscarPorCpf(String cpf) {
        return executar(restClient.get().uri("/api/responsavel/buscar?cpf={cpf}", cpf));
    }

    private Optional<ResponsavelResumo> executar(RestClient.RequestHeadersSpec<?> requisicao) {
        try {
            return Optional.ofNullable(requisicao
                    .header(ServicoProperties.CABECALHO, "Bearer " + jwt.emitirParaVitalis())
                    .retrieve()
                    .body(ResponsavelResumo.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServicoIndisponivelException("Não foi possível consultar o serviço de responsáveis.", e);
        }
    }
}
