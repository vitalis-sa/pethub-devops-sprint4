package fiap.pethub.service;

import fiap.pethub.dto.request.ConsultaRequest;
import fiap.pethub.dto.response.ConsultaResponse;
import fiap.pethub.dto.response.DeleteResponse;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.email.ConsultaAgendada;
import fiap.pethub.enums.TipoLembrete;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.messaging.LembreteSolicitado;
import fiap.pethub.mapper.ConsultaMapper;
import fiap.pethub.repository.AnaliseConsultaRepository;
import fiap.pethub.repository.EmailConsultaRepository;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.repository.PetRepository;
import fiap.pethub.security.EscopoDoUsuario;
import fiap.pethub.repository.UnidadeVeterinarioRepository;
import fiap.pethub.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    /** Como a data aparece no texto do lembrete: 31/10/2026 às 16:45. */
    private static final DateTimeFormatter DATA_HORA_DO_AVISO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ConsultaRepository repository;
    private final PetRepository petRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final UnidadeVeterinarioRepository unidadeRepository;
    private final ConsultaMapper mapper;
    private final EscopoDoUsuario escopo;
    private final ApplicationEventPublisher eventos;
    private final AnaliseConsultaRepository analiseConsultaRepository;
    private final EmailConsultaRepository emailConsultaRepository;

    @Cacheable(value = "consultas", key = "'lista-' + #petId + '-' + #veterinarioId + '-' + #status + '-' + #pageable + '-' + @escopoDoUsuario.chaveDeCache()")
    public Page<ConsultaResponse> findAll(Long petId, Long veterinarioId, StatusConsulta status, Pageable pageable) {
        if (petId != null) {
            exigirPosseDoPet(petId);
        }
        return Stream.<Map.Entry<Boolean, Supplier<Page<Consulta>>>>of(
                Map.entry(petId != null && status != null, () -> repository.findByPetIdAndStatus(petId, status, pageable)),
                Map.entry(petId != null,                   () -> repository.findByPetId(petId, pageable)),
                Map.entry(veterinarioId != null,           () -> repository.findByVeterinarioId(veterinarioId, pageable)),
                Map.entry(status != null,                  () -> repository.findByStatus(status, pageable))
        )
                .filter(Map.Entry::getKey)
                .findFirst()
                .map(Map.Entry::getValue)
                .map(Supplier::get)
                .orElseGet(listarNoEscopo(pageable))
                .map(mapper::toResponse);
    }

    @Cacheable(value = "consultas", key = "#id + '-' + @escopoDoUsuario.chaveDeCache()")
    public ConsultaResponse findById(Long id) {
        Consulta entidade = findEntityById(id);
        escopo.exigirPosse(entidade.getPet().getResponsavelId());
        return mapper.toResponse(entidade);
    }

    @Transactional
    @CacheEvict(value = "consultas", allEntries = true)
    public ConsultaResponse create(ConsultaRequest request) {
        Pet pet = findPet(request.getPetId());
        escopo.exigirPosse(pet.getResponsavelId());
        StatusConsulta status = statusPermitido(request);
        exigirAgendaLivre(request);
        exigirHorarioFuturoParaAgendamento(request, status);

        Consulta salva = repository.save(montarConsulta(request, pet, status));

        if (status == StatusConsulta.AGENDADA) {
            avisarResponsavel(salva);
        }
        return mapper.toResponse(salva);
    }

    /**
     * Só o veterinário lança consulta já realizada ou cancelada. O responsável
     * apenas agenda, seja qual for o status que mandar.
     */
    private StatusConsulta statusPermitido(ConsultaRequest request) {
        return escopo.ehVeterinario() ? request.getStatus() : StatusConsulta.AGENDADA;
    }

    /**
     * Um veterinario nao atende dois pets ao mesmo tempo. Sem esta checagem a
     * agenda aceita sobreposicao e o conflito so aparece no dia da consulta.
     */
    private void exigirAgendaLivre(ConsultaRequest request) {
        boolean ocupado = repository.existsByVeterinarioIdAndDataHoraAndStatusNot(
                request.getVeterinarioId(), request.getDataHora(), StatusConsulta.CANCELADA);
        if (ocupado) {
            throw new RegraDeNegocioException(
                    "Este veterinário já tem uma consulta marcada neste horário.");
        }
    }

    /**
     * Marcar para tras nao faz sentido; registrar o que ja aconteceu, faz. Por
     * isso a regra vale so para o que esta sendo agendado, e nao para uma
     * consulta lancada como realizada.
     */
    private void exigirHorarioFuturoParaAgendamento(ConsultaRequest request, StatusConsulta status) {
        boolean agendamento = status == StatusConsulta.AGENDADA;
        if (agendamento && request.getDataHora().isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException(
                    "Não é possível agendar uma consulta para uma data que já passou.");
        }
    }

    @Transactional
    @CacheEvict(value = "consultas", allEntries = true)
    public ConsultaResponse update(Long id, ConsultaRequest request) {
        Consulta entity = findEntityById(id);
        escopo.exigirPosse(entity.getPet().getResponsavelId());
        mapper.updateEntity(request, entity);
        applyUnidade(request.getUnidadeId(), entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Muda o horário de uma consulta ainda agendada. Vale as mesmas regras do agendamento (futuro, agenda
     * livre) e avisa o .NET de novo: o lembrete da consulta é atualizado, não duplicado.
     */
    @Transactional
    @CacheEvict(value = "consultas", allEntries = true)
    public ConsultaResponse remarcar(Long id, LocalDateTime novaDataHora) {
        Consulta consulta = consultaAgendadaDoUsuario(id);
        if (novaDataHora.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível remarcar para uma data que já passou.");
        }
        boolean mudou = !novaDataHora.equals(consulta.getDataHora());
        if (mudou && repository.existsByVeterinarioIdAndDataHoraAndStatusNot(
                consulta.getVeterinario().getId(), novaDataHora, StatusConsulta.CANCELADA)) {
            throw new RegraDeNegocioException("Este veterinário já tem uma consulta marcada neste horário.");
        }
        consulta.setDataHora(novaDataHora);
        Consulta salva = repository.save(consulta);
        solicitarLembrete(salva);
        return mapper.toResponse(salva);
    }

    /** O responsável conta ao veterinário o que observou, antes do atendimento. */
    @Transactional
    @CacheEvict(value = "consultas", allEntries = true)
    public ConsultaResponse registrarQueixas(Long id, String queixas) {
        Consulta consulta = consultaAgendadaDoUsuario(id);
        consulta.setQueixas(queixas.strip());
        return mapper.toResponse(repository.save(consulta));
    }

    private Consulta consultaAgendadaDoUsuario(Long id) {
        Consulta consulta = findEntityById(id);
        escopo.exigirPosse(consulta.getPet().getResponsavelId());
        if (consulta.getStatus() != StatusConsulta.AGENDADA) {
            throw new RegraDeNegocioException("Só é possível alterar uma consulta que ainda está agendada.");
        }
        return consulta;
    }

    /**
     * Marca a consulta como realizada. Chamado ao fim do prontuario: sem isto a
     * consulta seguiria aparecendo como agendada na agenda do veterinario mesmo
     * depois de atendida.
     */
    @Transactional
    @CacheEvict(value = "consultas", allEntries = true)
    public ConsultaResponse concluir(Long id) {
        Consulta entidade = findEntityById(id);
        escopo.exigirPosse(entidade.getPet().getResponsavelId());
        entidade.setStatus(StatusConsulta.REALIZADA);
        return mapper.toResponse(repository.save(entidade));
    }

    @Transactional
    @CacheEvict(value = "consultas", allEntries = true)
    public DeleteResponse delete(Long id) {
        Consulta entidade = findEntityById(id);
        escopo.exigirPosse(entidade.getPet().getResponsavelId());
        analiseConsultaRepository.deleteByConsultaId(id);
        emailConsultaRepository.deleteByConsultaId(id);
        repository.delete(entidade);
        return DeleteResponse.of("Consulta", id);
    }

    private Consulta montarConsulta(ConsultaRequest request, Pet pet, StatusConsulta status) {
        Consulta entity = mapper.toEntity(request);
        entity.setStatus(status);
        entity.setPet(pet);
        entity.setVeterinario(findVeterinario(request.getVeterinarioId()));
        applyUnidade(request.getUnidadeId(), entity);
        return entity;
    }

    private void applyUnidade(Long unidadeId, Consulta entity) {
        if (unidadeId == null) {
            entity.setUnidade(null);
            return;
        }
        unidadeRepository.findById(unidadeId)
                .ifPresentOrElse(
                        entity::setUnidade,
                        () -> { throw new ResourceNotFoundException("Unidade veterinária não encontrada com id: " + unidadeId);
                        });
    }

    /**
     * Os dois avisos só acontecem depois que a consulta foi confirmada no banco: o lembrete segue
     * para o .NET pela fila, e o e-mail de confirmação sai direto.
     */
    private void avisarResponsavel(Consulta consulta) {
        solicitarLembrete(consulta);
        eventos.publishEvent(new ConsultaAgendada(consulta.getId()));
    }

    private void solicitarLembrete(Consulta consulta) {
        Pet pet = consulta.getPet();
        eventos.publishEvent(new LembreteSolicitado(
                pet.getResponsavelId(), pet.getId(), TipoLembrete.CONSULTA, consulta.getDataHora().toLocalDate(),
                "Consulta agendada para " + pet.getNome() + " em " + consulta.getDataHora().format(DATA_HORA_DO_AVISO),
                consulta.getId(), "Consulta"));
    }

    private Pet findPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado com id: " + petId));
    }

    private Veterinario findVeterinario(Long veterinarioId) {
        return veterinarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado com id: " + veterinarioId));
    }

    /** Lista respeitando o escopo: veterinario ve tudo, responsavel so os pets dele. */
    private Supplier<Page<Consulta>> listarNoEscopo(Pageable pageable) {
        return () -> escopo.ehVeterinario()
                ? repository.findAll(pageable)
                : repository.findByPetResponsavelId(escopo.idDoResponsavel(), pageable);
    }

    /** Filtrar por um pet alheio nao pode revelar sequer que ele existe. */
    private void exigirPosseDoPet(Long petId) {
        escopo.exigirPosse(findPet(petId).getResponsavelId());
    }

    private Consulta findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com id: " + id));
    }
}

