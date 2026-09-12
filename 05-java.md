# Java Advanced — checklist Sprint 3

**Repo:** `pethub-java/` · **Entrega:** 12/09/2026
**Rubrica:** `enunciado-bruto-05-java-advanced.md`

A Sprint 3 muda o eixo: a API REST já existe desde a Sprint 1/2. Agora o que vale ponto é
**frontend, versionamento de schema e segurança**.

---

## Pontuação (100)

| Item | Pontos | Estado |
|---|---|---|
| Frontend (camada de visualização) | 30 | pronto |
| Flyway | 20 | pronto |
| Spring Security | 30 | pronto |
| Funcionalidades completas | 20 | pronto |

---

## Checklist de entrega

### Frontend (30)

- [x] Camada de visualização construída em Thymeleaf (13 telas)
- [x] Todas as telas carregam, sem rotas ou links quebrados — verificado nos dois perfis
- [x] Rota raiz `/` encaminha cada perfil ao seu painel
- [x] Página de erro própria (`templates/error.html`) para 403, 404 e 500
- [x] Folha de estilo única em `static/css/app.css`
- [x] Formulários com validação no cliente e no servidor

### Flyway (20)

- [x] `flyway-core` e `flyway-database-oracle` no `pom.xml`
- [x] Migration `V1__schema_inicial.sql` cobrindo as 13 entidades
- [x] `spring.jpa.hibernate.ddl-auto=validate`

> Atenção na oral: com `baseline-on-migrate=true` e `baseline-version=1`, a `V1` **não executa**
> no schema da FIAP, que já tinha as tabelas — o Flyway a marca como aplicada. Em banco vazio
> ela roda normalmente.

### Spring Security (30)

- [x] Autenticação nas duas frentes: formulário com sessão no site, JWT na API
- [x] Dois perfis com permissões diferentes: `VETERINARIO` e `RESPONSAVEL`
- [x] Rotas protegidas por perfil — `/vet/**` e `/tutor/**` se recusam mutuamente
- [x] Posse por recurso em `EscopoDoUsuario`: um tutor não enxerga o pet de outro
- [x] Senha com hash BCrypt

### Funcionalidades completas (20)

- [x] **Fluxo A — agendamento com verificação de agenda** (`/vet/consultas/nova`):
      recusa horário já ocupado pelo veterinário e data no passado, e cria o lembrete do tutor
- [x] **Fluxo B — registro de prontuário** (`/vet/consultas/{id}`): grava diagnóstico, emite o
      pedido médico quando há um e conclui a consulta, tudo numa transação; recusa o segundo envio
- [x] Dois fluxos automáticos: vigilância de hidratação às 21h e lembrete de próxima dose
- [x] O tutor vê o resultado em `/tutor/lembretes`
- [x] Validações nos formulários e nos dados

### Entrega

- [x] Repositório público no GitHub
- [x] README com instalação, execução e acesso — inclusive pelo navegador
- [ ] **Vídeo demonstrando a aplicação — máximo 10 minutos**

---

## Roteiro do vídeo

Está em `docs/roteiro-video.md`.

---

## Penalidades (por ocorrência, cumulativas)

| Problema | Desconto |
|---|---|
| Violação evidente de SOLID (método gigante, responsabilidade múltipla) | **-10 por ocorrência** |
| Repetição desnecessária de código (DRY) | -5 por ocorrência |
| Problemas de legibilidade (Clean Code) | -5 por ocorrência |
| Comentário no lugar de refatoração clara | -3 |
| Funcionalidade com comportamento inesperado ou erro evidente | -5 por funcionalidade |
| Página que não carrega ou link quebrado | -5 por página/link |
| Apresentação incompleta ou incoerente com a proposta | -15 |

---

## Avaliação oral individual

Pontos deste repositório que provavelmente serão perguntados:

- **Por que duas filter chains?** O site usa sessão porque quem navega é uma pessoa com
  cookie; a API usa JWT porque quem chama é um aplicativo sem sessão. `@Order(1)` prende a
  primeira em `/api/**`.
- **Por que o `GlobalExceptionHandler` filtra por `@RestController`?** Sem o filtro, um erro
  numa tela devolveria JSON no navegador em vez da página de erro.
- **Por que excluir tutor apaga tudo e excluir veterinário só desativa?** Os dados do tutor são
  dele; o nome do veterinário assina prontuários de outras pessoas, que continuam valendo.
- **Por que a ordem de exclusão em `RemocaoEmCascata`?** Diagnóstico, exame, pedido e vacina
  apontam para a consulta, então ela sai por último.
- **Por que o prontuário é um serviço e não ficou no controller?** São três escritas que
  precisam da mesma transação e de uma guarda contra duplicação.

---

## Convenções já estabelecidas no repositório

- Camadas: `controller` → `service` → `repository`, com `mapper` (MapStruct) entre entidade e DTO
- DTOs separados em `dto/request` e `dto/response`
- Injeção por construtor via `@RequiredArgsConstructor` do Lombok
- Erros centralizados em `exception/GlobalExceptionHandler`
- Todo endpoint de API anotado com `@Operation` e `@ApiResponses`
- Listagens sempre paginadas (`Page<T>` + `Pageable`)
- Entidades com `@SequenceGenerator` e tabelas prefixadas `TB_`
- Domínio em português
