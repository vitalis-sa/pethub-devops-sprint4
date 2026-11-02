package fiap.pethub;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe o contexto inteiro e por isso precisa do Oracle e das variáveis do .env.
 * A tag "integracao" deixa o CI do Azure DevOps rodar só os testes unitários
 * ({@code -DexcludedGroups=integracao}), sem levar credenciais de banco ao agente.
 */
@Tag("integracao")
@SpringBootTest
class PethubApplicationTests {

	@Test
	void contextLoads() {
	}

}
