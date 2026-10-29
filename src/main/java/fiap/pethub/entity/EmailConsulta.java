package fiap.pethub.entity;

import fiap.pethub.enums.StatusEmail;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Registro do e-mail de confirmação de uma consulta: um por consulta, com a marca de enviado ou falhou. */
@Entity
@Table(name = "TB_EMAIL_CONSULTA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailConsulta {

    private static final int TAMANHO_ERRO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_consulta_seq")
    @SequenceGenerator(name = "email_consulta_seq", sequenceName = "SQ_EMAIL_CONSULTA", allocationSize = 1)
    private Long id;

    @Column(name = "consulta_id", nullable = false, unique = true)
    private Long consultaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEmail status;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro", length = TAMANHO_ERRO)
    private String ultimoErro;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    private EmailConsulta(Long consultaId) {
        this.consultaId = consultaId;
        this.status = StatusEmail.FALHOU;
    }

    public static EmailConsulta para(Long consultaId) {
        return new EmailConsulta(consultaId);
    }

    public boolean foiEnviado() {
        return status == StatusEmail.ENVIADO;
    }

    public void registrarEnvio() {
        tentativas++;
        status = StatusEmail.ENVIADO;
        ultimoErro = null;
    }

    public void registrarFalha(String erro) {
        tentativas++;
        status = StatusEmail.FALHOU;
        ultimoErro = erro == null ? null : erro.substring(0, Math.min(erro.length(), TAMANHO_ERRO));
    }
}
