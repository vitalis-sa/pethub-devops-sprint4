package fiap.pethub.controller.web;

import fiap.pethub.dto.request.PetRequest;
import fiap.pethub.dto.request.ResponsavelRequest;
import fiap.pethub.dto.response.PetResponse;
import fiap.pethub.dto.response.ResponsavelResponse;
import fiap.pethub.enums.StatusLembrete;
import fiap.pethub.security.UsuarioAutenticado;
import fiap.pethub.service.ConsultaService;
import fiap.pethub.service.DiagnosticoService;
import fiap.pethub.service.LembreteService;
import fiap.pethub.service.PedidoMedicoService;
import fiap.pethub.service.PetService;
import fiap.pethub.service.ResponsavelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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

/**
 * Telas do tutor: seus pets, o prontuário de cada um e os lembretes que o
 * sistema gerou para ele.
 *
 * Os serviços já filtram tudo pelo usuário autenticado através do
 * EscopoDoUsuario, então aqui não há nenhum filtro por dono — repeti-lo seria
 * uma segunda regra de posse, capaz de discordar da primeira.
 */
@Controller
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class WebTutorController {

    private final PetService petService;
    private final ResponsavelService responsavelService;
    private final ConsultaService consultaService;
    private final DiagnosticoService diagnosticoService;
    private final PedidoMedicoService pedidoMedicoService;
    private final LembreteService lembreteService;

    // ─── Painel ───────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario,
                            Pageable pageable) {
        model.addAttribute("pets", petService.findAll(null, null, pageable).getContent());
        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("pendentes", lembreteService
                .findAll(null, null, StatusLembrete.PENDENTE, null, pageable)
                .getTotalElements());
        return "tutor/dashboard";
    }

    // ─── Lembretes ────────────────────────────────────────────────────────────

    /**
     * O tutor nunca pede um lembrete: eles nascem sozinhos quando o veterinário
     * agenda uma consulta, pede um exame ou marca a próxima dose de uma vacina.
     * Esta é a tela onde esse trabalho automático aparece.
     */
    @GetMapping("/lembretes")
    public String lembretes(Model model, Pageable pageable) {
        model.addAttribute("lembretes",
                lembreteService.findAll(null, null, null, null, pageable).getContent());
        return "tutor/lembretes";
    }

    @PostMapping("/lembretes/{id}/visto")
    public String marcarLembreteComoVisto(@PathVariable Long id) {
        lembreteService.updateStatus(id, StatusLembrete.ENVIADO);
        return "redirect:/tutor/lembretes";
    }

    // ─── Pets ─────────────────────────────────────────────────────────────────

    @GetMapping("/pets/novo")
    public String novoPetForm(Model model) {
        model.addAttribute("petForm", new PetForm());
        return "tutor/novo-pet";
    }

    @PostMapping("/pets/novo")
    public String registrarPet(@Valid @ModelAttribute("petForm") PetForm petForm,
                               BindingResult result,
                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            return "tutor/novo-pet";
        }
        petService.create(paraRequest(petForm, usuario));
        return "redirect:/tutor/dashboard";
    }

    @GetMapping("/pets/{id}")
    public String detalhesPet(@PathVariable Long id, Model model, Pageable pageable) {
        model.addAttribute("pet", petService.findById(id));
        model.addAttribute("consultas", consultaService.findAll(id, null, null, pageable).getContent());
        model.addAttribute("diagnosticos", diagnosticoService.findAll(id, null, pageable).getContent());
        model.addAttribute("exames", pedidoMedicoService.findAll(id, null, null, pageable).getContent());
        return "tutor/pet-detalhes";
    }

    @GetMapping("/pets/{id}/editar")
    public String editarPetForm(@PathVariable Long id, Model model) {
        model.addAttribute("petForm", PetForm.de(petService.findById(id)));
        model.addAttribute("petId", id);
        return "tutor/editar-pet";
    }

    @PostMapping("/pets/{id}/editar")
    public String editarPet(@PathVariable Long id,
                            @Valid @ModelAttribute("petForm") PetForm petForm,
                            BindingResult result, Model model,
                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            model.addAttribute("petId", id);
            return "tutor/editar-pet";
        }
        petService.update(id, paraRequest(petForm, usuario));
        return "redirect:/tutor/pets/" + id;
    }

    @PostMapping("/pets/{id}/deletar")
    public String deletarPet(@PathVariable Long id) {
        petService.delete(id);
        return "redirect:/tutor/dashboard";
    }

    // ─── Perfil ───────────────────────────────────────────────────────────────

    @GetMapping("/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        model.addAttribute("tutorForm", TutorForm.de(responsavelService.findById(usuario.getId())));
        return "tutor/perfil";
    }

    @PostMapping("/perfil/editar")
    public String editarPerfil(@Valid @ModelAttribute("tutorForm") TutorForm form,
                               BindingResult result,
                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            return "tutor/perfil";
        }
        responsavelService.update(usuario.getId(), new ResponsavelRequest(
                form.getNome(), form.getCpf(), form.getEmail(), form.getSenha(), true));
        return "redirect:/tutor/dashboard";
    }

    /** Encerrar a conta leva junto os pets e o histórico deles. */
    @PostMapping("/perfil/deletar")
    public String deletarConta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        responsavelService.delete(usuario.getId());
        return "redirect:/logout";
    }

    /**
     * O pet é vinculado ao tutor autenticado, e não ao CPF que viesse no
     * formulário: aceitar um CPF digitado deixaria um tutor cadastrar pet no
     * nome de outro.
     */
    private PetRequest paraRequest(PetForm form, UsuarioAutenticado usuario) {
        ResponsavelResponse dono = responsavelService.findById(usuario.getId());
        return new PetRequest(form.getNome(), form.getEspecie(), form.getRaca(),
                form.getIdade(), form.getPeso(), form.getGenero(), dono.getCpf(), null);
    }

    @Getter
    @Setter
    public static class PetForm {

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        private String nome;

        @NotBlank(message = "Espécie é obrigatória")
        @Size(max = 50, message = "Espécie deve ter no máximo 50 caracteres")
        private String especie;

        @Size(max = 50, message = "Raça deve ter no máximo 50 caracteres")
        private String raca;

        @Min(value = 0, message = "Idade não pode ser negativa")
        @Max(value = 40, message = "Idade deve ser no máximo 40 anos")
        private Integer idade;

        @Positive(message = "Peso deve ser maior que zero")
        @Max(value = 200, message = "Peso deve ser no máximo 200 kg")
        private Double peso;

        @Size(max = 20, message = "Gênero deve ter no máximo 20 caracteres")
        private String genero;

        static PetForm de(PetResponse pet) {
            PetForm form = new PetForm();
            form.setNome(pet.getNome());
            form.setEspecie(pet.getEspecie());
            form.setRaca(pet.getRaca());
            form.setIdade(pet.getIdade());
            form.setPeso(pet.getPeso());
            form.setGenero(pet.getGenero());
            return form;
        }
    }

    @Getter
    @Setter
    public static class TutorForm {

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        private String nome;

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
        private String cpf;

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        private String email;

        @NotBlank(message = "Confirme a senha para salvar as alterações")
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres")
        private String senha;

        static TutorForm de(ResponsavelResponse tutor) {
            TutorForm form = new TutorForm();
            form.setNome(tutor.getNome());
            form.setCpf(tutor.getCpf());
            form.setEmail(tutor.getEmail());
            return form;
        }
    }
}
