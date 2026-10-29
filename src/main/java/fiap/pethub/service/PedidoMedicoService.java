package fiap.pethub.service;

import fiap.pethub.dto.request.PedidoMedicoRequest;
import fiap.pethub.dto.response.DeleteResponse;
import fiap.pethub.dto.response.PedidoMedicoResponse;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.PedidoMedico;
import fiap.pethub.entity.Pet;
import fiap.pethub.enums.StatusPedidoMedico;
import fiap.pethub.enums.TipoLembrete;
import fiap.pethub.enums.TipoPedidoMedico;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.messaging.LembreteSolicitado;
import fiap.pethub.mapper.PedidoMedicoMapper;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.PedidoMedicoRepository;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.security.EscopoDoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PedidoMedicoService {

    private final PedidoMedicoRepository repository;
    private final ConsultaRepository consultaRepository;
    private final PetRepository petRepository;
    private final PedidoMedicoMapper mapper;
    private final EscopoDoUsuario escopo;
    private final ApplicationEventPublisher eventos;

    @Cacheable(value = "pedidos", key = "'lista-' + #petId + '-' + #status + '-' + #tipo + '-' + #pageable + '-' + @escopoDoUsuario.chaveDeCache()")
    public Page<PedidoMedicoResponse> findAll(Long petId, StatusPedidoMedico status, TipoPedidoMedico tipo, Pageable pageable) {
        if (petId != null) {
            exigirPosseDoPet(petId);
        }
        return Stream.<Map.Entry<Boolean, Supplier<Page<PedidoMedico>>>>of(
                Map.entry(petId != null && status != null, () -> repository.findByPetIdAndStatus(petId, status, pageable)),
                Map.entry(petId != null && tipo != null,   () -> repository.findByPetIdAndTipo(petId, tipo, pageable)),
                Map.entry(petId != null,                   () -> repository.findByPetId(petId, pageable))
        )
                .filter(Map.Entry::getKey)
                .findFirst()
                .map(Map.Entry::getValue)
                .map(Supplier::get)
                .orElseGet(listarNoEscopo(pageable))
                .map(mapper::toResponse);
    }

    @Cacheable(value = "pedidos", key = "#id + '-' + @escopoDoUsuario.chaveDeCache()")
    public PedidoMedicoResponse findById(Long id) {
        PedidoMedico entidade = findEntityById(id);
        escopo.exigirPosse(entidade.getPet().getResponsavelId());
        return mapper.toResponse(entidade);
    }

    @Transactional
    @CacheEvict(value = "pedidos", allEntries = true)
    public PedidoMedicoResponse create(PedidoMedicoRequest request) {
        PedidoMedico entity = buildPedidoEntity(request);
        PedidoMedico saved = repository.save(entity);
        notificarResponsavel(saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "pedidos", allEntries = true)
    public PedidoMedicoResponse update(Long id, PedidoMedicoRequest request) {
        PedidoMedico entity = findEntityById(id);
        escopo.exigirPosse(entity.getPet().getResponsavelId());
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    @CacheEvict(value = "pedidos", allEntries = true)
    public DeleteResponse delete(Long id) {
        PedidoMedico entidade = findEntityById(id);
        escopo.exigirPosse(entidade.getPet().getResponsavelId());
        repository.delete(entidade);
        return DeleteResponse.of("Pedido médico", id);
    }

    private PedidoMedico buildPedidoEntity(PedidoMedicoRequest request) {
        PedidoMedico entity = mapper.toEntity(request);
        entity.setConsulta(findConsulta(request.getConsultaId()));
        entity.setPet(findPet(request.getPetId()));
        entity.setStatus(entity.getStatus() != null ? entity.getStatus() : StatusPedidoMedico.PENDENTE);
        return entity;
    }

    /** Sem data limite não há o que lembrar: o lembrete precisa de uma data. */
    private void notificarResponsavel(PedidoMedico saved) {
        if (saved.getDataLimite() == null) {
            return;
        }
        Pet pet = saved.getPet();
        TipoLembrete tipo = saved.getTipo() == TipoPedidoMedico.EXAME ? TipoLembrete.EXAME : TipoLembrete.MEDICAMENTO;
        String instrucoes = saved.getInstrucoes() == null ? "" : " — " + saved.getInstrucoes();
        eventos.publishEvent(new LembreteSolicitado(
                pet.getResponsavelId(), pet.getId(), tipo, saved.getDataLimite(),
                saved.getDescricao() + instrucoes, saved.getId(), "PedidoMedico"));
    }

    private Consulta findConsulta(Long consultaId) {
        return consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com id: " + consultaId));
    }

    private Pet findPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado com id: " + petId));
    }

    /** Lista respeitando o escopo: veterinario ve tudo, responsavel so os pets dele. */
    private Supplier<Page<PedidoMedico>> listarNoEscopo(Pageable pageable) {
        return () -> escopo.ehVeterinario()
                ? repository.findAll(pageable)
                : repository.findByPetResponsavelId(escopo.idDoResponsavel(), pageable);
    }

    /** Filtrar por um pet alheio nao pode revelar sequer que ele existe. */
    private void exigirPosseDoPet(Long petId) {
        escopo.exigirPosse(findPet(petId).getResponsavelId());
    }

    private PedidoMedico findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido médico não encontrado com id: " + id));
    }
}
