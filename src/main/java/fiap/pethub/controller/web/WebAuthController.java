package fiap.pethub.controller.web;

import fiap.pethub.dto.request.ResponsavelRequest;
import fiap.pethub.dto.request.VeterinarioRequest;
import fiap.pethub.exception.EmailJaCadastradoException;
import fiap.pethub.security.PaginaInicial;
import fiap.pethub.security.UsuarioAutenticado;
import fiap.pethub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Porta de entrada: raiz, login e os dois auto-cadastros.
 */
@Controller
@RequiredArgsConstructor
public class WebAuthController {

    private final AuthService authService;

    /**
     * A raiz não tem tela própria — ela encaminha cada perfil ao seu dashboard.
     * Sem este mapeamento, quem digita o endereço do PetHub sem caminho algum
     * recebe um 404, que é o primeiro endereço que qualquer visitante tenta.
     */
    @GetMapping("/")
    public String raiz(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return "redirect:" + PaginaInicial.de(usuario.getPerfil());
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registrar/tutor")
    public String registrarTutorForm(Model model) {
        model.addAttribute("tutorForm", new ResponsavelRequest());
        return "registrar-tutor";
    }

    @PostMapping("/registrar/tutor")
    public String registrarTutor(@Valid @ModelAttribute("tutorForm") ResponsavelRequest request,
                                 BindingResult result) {
        return registrar(() -> authService.registrarResponsavel(request), result, "registrar-tutor");
    }

    @GetMapping("/registrar/vet")
    public String registrarVetForm(Model model) {
        model.addAttribute("vetForm", new VeterinarioRequest());
        return "registrar-vet";
    }

    @PostMapping("/registrar/vet")
    public String registrarVet(@Valid @ModelAttribute("vetForm") VeterinarioRequest request,
                               BindingResult result) {
        return registrar(() -> authService.registrarVeterinario(request), result, "registrar-vet");
    }

    /**
     * Os dois cadastros só diferem no formulário e no serviço chamado; o que
     * fazer com o resultado é idêntico nos dois.
     *
     * Só o e-mail duplicado volta para o formulário: é o único erro que o
     * usuário consegue corrigir sozinho. Qualquer outro sobe para a página de
     * erro, em vez de ser silenciado aqui.
     */
    private String registrar(Runnable cadastro, BindingResult result, String formulario) {
        if (result.hasErrors()) {
            return formulario;
        }
        try {
            cadastro.run();
            return "redirect:/login?registered";
        } catch (EmailJaCadastradoException e) {
            result.rejectValue("email", "email.duplicado", e.getMessage());
            return formulario;
        }
    }
}
