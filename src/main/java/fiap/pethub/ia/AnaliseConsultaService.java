package fiap.pethub.ia;

import fiap.pethub.dto.request.AprovarAnaliseRequest;
import fiap.pethub.dto.request.ExameSolicitado;
import fiap.pethub.dto.request.MedicamentoPrescrito;
import fiap.pethub.dto.response.AnaliseConsultaResponse;
import fiap.pethub.dto.response.RascunhoAnaliseResponse;
import fiap.pethub.dto.response.ReceitaResponse;
import fiap.pethub.entity.AnaliseConsulta;
import fiap.pethub.entity.Consulta;
import fiap.pethub.entity.Pet;
import fiap.pethub.entity.UnidadeVeterinario;
import fiap.pethub.entity.Veterinario;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.exception.IaIndisponivelException;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.exception.ResourceNotFoundException;
import fiap.pethub.repository.AnaliseConsultaRepository;
import fiap.pethub.repository.ConsultaRepository;
import fiap.pethub.security.EscopoDoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fluxo do atendimento (anamnese, receita, pedido de exames e sugestão):
 * 1. {@link #gerarRascunho}: a IA sugere os textos a partir da transcrição; nada é gravado.
 * 2. {@link #aprovar}: o veterinário aceita ou corrige e só então tudo é gravado; a consulta passa a realizada.
 * 3. {@link #receita}: a receita aprovada fica disponível ao veterinário e ao responsável do pet.
 *
 * Não é transacional de propósito: a chamada ao modelo leva segundos, e segurar uma conexão do
 * banco durante a espera esgotaria o pool (são só duas). Cada leitura e gravação já é uma transação curta.
 */
@Service
@RequiredArgsConstructor
public class AnaliseConsultaService {

    private final ConsultaRepository consultaRepository;
    private final AnaliseConsultaRepository repository;
    private final AnaliseConsultaMapper mapper;
    private final GroqClient groq;
    private final ProntuarioDaConsulta prontuario;
    private final EscopoDoUsuario escopo;

    /** Formato que o modelo devolve, conforme o esquema de {@link PromptIa#ANALISE_CONSULTA}. */
    public record SaidaAnalise(String anamnese, List<MedicamentoPrescrito> medicamentos, List<ExameSolicitado> exames, String sugestao) {

        /** Medicamentos e exames podem ser vazios (nada foi prescrito); os textos, não. */
        boolean completa() {
            return Stream.of(anamnese, sugestao).noneMatch(texto -> texto == null || texto.isBlank());
        }
    }

    public RascunhoAnaliseResponse gerarRascunho(Long consultaId, String transcricao) {
        Consulta consulta = consultaDoUsuario(consultaId);
        exigirAberta(consulta);

        SaidaAnalise saida = groq.gerar(PromptIa.ANALISE_CONSULTA, transcricao, SaidaAnalise.class);
        if (!saida.completa()) {
            throw new IaIndisponivelException("A resposta do serviço de IA veio incompleta.");
        }
        return new RascunhoAnaliseResponse(consultaId, saida.anamnese(),
                Prescricao.limparMedicamentos(saida.medicamentos()), Prescricao.limparExames(saida.exames()),
                saida.sugestao(), groq.modelo());
    }

    /**
     * Grava (ou substitui) a anamnese, a receita e o pedido de exames revisados pelo veterinário e os reflete
     * no prontuário do pet (Tratamentos e Exames).
     */
    @Caching(evict = {
            @CacheEvict(value = "vacinas", allEntries = true),
            @CacheEvict(value = "exames", allEntries = true),
            @CacheEvict(value = "consultas", allEntries = true)
    })
    public AnaliseConsultaResponse aprovar(Long consultaId, AprovarAnaliseRequest aprovada) {
        Consulta consulta = consultaDoUsuario(consultaId);
        exigirAberta(consulta);

        AnaliseConsulta analise = repository.findByConsultaId(consultaId)
                .orElseGet(() -> AnaliseConsulta.builder().consulta(consulta).build());
        mapper.atualizar(aprovada, analise);
        analise.setReceita(Prescricao.escrever(Prescricao.limparMedicamentos(aprovada.medicamentos())));
        analise.setExames(Prescricao.escrever(Prescricao.limparExames(aprovada.exames())));
        analise.setAprovadaEm(LocalDateTime.now());
        AnaliseConsultaResponse resposta = paraResposta(repository.save(analise));
        prontuario.refletir(consulta, resposta.medicamentos(), resposta.exames());
        // Aprovados os documentos, o atendimento acabou: a consulta vira realizada e não aceita mais análise.
        consulta.setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(consulta);
        return resposta;
    }

    public AnaliseConsultaResponse buscar(Long consultaId) {
        return paraResposta(analiseDe(consultaId));
    }

    /** Receita aprovada, ou vazio enquanto o veterinário ainda não salvou (não é erro: o app só pergunta). */
    public Optional<ReceitaResponse> receitaSeExistir(Long consultaId) {
        consultaDoUsuario(consultaId);
        return repository.findByConsultaId(consultaId).map(this::paraReceita);
    }

    public ReceitaResponse receita(Long consultaId) {
        return paraReceita(analiseDe(consultaId));
    }

    private AnaliseConsultaResponse paraResposta(AnaliseConsulta analise) {
        return new AnaliseConsultaResponse(analise.getConsulta().getId(), analise.getAnamnese(),
                Prescricao.medicamentos(analise.getReceita()), Prescricao.exames(analise.getExames()),
                analise.getSugestao(), analise.getAprovadaEm());
    }

    private ReceitaResponse paraReceita(AnaliseConsulta analise) {
        Consulta consulta = analise.getConsulta();
        Pet pet = consulta.getPet();
        Veterinario vet = consulta.getVeterinario();
        UnidadeVeterinario unidade = consulta.getUnidade() != null ? consulta.getUnidade() : vet.getUnidade();
        return new ReceitaResponse(consulta.getId(),
                pet.getNome(), pet.getEspecie(), pet.getRaca(), pet.getGenero(), pet.getDataNascimento(), pet.getPeso(),
                pet.getResponsavelId(), pet.getNomeResponsavel(),
                vet.getNome(), vet.getCrmv(), vet.getEspecialidade(), vet.getTelefone(),
                unidade == null ? null : unidade.getNome(), unidade == null ? null : enderecoDe(unidade),
                Prescricao.medicamentos(analise.getReceita()), Prescricao.exames(analise.getExames()), analise.getAprovadaEm());
    }

    private String enderecoDe(UnidadeVeterinario u) {
        String rua = Stream.of(u.getLogradouro(), u.getNumero()).filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(", "));
        String cidade = Stream.of(u.getCidade(), u.getEstado()).filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(" - "));
        return Stream.of(rua, u.getBairro(), cidade, u.getCep() == null || u.getCep().isBlank() ? null : "CEP " + u.getCep())
                .filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(" - "));
    }

    private AnaliseConsulta analiseDe(Long consultaId) {
        consultaDoUsuario(consultaId);
        return repository.findByConsultaId(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Esta consulta ainda não tem anamnese e receita aprovadas."));
    }

    /** Só uma consulta ainda agendada pode ser atendida: cancelada não acontece e realizada já foi fechada. */
    private void exigirAberta(Consulta consulta) {
        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new RegraDeNegocioException("Uma consulta cancelada não pode receber anamnese e receita.");
        }
        if (consulta.getStatus() == StatusConsulta.REALIZADA) {
            throw new RegraDeNegocioException("Esta consulta já foi realizada e não pode mais ser atendida de novo.");
        }
    }

    private Consulta consultaDoUsuario(Long consultaId) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com id: " + consultaId));
        escopo.exigirPosse(consulta.getPet().getResponsavelId());
        return consulta;
    }
}
