package fiap.pethub.messaging;

import fiap.pethub.enums.TipoLembrete;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pedido para criar um lembrete do responsável.
 *
 * É ao mesmo tempo o evento interno e o corpo JSON da mensagem: os campos têm exatamente os
 * nomes do {@code CriarLembreteDto} do .NET, que é quem cria o lembrete ao consumir a fila.
 *
 * @param referenciaId   id do registro que originou o lembrete (consulta, pedido, vacina)
 * @param referenciaTipo nome desse registro, para o .NET não criar o mesmo lembrete duas vezes
 * @param vezesPorDia    só em medicamentos: doses por dia (o .NET gera um lembrete por dose)
 * @param duracaoDias    só em medicamentos: duração do tratamento em dias
 * @param cancelar       true = apaga as doses ainda não liberadas desse registro (medicamento removido)
 * @param inicio         só em medicamentos: quando a receita foi salva, no horário de São Paulo
 */
public record LembreteSolicitado(Long responsavelId,
                                 Long petId,
                                 TipoLembrete tipo,
                                 LocalDate dataAgendada,
                                 String mensagem,
                                 Long referenciaId,
                                 String referenciaTipo,
                                 Integer vezesPorDia,
                                 Integer duracaoDias,
                                 LocalDateTime inicio,
                                 boolean cancelar) {

    /** Agenda de doses de um medicamento (vezes por dia x dias, a partir de {@code inicio}). */
    public LembreteSolicitado(Long responsavelId, Long petId, TipoLembrete tipo, LocalDate dataAgendada,
                              String mensagem, Long referenciaId, String referenciaTipo,
                              Integer vezesPorDia, Integer duracaoDias, LocalDateTime inicio) {
        this(responsavelId, petId, tipo, dataAgendada, mensagem, referenciaId, referenciaTipo,
                vezesPorDia, duracaoDias, inicio, false);
    }

    /** Pede ao .NET que apague as doses pendentes de um tratamento que deixou de existir. */
    public static LembreteSolicitado cancelamento(Long responsavelId, Long petId, Long tratamentoId) {
        return new LembreteSolicitado(responsavelId, petId, TipoLembrete.MEDICAMENTO, LocalDate.now(),
                "Tratamento removido", tratamentoId, "VacinaTratamento", null, null, null, true);
    }

    /** Lembrete de um dia só (consulta, exame, vacina): sem agenda de doses. */
    public LembreteSolicitado(Long responsavelId, Long petId, TipoLembrete tipo, LocalDate dataAgendada,
                              String mensagem, Long referenciaId, String referenciaTipo) {
        this(responsavelId, petId, tipo, dataAgendada, mensagem, referenciaId, referenciaTipo, null, null, null, false);
    }
}
