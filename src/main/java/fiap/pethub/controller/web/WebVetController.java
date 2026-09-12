package fiap.pethub.controller.web;

import fiap.pethub.dto.request.ConsultaRequest;
import fiap.pethub.dto.request.ProntuarioRequest;
import fiap.pethub.dto.request.VeterinarioRequest;
import fiap.pethub.dto.response.ConsultaResponse;
import fiap.pethub.dto.response.VeterinarioResponse;
import fiap.pethub.enums.StatusConsulta;
import fiap.pethub.enums.TipoConsulta;
import fiap.pethub.enums.TipoPedidoMedico;
import fiap.pethub.exception.RegraDeNegocioException;
import fiap.pethub.security.UsuarioAutenticado;
import fiap.pethub.service.ConsultaService;
import fiap.pethub.service.PetService;
import fiap.pethub.service.ProntuarioService;
import fiap.pethub.service.VeterinarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

/**
 * Telas do veterinário: a agenda dele, o agendamento e o fechamento da consulta.
 *
 * O controller recebe o formulário, entrega ao serviço e escolhe a próxima
 * página. Quem sabe o que é fechar uma consulta é o {@link ProntuarioService}.
 */
@Controller
@RequestMapping("/vet")
@RequiredArgsConstructor
public class WebVetController {

    private final ConsultaService consultaService;
    private final ProntuarioService prontuarioService;
    private final PetService petService;
    private final VeterinarioService veterinarioService;

    // ─── Agenda ───────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario,
                            Pageable pageable) {
        model.addAttribute("consultas",
                consultaService.findAll(null, usuario.getId(), null, pageable).getContent());
        model.addAttribute("nome", usuario.getNome());
        return "vet/dashboard";
    }

    // ─── Agendamento ──────────────────────────────────────────────────────────

    @GetMapping("/consultas/nova")
    public String novaConsultaForm(Model model, Pageable pageable) {
        model.addAttribute("consultaForm", new ConsultaForm());
        prepararFormularioDeConsulta(model, pageable);
        return "vet/nova-consulta";
    }

    @PostMapping("/consultas/nova")
    public String agendarConsulta(@Valid @ModelAttribute("consultaForm") ConsultaForm form,
                                  BindingResult result,
                                  @AuthenticationPrincipal UsuarioAutenticado usuario,
                                  Model model, Pageable pageable) {
        if (result.hasErrors()) {
            prepararFormularioDeConsulta(model, pageable);
            return "vet/nova-consulta";
        }
        try {
            consultaService.create(new ConsultaRequest(
                    form.getDataHora(), form.getTipo(), form.getMotivo(),
                    StatusConsulta.AGENDADA, form.getPetId(), usuario.getId(), null));
        } catch (RegraDeNegocioException e) {
            // Conflito de agenda é erro do horário escolhido, então volta preso
            // ao campo que o veterinário precisa corrigir.
            result.rejectValue("dataHora", "agenda.ocupada", e.getMessage());
            prepararFormularioDeConsulta(model, pageable);
            return "vet/nova-consulta";
        }
        return "redirect:/vet/dashboard";
    }

    // ─── Prontuário ───────────────────────────────────────────────────────────

    @GetMapping("/consultas/{id}")
    public String detalhesConsulta(@PathVariable Long id, Model model) {
        model.addAttribute("consulta", consultaService.findById(id));
        model.addAttribute("prontuarioForm", new ProntuarioRequest());
        model.addAttribute("tiposPedido", TipoPedidoMedico.values());
        return "vet/consulta-detalhes";
    }

    @PostMapping("/consultas/{id}/prontuario")
    public String salvarProntuario(@PathVariable Long id,
                                   @Valid @ModelAttribute("prontuarioForm") ProntuarioRequest form,
                                   BindingResult result,
                                   Model model) {
        if (form.pedidoIncompleto()) {
            result.rejectValue("descricao", "pedido.incompleto",
                    "Para solicitar um exame, preencha o tipo e a descrição.");
        }
        if (result.hasErrors()) {
            return prepararProntuario(id, model);
        }
        try {
            prontuarioService.registrar(id, form);
        } catch (RegraDeNegocioException e) {
            result.reject("prontuario.duplicado", e.getMessage());
            return prepararProntuario(id, model);
        }
        return "redirect:/vet/dashboard";
    }

    // ─── Perfil ───────────────────────────────────────────────────────────────

    @GetMapping("/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        model.addAttribute("vetForm", VetForm.de(veterinarioService.findById(usuario.getId())));
        return "vet/perfil";
    }

    @PostMapping("/perfil/editar")
    public String editarPerfil(@Valid @ModelAttribute("vetForm") VetForm form,
                               BindingResult result,
                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            return "vet/perfil";
        }
        VeterinarioResponse atual = veterinarioService.findById(usuario.getId());
        // O CRMV vem do cadastro, e não do formulário: é registro profissional e
        // a tela o mostra apenas para leitura.
        veterinarioService.update(usuario.getId(), new VeterinarioRequest(
                form.getNome(), atual.getCrmv(), form.getEspecialidade(), form.getEmail(),
                form.getTelefone(), form.getSenha(), true, null));
        return "redirect:/vet/dashboard";
    }

    /**
     * Encerrar a conta do veterinário desativa o acesso em vez de apagar o
     * cadastro — o nome dele assina prontuários que continuam valendo.
     */
    @PostMapping("/perfil/deletar")
    public String encerrarConta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        veterinarioService.desativar(usuario.getId());
        return "redirect:/logout";
    }

    private void prepararFormularioDeConsulta(Model model, Pageable pageable) {
        model.addAttribute("pets", petService.findAll(null, null, pageable).getContent());
        model.addAttribute("tiposConsulta", TipoConsulta.values());
    }

    /** Recarrega o que a tela do prontuário precisa quando o formulário volta com erro. */
    private String prepararProntuario(Long consultaId, Model model) {
        ConsultaResponse consulta = consultaService.findById(consultaId);
        model.addAttribute("consulta", consulta);
        model.addAttribute("tiposPedido", TipoPedidoMedico.values());
        return "vet/consulta-detalhes";
    }

    @Getter
    @Setter
    public static class ConsultaForm {

        @NotNull(message = "Selecione o pet")
        private Long petId;

        @NotNull(message = "Informe a data e a hora")
        @Future(message = "A consulta precisa ser marcada para uma data futura")
        private LocalDateTime dataHora;

        @NotNull(message = "Selecione o tipo da consulta")
        private TipoConsulta tipo;

        @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres")
        private String motivo;
    }

    @Getter
    @Setter
    public static class VetForm {

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        private String nome;

        /** Só de leitura na tela; vai junto para que o formulário volte preenchido. */
        private String crmv;

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        private String email;

        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        private String especialidade;

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        private String telefone;

        @NotBlank(message = "Confirme a senha para salvar as alterações")
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres")
        private String senha;

        static VetForm de(VeterinarioResponse vet) {
            VetForm form = new VetForm();
            form.setNome(vet.getNome());
            form.setCrmv(vet.getCrmv());
            form.setEmail(vet.getEmail());
            form.setEspecialidade(vet.getEspecialidade());
            form.setTelefone(vet.getTelefone());
            return form;
        }
    }
}
