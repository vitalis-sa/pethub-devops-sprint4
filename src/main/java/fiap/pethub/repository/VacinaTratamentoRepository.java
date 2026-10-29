package fiap.pethub.repository;

import fiap.pethub.entity.VacinaTratamento;
import fiap.pethub.enums.TipoVacinaTratamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacinaTratamentoRepository extends JpaRepository<VacinaTratamento, Long> {
    Page<VacinaTratamento> findByPetId(Long petId, Pageable pageable);

    /** Usado na remocao em cascata de um pet. */
    void deleteByPetId(Long petId);

    /** Todos os registros dos pets de um responsavel. Base do filtro por posse. */
    Page<VacinaTratamento> findByPetResponsavelId(Long responsavelId, Pageable pageable);
    Page<VacinaTratamento> findByPetIdAndTipo(Long petId, TipoVacinaTratamento tipo, Pageable pageable);

    /** Registros de um tipo ligados a uma consulta (os medicamentos da receita, por exemplo). */
    List<VacinaTratamento> findAllByConsultaIdAndTipo(Long consultaId, TipoVacinaTratamento tipo);
}

