package fiap.pethub.service;

import fiap.pethub.repository.AnaliseConsultaRepository;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.DiagnosticoRepository;
import fiap.pethub.repository.ExameRepository;
import fiap.pethub.repository.EmailConsultaRepository;
import fiap.pethub.repository.PedidoMedicoRepository;
import fiap.pethub.repository.VacinaTratamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apaga o que depende de um pet.
 *
 * Existe porque um pet é o centro de várias tabelas. Apagar só a linha do pet
 * esbarra nas chaves estrangeiras e o banco recusa — era esse o erro que as
 * telas de exclusão mostravam. Reunir a ordem correta num lugar só evita que
 * cada serviço tente adivinhá-la de novo.
 */
@Component
@RequiredArgsConstructor
public class RemocaoEmCascata {

    private final ConsultaRepository consultaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final ExameRepository exameRepository;
    private final PedidoMedicoRepository pedidoMedicoRepository;
    private final VacinaTratamentoRepository vacinaRepository;
    private final EmailConsultaRepository emailConsultaRepository;
    private final AnaliseConsultaRepository analiseConsultaRepository;

    /**
     * A consulta sai por último: diagnóstico, exame, pedido e vacina apontam
     * para ela, e o banco só libera a linha depois que ninguém mais a referencia.
     */
    @Transactional
    public void apagarHistoricoDoPet(Long petId) {
        diagnosticoRepository.deleteByPetId(petId);
        exameRepository.deleteByPetId(petId);
        pedidoMedicoRepository.deleteByPetId(petId);
        vacinaRepository.deleteByPetId(petId);
        emailConsultaRepository.deleteByPetId(petId);
        analiseConsultaRepository.deleteByConsultaPetId(petId);
        consultaRepository.deleteByPetId(petId);
    }
}
