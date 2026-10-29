package fiap.pethub.repository;

import fiap.pethub.entity.AnaliseConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnaliseConsultaRepository extends JpaRepository<AnaliseConsulta, Long> {

    Optional<AnaliseConsulta> findByConsultaId(Long consultaId);

    /** Usado ao apagar uma consulta: a análise não existe sem ela. */
    void deleteByConsultaId(Long consultaId);

    /** Usado na remoção em cascata de um pet. */
    void deleteByConsultaPetId(Long petId);
}
