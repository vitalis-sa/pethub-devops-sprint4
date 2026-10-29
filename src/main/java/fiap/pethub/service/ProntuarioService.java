package fiap.pethub.service;

import fiap.pethub.dto.request.DiagnosticoRequest;
import fiap.pethub.dto.request.PedidoMedicoRequest;
import fiap.pethub.dto.request.ProntuarioRequest;
import fiap.pethub.dto.response.ConsultaResponse;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.enums.StatusPedidoMedico;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.repository.DiagnosticoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fecha uma consulta: registra o diagnóstico, emite o pedido médico quando há
 * um, e dá a consulta por realizada.
 *
 * É um fluxo, não um CRUD — três escritas que só fazem sentido juntas. Estava
 * no controller, onde uma falha no meio deixava diagnóstico gravado e consulta
 * ainda em aberto. Aqui as três compartilham a mesma transação: ou tudo entra,
 * ou nada entra.
 */
@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private static final int PRAZO_PADRAO_DO_PEDIDO_EM_DIAS = 7;

    private final ConsultaService consultaService;
    private final DiagnosticoService diagnosticoService;
    private final PedidoMedicoService pedidoMedicoService;
    private final DiagnosticoRepository diagnosticoRepository;

    @Transactional
    public void registrar(Long consultaId, ProntuarioRequest request) {
        ConsultaResponse consulta = consultaService.findById(consultaId);
        recusarSeJaFechada(consulta);

        diagnosticoService.create(montarDiagnostico(consulta, request));
        if (request.temPedidoMedico()) {
            pedidoMedicoService.create(montarPedido(consulta, request));
        }
        consultaService.concluir(consultaId);
    }

    /**
     * Sem esta guarda, reenviar o formulário — um F5 depois de salvar já basta —
     * empilharia um segundo diagnóstico na mesma consulta.
     */
    private void recusarSeJaFechada(ConsultaResponse consulta) {
        boolean jaRealizada = consulta.getStatus() == StatusConsulta.REALIZADA;
        boolean jaTemDiagnostico = diagnosticoRepository.existsByConsultaId(consulta.getId());
        if (jaRealizada || jaTemDiagnostico) {
            throw new RegraDeNegocioException(
                    "O prontuário desta consulta já foi registrado e não pode ser preenchido de novo.");
        }
    }

    private DiagnosticoRequest montarDiagnostico(ConsultaResponse consulta, ProntuarioRequest request) {
        DiagnosticoRequest diagnostico = new DiagnosticoRequest();
        diagnostico.setPetId(consulta.getPetId());
        diagnostico.setConsultaId(consulta.getId());
        diagnostico.setData(LocalDateTime.now());
        diagnostico.setSintoma1(request.getSintoma1());
        diagnostico.setDoencaPredita(request.getDoencaPredita());
        return diagnostico;
    }

    private PedidoMedicoRequest montarPedido(ConsultaResponse consulta, ProntuarioRequest request) {
        return new PedidoMedicoRequest(
                consulta.getId(),
                consulta.getPetId(),
                request.getTipo(),
                request.getDescricao(),
                request.getInstrucoes(),
                LocalDate.now().plusDays(PRAZO_PADRAO_DO_PEDIDO_EM_DIAS),
                StatusPedidoMedico.PENDENTE);
    }
}
