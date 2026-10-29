package fiap.pethub.repository;

import fiap.pethub.entity.Consulta;
import fiap.pethub.enums.StatusConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    /** Traz pet, veterinário e unidade de uma vez: o e-mail é montado fora de qualquer sessão do Hibernate. */
    @EntityGraph(attributePaths = {"pet", "veterinario", "unidade"})
    Optional<Consulta> findComDetalhesById(Long id);

    Page<Consulta> findByPetId(Long petId, Pageable pageable);

    /** Usado na remocao em cascata de um pet. */
    void deleteByPetId(Long petId);

    /** Todos os registros dos pets de um responsavel. Base do filtro por posse. */
    Page<Consulta> findByPetResponsavelId(Long responsavelId, Pageable pageable);
    Page<Consulta> findByVeterinarioId(Long veterinarioId, Pageable pageable);
    Page<Consulta> findByStatus(StatusConsulta status, Pageable pageable);
    Page<Consulta> findByPetIdAndStatus(Long petId, StatusConsulta status, Pageable pageable);

    /**
     * Ha outra consulta ocupando este veterinario neste horario? Consulta
     * cancelada nao ocupa agenda, por isso fica de fora.
     */
    boolean existsByVeterinarioIdAndDataHoraAndStatusNot(Long veterinarioId,
                                                         LocalDateTime dataHora,
                                                         StatusConsulta status);
}

