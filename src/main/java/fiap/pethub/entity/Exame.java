package fiap.pethub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_EXAME")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exame {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exame_seq")
    @SequenceGenerator(name = "exame_seq", sequenceName = "SQ_EXAME", allocationSize = 1)
    private Long id;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "resultado")
    private String resultado;

    @Column(name = "arquivo_resultado")
    private String arquivoResultado;

    /** Resumo do resultado gerado por IA: evita abrir o PDF a cada consulta. */
    @Column(name = "resumo_ia", length = 2000)
    private String resumoIa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;
}

