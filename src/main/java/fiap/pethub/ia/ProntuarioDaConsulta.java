package fiap.pethub.ia;

import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Exame;
import fiap.pethub.entity.VacinaTratamento;
import fiap.pethub.enums.TipoLembrete;
import fiap.pethub.enums.TipoVacinaTratamento;
import fiap.pethub.messaging.LembreteSolicitado;
import fiap.pethub.repository.ExameRepository;
import fiap.pethub.repository.VacinaTratamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Leva o que o veterinário aprovou para o prontuário do pet, para aparecer nas abas dele:
 * cada medicamento da receita vira um registro em Tratamentos e cada exame pedido vira um exame (sem
 * resultado ainda), ao qual o PDF do resultado é anexado depois.
 *
 * Salvar de novo não duplica: itens com o mesmo nome são atualizados. Medicamentos tirados da receita saem
 * dos Tratamentos; exames nunca são apagados aqui, porque podem já ter resultado anexado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProntuarioDaConsulta {

    private static final int LIMITE_DA_COLUNA = 255;
    private static final ZoneId FUSO_DA_CLINICA = ZoneId.of("America/Sao_Paulo");

    private final VacinaTratamentoRepository tratamentos;
    private final ExameRepository exames;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public void refletir(Consulta consulta, List<MedicamentoPrescrito> medicamentos, List<ExameSolicitado> pedidos) {
        refletirMedicamentos(consulta, medicamentos);
        refletirExames(consulta, pedidos);
        log.info("Prontuário da consulta {} atualizado: {} medicamento(s) em Tratamentos, {} exame(s) pedido(s)",
                consulta.getId(), medicamentos.size(), pedidos.size());
    }

    private void refletirMedicamentos(Consulta consulta, List<MedicamentoPrescrito> medicamentos) {
        List<VacinaTratamento> existentes = tratamentos.findAllByConsultaIdAndTipo(consulta.getId(), TipoVacinaTratamento.MEDICAMENTO);

        Set<String> receitados = medicamentos.stream().map(m -> chave(m.nome())).collect(Collectors.toSet());
        List<VacinaTratamento> removidos = existentes.stream().filter(t -> !receitados.contains(chave(t.getNome()))).toList();
        tratamentos.deleteAll(removidos);
        removidos.forEach(t -> eventos.publishEvent(
                LembreteSolicitado.cancelamento(consulta.getPet().getResponsavelId(), consulta.getPet().getId(), t.getId())));

        medicamentos.forEach(m -> {
            VacinaTratamento registro = existentes.stream()
                    .filter(t -> chave(t.getNome()).equals(chave(m.nome())))
                    .findFirst()
                    .orElseGet(() -> VacinaTratamento.builder()
                            .tipo(TipoVacinaTratamento.MEDICAMENTO)
                            .nome(limitar(m.nome()))
                            .dataAplicacao(LocalDate.now())
                            .pet(consulta.getPet())
                            .veterinario(consulta.getVeterinario())
                            .consulta(consulta)
                            .build());
            registro.setDose(limitar(m.posologia()));
            registro.setObservacoes(limitar(observacoesDe(m)));
            tratamentos.save(registro);
            solicitarLembretes(consulta, registro, m);
        });
    }

    /** Frequência e duração viram a mensagem para o .NET, que cria um lembrete por dose. */
    private void solicitarLembretes(Consulta consulta, VacinaTratamento registro, MedicamentoPrescrito m) {
        if (!m.temAgenda()) {
            return;
        }
        var pet = consulta.getPet();
        LocalDateTime agora = LocalDateTime.now(FUSO_DA_CLINICA);
        eventos.publishEvent(new LembreteSolicitado(
                pet.getResponsavelId(), pet.getId(), TipoLembrete.MEDICAMENTO, agora.toLocalDate(),
                "Hora de dar %s a %s".formatted(m.nome().strip(), pet.getNome()),
                registro.getId(), "VacinaTratamento", m.vezesPorDia(), m.duracaoDias(), agora));
    }

    private void refletirExames(Consulta consulta, List<ExameSolicitado> pedidos) {
        Set<String> jaRegistrados = exames.findAllByConsultaId(consulta.getId()).stream()
                .map(e -> chave(e.getTipo()))
                .collect(Collectors.toSet());

        pedidos.stream()
                .filter(p -> jaRegistrados.add(chave(p.nome())))
                .forEach(p -> exames.save(Exame.builder()
                        .tipo(limitar(p.nome()))
                        .data(LocalDate.now())
                        .pet(consulta.getPet())
                        .consulta(consulta)
                        .build()));
    }

    /** Quantidade e observação do medicamento numa linha só, já que o registro de tratamento tem um campo de observações. */
    private String observacoesDe(MedicamentoPrescrito m) {
        return Stream.of(vazio(m.quantidade()) ? null : "Qtd.: " + m.quantidade(), m.observacoes())
                .filter(t -> !vazio(t))
                .collect(Collectors.joining(" · "));
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String chave(String nome) {
        return nome == null ? "" : nome.strip().toLowerCase(Locale.ROOT);
    }

    private static String limitar(String texto) {
        if (vazio(texto)) {
            return null;
        }
        String limpo = texto.strip();
        return limpo.length() <= LIMITE_DA_COLUNA ? limpo : limpo.substring(0, LIMITE_DA_COLUNA - 1) + "…";
    }
}
