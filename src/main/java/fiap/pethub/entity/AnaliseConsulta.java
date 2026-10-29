package fiap.pethub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Anamnese, receita e pedido de exames de uma consulta, já revisadas e aprovadas pelo veterinário.
 *
 * O rascunho da IA não é gravado: só o que o veterinário aceitou (ou corrigiu) entra
 * no prontuário, inclusive a sugestão de acompanhamento. A transcrição também
 * não é guardada, por ser dado sensível. Uma consulta tem no máximo uma análise.
 */
@Entity
@Table(name = "TB_ANALISE_CONSULTA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analise_consulta_seq")
    @SequenceGenerator(name = "analise_consulta_seq", sequenceName = "SQ_ANALISE_CONSULTA", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulta_id", nullable = false, unique = true)
    private Consulta consulta;

    @Lob
    @Column(name = "anamnese", nullable = false)
    private String anamnese;

    /** Medicamentos da receita, em JSON (lista; "[]" = sem receita). Ver {@code Prescricao}. */
    @Lob
    @Column(name = "receita", nullable = false)
    private String receita;

    /** Exames solicitados, em JSON (lista; vazio ou "[]" = sem pedido). */
    @Lob
    @Column(name = "exames")
    private String exames;

    /** Sugestão de acompanhamento da IA, vista e aceita pelo veterinário. Nunca é diagnóstico. */
    @Lob
    @Column(name = "sugestao")
    private String sugestao;

    @Column(name = "aprovada_em", nullable = false)
    private LocalDateTime aprovadaEm;
}
