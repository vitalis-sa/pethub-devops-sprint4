package fiap.pethub.repository;

import fiap.pethub.entity.Exame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExameRepository extends JpaRepository<Exame, Long> {
    Page<Exame> findByPetId(Long petId, Pageable pageable);

    /** Usado na remocao em cascata de um pet. */
    void deleteByPetId(Long petId);

    /** Todos os registros dos pets de um responsavel. Base do filtro por posse. */
    Page<Exame> findByPetResponsavelId(Long responsavelId, Pageable pageable);
    Page<Exame> findByConsultaId(Long consultaId, Pageable pageable);

    /** Todos os exames de uma consulta, sem paginação (usado ao refletir o pedido de exames no prontuário). */
    List<Exame> findAllByConsultaId(Long consultaId);
}

