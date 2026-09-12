package fiap.pethub.service;

import fiap.pethub.entity.Pet;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.DiagnosticoRepository;
import fiap.pethub.repository.ExameRepository;
import fiap.pethub.repository.LeituraWearableRepository;
import fiap.pethub.repository.LembreteRepository;
import fiap.pethub.repository.PedidoMedicoRepository;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.repository.VacinaTratamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Apaga o que depende de um pet, ou de um tutor inteiro.
 *
 * Existe porque um pet é o centro de sete tabelas. Apagar só a linha do pet
 * esbarra nas chaves estrangeiras e o banco recusa — era esse o erro que as
 * telas de exclusão mostravam. Reunir a ordem correta num lugar só evita que
 * cada serviço tente adivinhá-la de novo.
 */
@Component
@RequiredArgsConstructor
public class RemocaoEmCascata {

    private final PetRepository petRepository;
    private final ConsultaRepository consultaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final ExameRepository exameRepository;
    private final PedidoMedicoRepository pedidoMedicoRepository;
    private final VacinaTratamentoRepository vacinaRepository;
    private final LembreteRepository lembreteRepository;
    private final LeituraWearableRepository leituraRepository;

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
        lembreteRepository.deleteByPetId(petId);
        leituraRepository.deleteByPetId(petId);
        consultaRepository.deleteByPetId(petId);
    }

    /**
     * Encerrar a conta do tutor leva junto os pets dele e o histórico de cada um.
     * Os lembretes ficam para o fim porque alguns pertencem ao tutor sem estarem
     * presos a nenhum pet.
     */
    @Transactional
    public void apagarDadosDoTutor(Long responsavelId) {
        List<Pet> pets = petRepository.findByResponsavelId(responsavelId);
        for (Pet pet : pets) {
            apagarHistoricoDoPet(pet.getId());
        }
        petRepository.deleteAll(pets);
        lembreteRepository.deleteByResponsavelId(responsavelId);
    }
}
