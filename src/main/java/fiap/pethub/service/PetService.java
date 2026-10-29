package fiap.pethub.service;

import fiap.pethub.armazenamento.ArmazenamentoService;
import fiap.pethub.armazenamento.PastaDoBucket;
import fiap.pethub.dto.request.PetRequest;
import fiap.pethub.dto.response.DeleteResponse;
import fiap.pethub.dto.response.PetResponse;
import fiap.pethub.dto.response.UrlDeDownloadResponse;
import fiap.pethub.dto.response.UrlDeEnvioResponse;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.integracao.ResponsavelClient;
import fiap.pethub.integracao.ResponsavelResumo;
import fiap.pethub.mapper.PetMapper;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.repository.VeterinarioRepository;
import fiap.pethub.security.EscopoDoUsuario;
import lombok.RequiredArgsConstructor;
import fiap.pethub.config.LimpaCachesDoPet;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository repository;
    private final VeterinarioRepository veterinarioRepository;
    private final ResponsavelClient responsavelClient;
    private final PetMapper mapper;
    private final EscopoDoUsuario escopo;
    private final RemocaoEmCascata remocao;
    private final ArmazenamentoService armazenamento;

    @Cacheable(value = "pets", key = "'lista-' + #nome + '-' + #veterinarioId + '-' + #pageable + '-' + @escopoDoUsuario.chaveDeCache()")
    public Page<PetResponse> findAll(String nome, Long veterinarioId, Pageable pageable) {
        return (escopo.ehVeterinario()
                    ? listarComoVeterinario(nome, veterinarioId, pageable)
                    : listarComoResponsavel(nome, veterinarioId, pageable, escopo.idDoResponsavel()))
                .map(mapper::toResponse);
    }

    private Page<Pet> listarComoVeterinario(String nome, Long veterinarioId, Pageable pageable) {
        return Stream.<Map.Entry<Boolean, Supplier<Page<Pet>>>>of(
                        Map.entry(nome != null,          () -> repository.findByNomeContainingIgnoreCase(nome, pageable)),
                        Map.entry(veterinarioId != null, () -> repository.findByVeterinarioResponsavelId(veterinarioId, pageable)))
                .filter(Map.Entry::getKey)
                .findFirst()
                .map(Map.Entry::getValue)
                .map(Supplier::get)
                .orElseGet(() -> repository.findAll(pageable));
    }

    /** Os mesmos filtros, sempre restritos aos pets do próprio responsável. */
    private Page<Pet> listarComoResponsavel(String nome, Long veterinarioId, Pageable pageable, Long dono) {
        return Stream.<Map.Entry<Boolean, Supplier<Page<Pet>>>>of(
                        Map.entry(nome != null,          () -> repository.findByNomeContainingIgnoreCaseAndResponsavelId(nome, dono, pageable)),
                        Map.entry(veterinarioId != null, () -> repository.findByVeterinarioResponsavelIdAndResponsavelId(veterinarioId, dono, pageable)))
                .filter(Map.Entry::getKey)
                .findFirst()
                .map(Map.Entry::getValue)
                .map(Supplier::get)
                .orElseGet(() -> repository.findByResponsavelId(dono, pageable));
    }

    public Page<PetResponse> findByResponsavelCpf(String cpf, Pageable pageable) {
        Long responsavelId = buscarResponsavelPorCpf(cpf).id();
        // Sem esta guarda, um responsável consultaria os pets de outro passando o CPF dele.
        escopo.exigirPosse(responsavelId);
        return repository.findByResponsavelId(responsavelId, pageable).map(mapper::toResponse);
    }

    @Cacheable(value = "pets", key = "#id + '-' + @escopoDoUsuario.chaveDeCache()")
    public PetResponse findById(Long id) {
        Pet pet = findEntityById(id);
        escopo.exigirPosse(pet.getResponsavelId());
        return mapper.toResponse(pet);
    }

    /**
     * Veterinário cadastra o pet para o responsável do CPF informado; o responsável cadastra sempre
     * para si mesmo, seja qual for o CPF que mandar.
     */
    @Transactional
    @CacheEvict(value = "pets", allEntries = true)
    public PetResponse create(PetRequest request) {
        Pet pet = mapper.toEntity(request);
        definirResponsavel(pet, donoDoNovoPet(request));
        pet.setVeterinarioResponsavel(buscarVeterinario(request.getVeterinarioResponsavelId()).orElse(null));
        return mapper.toResponse(repository.save(pet));
    }

    @Transactional
    @LimpaCachesDoPet
    public PetResponse update(Long id, PetRequest request) {
        Pet pet = findEntityById(id);
        escopo.exigirPosse(pet.getResponsavelId());
        mapper.updateEntity(request, pet);
        if (escopo.ehVeterinario() && temTexto(request.getResponsavelCpf())) {
            definirResponsavel(pet, buscarResponsavelPorCpf(request.getResponsavelCpf()));
        }
        buscarVeterinario(request.getVeterinarioResponsavelId()).ifPresent(pet::setVeterinarioResponsavel);
        return mapper.toResponse(repository.save(pet));
    }

    @Transactional
    @LimpaCachesDoPet
    public DeleteResponse delete(Long id) {
        Pet pet = findEntityById(id);
        escopo.exigirPosse(pet.getResponsavelId());
        remocao.apagarHistoricoDoPet(id);
        repository.delete(pet);
        return DeleteResponse.of("Pet", id);
    }

    /**
     * Chamado pelo .NET antes de excluir um responsável: apaga todos os pets dele com o histórico.
     * Idempotente: sem pets, não faz nada e responde sucesso.
     */
    @Transactional
    @LimpaCachesDoPet
    public DeleteResponse deleteByResponsavel(Long responsavelId) {
        escopo.exigirPosse(responsavelId);
        repository.findByResponsavelId(responsavelId).forEach(pet -> {
            remocao.apagarHistoricoDoPet(pet.getId());
            repository.delete(pet);
        });
        return DeleteResponse.of("Pets do responsável", responsavelId);
    }

    /** Passo 1 da foto: devolve a URL assinada para o app enviar a imagem direto ao bucket. */
    public UrlDeEnvioResponse prepararEnvioDaFoto(Long id, String contentType) {
        Pet pet = petDoUsuario(id);
        return armazenamento.prepararEnvio(PastaDoBucket.FOTOS, String.valueOf(pet.getId()), contentType);
    }

    /** Passo 2 da foto: depois do envio, o app confirma e o pet passa a apontar para o arquivo. */
    @Transactional
    @CacheEvict(value = "pets", allEntries = true)
    public PetResponse confirmarFoto(Long id, String caminho) {
        Pet pet = petDoUsuario(id);
        armazenamento.exigirCaminhoDe(PastaDoBucket.FOTOS, String.valueOf(pet.getId()), caminho);
        pet.setFotoPath(caminho);
        return mapper.toResponse(repository.save(pet));
    }

    public UrlDeDownloadResponse urlDaFoto(Long id) {
        Pet pet = petDoUsuario(id);
        if (pet.getFotoPath() == null) {
            throw new ResourceNotFoundException("O pet ainda não tem foto.");
        }
        return armazenamento.urlDeDownload(pet.getFotoPath());
    }

    private Pet petDoUsuario(Long id) {
        Pet pet = findEntityById(id);
        escopo.exigirPosse(pet.getResponsavelId());
        return pet;
    }

    private ResponsavelResumo donoDoNovoPet(PetRequest request) {
        if (!escopo.ehVeterinario()) {
            return buscarResponsavelPorId(escopo.idDoResponsavel());
        }
        if (!temTexto(request.getResponsavelCpf())) {
            throw new RegraDeNegocioException("Informe o CPF do responsável pelo pet.");
        }
        return buscarResponsavelPorCpf(request.getResponsavelCpf());
    }

    private void definirResponsavel(Pet pet, ResponsavelResumo responsavel) {
        pet.setResponsavelId(responsavel.id());
        pet.setNomeResponsavel(responsavel.nome());
    }

    private ResponsavelResumo buscarResponsavelPorCpf(String cpf) {
        return responsavelClient.buscarPorCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com CPF: " + cpf));
    }

    private ResponsavelResumo buscarResponsavelPorId(Long id) {
        return responsavelClient.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com id: " + id));
    }

    private Optional<Veterinario> buscarVeterinario(Long veterinarioId) {
        return Optional.ofNullable(veterinarioId)
                .map(id -> veterinarioRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado com id: " + id)));
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private Pet findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado com id: " + id));
    }
}
