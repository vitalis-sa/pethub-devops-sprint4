package fiap.pethub.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Esvazia todos os caches que carregam dados de um pet: o próprio pet e o que depende dele
 * (consultas, exames, vacinas, diagnósticos e pedidos, que também trazem o nome do pet ou
 * o dono). Vai em operações que mudam o pet ou o apagam com o histórico.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(value = "pets", allEntries = true),
        @CacheEvict(value = "consultas", allEntries = true),
        @CacheEvict(value = "exames", allEntries = true),
        @CacheEvict(value = "vacinas", allEntries = true),
        @CacheEvict(value = "diagnosticos", allEntries = true),
        @CacheEvict(value = "pedidos", allEntries = true)
})
public @interface LimpaCachesDoPet {
}
