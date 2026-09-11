package fiap.pethub.controller.web;

import fiap.pethub.dto.request.DiagnosticoRequest;
import fiap.pethub.dto.request.PedidoMedicoRequest;
import fiap.pethub.dto.response.ConsultaResponse;
import fiap.pethub.dto.response.DiagnosticoResponse;
import fiap.pethub.dto.response.PedidoMedicoResponse;
import fiap.pethub.enums.StatusPedidoMedico;
import fiap.pethub.enums.TipoPedidoMedico;
import fiap.pethub.security.UsuarioAutenticado;
import fiap.pethub.service.ConsultaService;
import fiap.pethub.service.DiagnosticoService;
import fiap.pethub.service.PedidoMedicoService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/vet")
@RequiredArgsConstructor
public class WebVetController {

    private final ConsultaService consultaService;
    private final DiagnosticoService diagnosticoService;
    private final PedidoMedicoService pedidoMedicoService;
    private final fiap.pethub.service.PetService petService;
    private final fiap.pethub.service.VeterinarioService veterinarioService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario, Pageable pageable) {
        // Busca todas as consultas vinculadas ao veterinário logado
        Page<ConsultaResponse> consultas = consultaService.findAll(null, usuario.getId(), null, pageable);
        model.addAttribute("consultas", consultas.getContent());
        model.addAttribute("nome", usuario.getNome());
        return "vet/dashboard";
    }

    @GetMapping("/consultas/nova")
    public String novaConsultaForm(Model model, Pageable pageable) {
        model.addAttribute("consultaForm", new ConsultaForm());
        model.addAttribute("pets", petService.findAll(null, null, pageable).getContent());
        model.addAttribute("tiposConsulta", fiap.pethub.enums.TipoConsulta.values());
        return "vet/nova-consulta";
    }

    @PostMapping("/consultas/nova")
    public String agendarConsulta(@Valid @ModelAttribute("consultaForm") ConsultaForm form,
                                  BindingResult result,
                                  @AuthenticationPrincipal UsuarioAutenticado usuario,
                                  Model model, Pageable pageable) {
        if (result.hasErrors()) {
            model.addAttribute("pets", petService.findAll(null, null, pageable).getContent());
            model.addAttribute("tiposConsulta", fiap.pethub.enums.TipoConsulta.values());
            return "vet/nova-consulta";
        }
        
        fiap.pethub.dto.request.ConsultaRequest request = new fiap.pethub.dto.request.ConsultaRequest(
                form.getDataHora(),
                form.getTipo(),
                form.getMotivo(),
                fiap.pethub.enums.StatusConsulta.AGENDADA,
                form.getPetId(),
                usuario.getId(),
                null // unidadeId
        );
        
        consultaService.create(request);
        return "redirect:/vet/dashboard";
    }

    @GetMapping("/consultas/{id}")
    public String detalhesConsulta(@PathVariable Long id, Model model, Pageable pageable) {
        ConsultaResponse consulta = consultaService.findById(id);
        
        model.addAttribute("consulta", consulta);
        model.addAttribute("prontuarioForm", new ProntuarioForm());
        model.addAttribute("tiposPedido", TipoPedidoMedico.values());
        
        return "vet/consulta-detalhes";
    }

    @PostMapping("/consultas/{id}/prontuario")
    public String salvarProntuario(@PathVariable Long id,
                                   @Valid @ModelAttribute("prontuarioForm") ProntuarioForm form,
                                   BindingResult result,
                                   Model model) {
        ConsultaResponse consulta = consultaService.findById(id);
        
        if (result.hasErrors()) {
            model.addAttribute("consulta", consulta);
            model.addAttribute("tiposPedido", TipoPedidoMedico.values());
            return "vet/consulta-detalhes";
        }
        
        // 1. Salva Diagnóstico (sempre)
        DiagnosticoRequest diagRequest = new DiagnosticoRequest(
                consulta.getPetId(),
                consulta.getId(),
                LocalDateTime.now(),
                form.getSintoma1(),
                null, null, null, null,
                false, false, false, false, false, false, false, false, false,
                null, null,
                form.getDoencaPredita(),
                null, null
        );
        diagnosticoService.create(diagRequest);
        
        // 2. Salva Pedido Médico apenas se o tipo foi selecionado e houver descrição
        if (form.getTipo() != null && form.getDescricao() != null && !form.getDescricao().trim().isEmpty()) {
            PedidoMedicoRequest pedidoRequest = new PedidoMedicoRequest(
                    consulta.getId(),
                    consulta.getPetId(),
                    form.getTipo(),
                    form.getDescricao(),
                    form.getInstrucoes(),
                    LocalDate.now().plusDays(7),
                    StatusPedidoMedico.PENDENTE
            );
            pedidoMedicoService.create(pedidoRequest);
        }
        
        // Atualiza a consulta para REALIZADA? Opcional. Se tiver, poderiamos chamar consultaService.updateStatus(id, REALIZADA)
        
        return "redirect:/vet/dashboard";
    }

    @Getter
    @Setter
    public static class ProntuarioForm {
        @jakarta.validation.constraints.NotBlank(message = "Sintoma é obrigatório")
        private String sintoma1;
        
        private String doencaPredita;
        
        // Pedido Medico (opcional)
        private TipoPedidoMedico tipo;
        private String descricao;
        private String instrucoes;
    }

    @Getter
    @Setter
    public static class ConsultaForm {
        private Long petId;
        private LocalDateTime dataHora;
        private fiap.pethub.enums.TipoConsulta tipo;
        private String motivo;
    }

    @GetMapping("/perfil")
    public String perfilVet(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        fiap.pethub.dto.response.VeterinarioResponse resp = veterinarioService.findById(usuario.getId());
        VetForm form = new VetForm();
        form.setNome(resp.getNome());
        form.setCrmv(resp.getCrmv());
        form.setEmail(resp.getEmail());
        form.setEspecialidade(resp.getEspecialidade());
        model.addAttribute("vetForm", form);
        return "vet/perfil";
    }

    @PostMapping("/perfil/editar")
    public String editarVet(@Valid @ModelAttribute("vetForm") VetForm form, BindingResult result, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            return "vet/perfil";
        }
        fiap.pethub.dto.request.VeterinarioRequest request =    new fiap.pethub.dto.request.VeterinarioRequest(
            form.getNome(), form.getCrmv(), form.getEspecialidade(), form.getEmail(), null, form.getSenha(), true, null
        );
        veterinarioService.update(usuario.getId(), request);
        return "redirect:/vet/dashboard";
    }

    @Getter
    @Setter
    public static class VetForm {
        @jakarta.validation.constraints.NotBlank(message = "Nome é obrigatório")
        private String nome;
        @jakarta.validation.constraints.NotBlank(message = "CRMV é obrigatório")
        private String crmv;
        @jakarta.validation.constraints.NotBlank(message = "E-mail é obrigatório")
        private String email;
        private String especialidade;
        @jakarta.validation.constraints.NotBlank(message = "Senha é obrigatória para atualizar o perfil")
        private String senha;
    }
    @PostMapping("/perfil/deletar")
    public String deletarVet(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        veterinarioService.delete(usuario.getId());
        return "redirect:/logout";
    }
}
