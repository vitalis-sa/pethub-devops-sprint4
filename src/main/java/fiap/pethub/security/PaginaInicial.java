package fiap.pethub.security;

import fiap.pethub.enums.Perfil;

/**
 * Onde cada perfil começa a navegar depois de autenticar.
 *
 * Duas partes do sistema precisam desta resposta — o redirecionamento após o
 * login e a rota raiz — e elas não podem discordar. Por isso a regra mora aqui,
 * e não copiada nas duas.
 */
public final class PaginaInicial {

    private static final String DASHBOARD_VETERINARIO = "/vet/dashboard";
    private static final String DASHBOARD_TUTOR = "/tutor/dashboard";

    private PaginaInicial() {
    }

    public static String de(Perfil perfil) {
        return perfil == Perfil.VETERINARIO ? DASHBOARD_VETERINARIO : DASHBOARD_TUTOR;
    }
}
