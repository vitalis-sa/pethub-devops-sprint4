package fiap.pethub.controller.web;

import fiap.pethub.dto.request.PetRequest;
import fiap.pethub.dto.response.ConsultaResponse;
import fiap.pethub.dto.response.PetResponse;
import fiap.pethub.dto.response.ResponsavelResponse;
import fiap.pethub.security.UsuarioAutenticado;
import fiap.pethub.service.ConsultaService;
import fiap.pethub.service.PetService;
import fiap.pethub.service.ResponsavelService;
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

@Controller
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class WebTutorController {

    private final PetService petService;
    private final ResponsavelService responsavelService;
    private final ConsultaService consultaService;
    private final fiap.pethub.service.DiagnosticoService diagnosticoService;
    private final fiap.pethub.service.PedidoMedicoService pedidoMedicoService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario, Pageable pageable) {
        // O PetService já filtra os pets pelo usuário logado usando o EscopoDoUsuario.
        Page<PetResponse> pets = petService.findAll(null, null, pageable);
        model.addAttribute("pets", pets.getContent());
        model.addAttribute("nome", usuario.getNome());
        return "tutor/dashboard";
    }

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
        
        // Pega o CPF do responsável logado para enviar na requisição
        ResponsavelResponse resp = responsavelService.findById(usuario.getId());
        
        PetRequest request = new PetRequest(
                petForm.getNome(),
                petForm.getEspecie(),
                petForm.getRaca(),
                petForm.getIdade(),
                petForm.getPeso(),
                petForm.getGenero(),
                resp.getCpf(),
                null // veterinarioResponsavelId (opcional)
        );
        
        petService.create(request);
        return "redirect:/tutor/dashboard";
    }

    @GetMapping("/pets/{id}")
    public String detalhesPet(@PathVariable Long id, Model model, Pageable pageable) {
        PetResponse pet = petService.findById(id);
        Page<fiap.pethub.dto.response.ConsultaResponse> consultas = consultaService.findAll(id, null, null, pageable);
        Page<fiap.pethub.dto.response.DiagnosticoResponse> diagnosticos = diagnosticoService.findAll(id, null, pageable);
        Page<fiap.pethub.dto.response.PedidoMedicoResponse> exames = pedidoMedicoService.findAll(id, null, null, pageable);
        
        model.addAttribute("pet", pet);
        model.addAttribute("consultas", consultas.getContent());
        model.addAttribute("diagnosticos", diagnosticos.getContent());
        model.addAttribute("exames", exames.getContent());
        return "tutor/pet-detalhes";
    }
    @PostMapping("/pets/{id}/deletar")
    public String deletarPet(@PathVariable Long id) {
        petService.delete(id);
        return "redirect:/tutor/dashboard";
    }

    @GetMapping("/pets/{id}/editar")
    public String editarPetForm(@PathVariable Long id, Model model) {
        PetResponse pet = petService.findById(id);
        PetForm form = new PetForm();
        form.setNome(pet.getNome());
        form.setEspecie(pet.getEspecie());
        form.setRaca(pet.getRaca());
        form.setIdade(pet.getIdade());
        form.setPeso(pet.getPeso());
        form.setGenero(pet.getGenero());
        
        model.addAttribute("petForm", form);
        model.addAttribute("petId", id);
        return "tutor/editar-pet";
    }

    @PostMapping("/pets/{id}/editar")
    public String editarPet(@PathVariable Long id, @Valid @ModelAttribute("petForm") PetForm petForm,
                            BindingResult result, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            return "tutor/editar-pet";
        }
        ResponsavelResponse resp = responsavelService.findById(usuario.getId());
        PetRequest request = new PetRequest(petForm.getNome(), petForm.getEspecie(), petForm.getRaca(), 
                                            petForm.getIdade(), petForm.getPeso(), petForm.getGenero(), 
                                            resp.getCpf(), null);
        petService.update(id, request);
        return "redirect:/tutor/dashboard";
    }

    @PostMapping("/perfil/deletar")
    public String deletarTutor(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        responsavelService.delete(usuario.getId());
        return "redirect:/logout";
    }

    @GetMapping("/perfil")
    public String perfilTutor(Model model, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ResponsavelResponse resp = responsavelService.findById(usuario.getId());
        TutorForm form = new TutorForm();
        form.setNome(resp.getNome());
        form.setCpf(resp.getCpf());
        form.setEmail(resp.getEmail());
        model.addAttribute("tutorForm", form);
        return "tutor/perfil";
    }

    @PostMapping("/perfil/editar")
    public String editarTutor(@Valid @ModelAttribute("tutorForm") TutorForm form, BindingResult result, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (result.hasErrors()) {
            return "tutor/perfil";
        }
        fiap.pethub.dto.request.ResponsavelRequest request = new fiap.pethub.dto.request.ResponsavelRequest(
            form.getNome(), form.getCpf(), form.getEmail(), form.getSenha(), true
        );
        responsavelService.update(usuario.getId(), request);
        return "redirect:/tutor/dashboard";
    }

    @Getter
    @Setter
    public static class PetForm {
        private String nome;
        private String especie;
        private String raca;
        private Integer idade;
        private Double peso;
        private String genero;
    }

    @Getter
    @Setter
    public static class TutorForm {
        @jakarta.validation.constraints.NotBlank(message = "Nome é obrigatório")
        private String nome;
        @jakarta.validation.constraints.NotBlank(message = "CPF é obrigatório")
        private String cpf;
        @jakarta.validation.constraints.NotBlank(message = "E-mail é obrigatório")
        private String email;
        @jakarta.validation.constraints.NotBlank(message = "Senha é obrigatória para atualizar o perfil")
        private String senha;
    }
}
