package fiap.pethub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "TB_PET")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pet_seq")
    @SequenceGenerator(name = "pet_seq", sequenceName = "SQ_PET", allocationSize = 1)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "especie", nullable = false)
    private String especie;

    @Column(name = "raca")
    private String raca;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "peso")
    private Double peso;

    @Column(name = "genero")
    private String genero;

    /** Id do responsável no serviço .NET. Sem chave estrangeira: cada serviço é dono do seu banco. */
    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    /** Cópia do nome no momento do cadastro, para listar pets sem consultar o .NET a cada linha. */
    @Column(name = "nome_responsavel", nullable = false, length = 150)
    private String nomeResponsavel;

    /** Caminho da foto no bucket (fotos/{petId}/...). Só o caminho: o arquivo fica no Cloud Storage. */
    @Column(name = "foto_path")
    private String fotoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_responsavel_id")
    private Veterinario veterinarioResponsavel;

    /** A idade nunca é gravada: sai da data de nascimento, então não fica desatualizada. */
    public Integer idadeEmAnos() {
        return dataNascimento == null ? null : Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
