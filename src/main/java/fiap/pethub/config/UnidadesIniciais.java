package fiap.pethub.config;

import fiap.pethub.entity.UnidadeVeterinario;
import fiap.pethub.repository.UnidadeVeterinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Garante as duas unidades da clínica na subida da aplicação. É idempotente: a unidade é
 * identificada pelo CEP, então reiniciar não duplica nada e uma unidade já existente não é alterada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnidadesIniciais implements ApplicationRunner {

    private static final List<UnidadeVeterinario> UNIDADES = List.of(
            unidade("Vitalis Paulista", "Av. Paulista", "1106", "Bela Vista / Cerqueira César", "01311000"),
            unidade("Vitalis Aclimação", "Av. Lins de Vasconcelos", "1222", "Aclimação", "01538001"));

    private final UnidadeVeterinarioRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UNIDADES.stream()
                .filter(unidade -> !repository.existsByCep(unidade.getCep()))
                .forEach(unidade -> {
                    repository.save(unidade);
                    log.info("Unidade veterinária cadastrada: {}", unidade.getNome());
                });
    }

    private static UnidadeVeterinario unidade(String nome, String logradouro, String numero, String bairro, String cep) {
        return UnidadeVeterinario.builder()
                .nome(nome).logradouro(logradouro).numero(numero).bairro(bairro)
                .cidade("São Paulo").estado("SP").cep(cep)
                .build();
    }
}
