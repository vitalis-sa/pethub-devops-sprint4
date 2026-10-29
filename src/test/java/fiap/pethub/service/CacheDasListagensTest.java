package fiap.pethub.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante a fiação do cache das listagens sem subir o Spring: o `findAll` guarda no cache do recurso
 * e toda escrita que mexe nesse cache o esvazia por inteiro. Uma escrita que apagasse só uma chave
 * (`key = "#id"`) deixaria a listagem velha por até 10 minutos.
 */
class CacheDasListagensTest {

    private static final AnnotationCacheOperationSource OPERACOES = new AnnotationCacheOperationSource();

    private static final Map<Class<?>, String> CACHE_DE_CADA_SERVICO = Map.of(
            PetService.class, "pets",
            ConsultaService.class, "consultas",
            ExameService.class, "exames",
            DiagnosticoService.class, "diagnosticos",
            PedidoMedicoService.class, "pedidos",
            VacinaTratamentoService.class, "vacinas",
            VeterinarioService.class, "veterinarios",
            UnidadeVeterinarioService.class, "unidades");

    private static Collection<CacheOperation> operacoes(Class<?> servico, Method metodo) {
        Collection<CacheOperation> operacoes = OPERACOES.getCacheOperations(metodo, servico);
        return operacoes == null ? List.of() : operacoes;
    }

    private static boolean esvaziaPorInteiro(Class<?> servico, Method metodo, String cache) {
        return operacoes(servico, metodo).stream()
                .filter(CacheEvictOperation.class::isInstance)
                .map(CacheEvictOperation.class::cast)
                .anyMatch(op -> op.isCacheWide() && op.getCacheNames().contains(cache));
    }

    private static List<Method> metodosPublicos(Class<?> servico) {
        return Arrays.stream(servico.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                .toList();
    }

    @Test
    @DisplayName("o findAll de cada recurso é guardado no cache do próprio recurso")
    void listagemFicaNoCache() {
        CACHE_DE_CADA_SERVICO.forEach((servico, cache) -> {
            Method findAll = metodosPublicos(servico).stream()
                    .filter(m -> m.getName().equals("findAll")).findFirst().orElseThrow();

            assertThat(operacoes(servico, findAll))
                    .as("findAll de %s", servico.getSimpleName())
                    .anyMatch(op -> op instanceof CacheableOperation && op.getCacheNames().contains(cache));
        });
    }

    @Test
    @DisplayName("toda escrita que mexe no cache do recurso o esvazia por inteiro, lista incluída")
    void escritaEsvaziaOCacheTodo() {
        CACHE_DE_CADA_SERVICO.forEach((servico, cache) -> metodosPublicos(servico).stream()
                .filter(m -> operacoes(servico, m).stream().anyMatch(CacheEvictOperation.class::isInstance))
                .forEach(m -> assertThat(esvaziaPorInteiro(servico, m, cache))
                        .as("%s.%s precisa esvaziar o cache '%s' por inteiro", servico.getSimpleName(), m.getName(), cache)
                        .isTrue()));
    }

    @Test
    @DisplayName("criar, alterar e apagar sempre esvaziam o cache do recurso")
    void escritasPrincipaisEsvaziam() {
        Set<String> escritas = Set.of("create", "update", "delete");
        CACHE_DE_CADA_SERVICO.forEach((servico, cache) -> metodosPublicos(servico).stream()
                .filter(m -> escritas.contains(m.getName()))
                .forEach(m -> assertThat(esvaziaPorInteiro(servico, m, cache))
                        .as("%s.%s", servico.getSimpleName(), m.getName())
                        .isTrue()));
    }

    @Test
    @DisplayName("mudar ou apagar o pet esvazia também o que depende dele")
    void petLimpaOsDependentes() {
        List<String> dependentes = List.of("consultas", "exames", "vacinas", "diagnosticos", "pedidos");

        for (String nome : List.of("update", "delete", "deleteByResponsavel")) {
            Method metodo = metodosPublicos(PetService.class).stream()
                    .filter(m -> m.getName().equals(nome)).findFirst().orElseThrow();
            dependentes.forEach(cache -> assertThat(esvaziaPorInteiro(PetService.class, metodo, cache))
                    .as("PetService.%s precisa esvaziar '%s'", nome, cache)
                    .isTrue());
        }
    }
}
