# PetHub API (Java) — guia para agentes

API REST do **domínio do veterinário** no PetHub: clínicas, veterinários, pets, consultas,
exames, diagnósticos, vacinas, pedidos médicos e leituras do wearable IoT.

Faz parte de um sistema maior. O domínio do **responsável** (login, endereços, contatos,
lembretes) pertence à API .NET `Vitalis`, e ambos compartilham o mesmo banco Oracle.

**Este repositório é a fonte da verdade do código Java** do projeto.

---

## Stack

Java 21 · Spring Boot 4.0.6 · Maven · Spring Data JPA · Oracle (FIAP) · Lombok · MapStruct ·
Caffeine Cache · Springdoc OpenAPI · Spring Security Crypto (só BCrypt, sem filtro de segurança)

## Comandos

```bash
./mvnw spring-boot:run        # sobe em http://localhost:8080
./mvnw clean package          # empacota
./mvnw test                   # testes (hoje só o smoke test de contexto)
```

Swagger UI em `/swagger-ui.html`, OpenAPI em `/api-docs`.

As credenciais do Oracle vêm das variáveis `DB_USER` e `DB_PASSWORD`. A URL do banco está fixa
em `application.properties` apontando para `oracle.fiap.com.br:1521/orcl`.

---

## Arquitetura

Camadas em `src/main/java/fiap/pethub/`:

```
controller/  REST, validação de entrada, anotações Swagger
service/     regra de negócio, cache, transação
repository/  Spring Data JPA
mapper/      MapStruct: entidade <-> DTO
entity/      JPA
dto/request/ e dto/response/
exception/   GlobalExceptionHandler + tipos de erro
config/      CacheConfig, WebConfig, PasswordUtil
enums/
```

Treze entidades. O grafo central é `Responsavel` → `Pet` → `Consulta` → (`Diagnostico`,
`Exame`, `PedidoMedico`, `VacinaTratamento`), com `Veterinario` ligado a `UnidadeVeterinario`
e `LeituraWearable` ligada ao `Pet`.

## Convenções — siga o padrão existente

- **Domínio em português.** Classes, campos e rotas: `VacinaTratamento`, `/api/vacinas-tratamentos`.
- **Injeção por construtor** via `@RequiredArgsConstructor`. Nunca `@Autowired` em campo.
- **Controller é fino**: recebe, delega ao service, devolve `ResponseEntity`. Sem regra de negócio.
- **Todo endpoint documentado** com `@Operation` e `@ApiResponses`, incluindo os códigos de erro.
- **Listagens sempre paginadas**: `Page<T>` + `Pageable` com `@ParameterObject`.
- **Entidades**: `@Table(name = "TB_...")`, id por `@SequenceGenerator` com `SQ_...` e
  `allocationSize = 1`, relacionamentos `@ManyToOne(fetch = LAZY)`.
- **Erros**: lance `ResourceNotFoundException`; o `GlobalExceptionHandler` cuida da resposta.
- **Cache**: `@Cacheable` nas leituras por id, `@CacheEvict` nas escritas. Caffeine, 10 min, 500 entradas.
- **Mapeamento** entidade↔DTO é sempre via MapStruct, nunca à mão no service.

---

## Sprint 3 — o que falta aqui

Checklist completo em `../docs/sprint-3/05-java.md`. Resumo: **frontend (30 pts), Flyway (20),
Spring Security com dois perfis (30), dois fluxos completos não-CRUD (20)**.

Três avisos que valem antes de começar:

1. **Flyway e `ddl-auto` são incompatíveis.** O `application.properties` usa
   `spring.jpa.hibernate.ddl-auto=update`. Ao adicionar Flyway isso precisa virar `validate`,
   senão Hibernate e Flyway disputam o schema.
2. **O pool do Oracle da FIAP é limitado.** `hikari.maximum-pool-size=1` é intencional.
3. **A rubrica desconta por ocorrência**: SOLID -10, DRY -5, Clean Code -5, comentário no lugar
   de refatoração -3. E há avaliação oral individual sobre o próprio código — prefira sempre a
   solução mais simples e explicável.

## Cuidados

- **Não faça commit nem push sem o usuário pedir.**
- **Quando pedido o commit, deixe o usuario e email que já tem, não coloque o do cluade como autor do commit**
- **Nunca coloque credencial em arquivo versionado.**
- O banco é compartilhado com a API `Vitalis` (.NET). Mudança de schema aqui quebra lá.
- Existe um segundo repositório com esta mesma aplicação, `challenge1sem2026-devops`, usado
  na disciplina de DevOps. Ele seguiu uma arquitetura de microsserviços (sem `Responsavel`
  local, com `LembreteClient` chamando a API .NET) e **está divergente deste**. Não sincronize
  os dois por conta própria.
