package fiap.pethub.repository;

import fiap.pethub.entity.EmailConsulta;
import fiap.pethub.enums.StatusEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailConsultaRepository extends JpaRepository<EmailConsulta, Long> {

    Optional<EmailConsulta> findByConsultaId(Long consultaId);

    @Query("select e from EmailConsulta e where e.status = :status and e.criadoEm >= :inicio and e.criadoEm < :fim")
    List<EmailConsulta> findByStatusCriadosEntre(@Param("status") StatusEmail status,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fim") LocalDateTime fim);

    void deleteByConsultaId(Long consultaId);

    /** Usado na remoção em cascata de um pet. */
    @Modifying
    @Query("delete from EmailConsulta e where e.consultaId in (select c.id from Consulta c where c.pet.id = :petId)")
    void deleteByPetId(@Param("petId") Long petId);

    /**
     * Grava em transação própria. O envio acontece logo depois do commit da consulta, quando a
     * transação original já terminou mas ainda está associada à thread; uma gravação comum
     * entraria nela e seria descartada.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    <S extends EmailConsulta> S save(S registro);
}
