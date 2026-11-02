-- =====================================================================
-- SELECTs de evidência do CRUD do vídeo (Oracle da FIAP, schema do Azure).
-- Rode cada bloco logo depois da chamada correspondente em docs/demo-crud.http.
-- =====================================================================

-- O Flyway aplicou a V1 no primeiro boot do Web App
SELECT "version", "description", "installed_on", "success"
  FROM "flyway_schema_history"
 ORDER BY "installed_rank";

-- ---------------------------------------------------------------------
-- CRUD 1 - UNIDADE VETERINÁRIA (TB_UNIDADE_VETERINARIO)
-- ---------------------------------------------------------------------
-- Depois de 1.1 (inserir) e 1.3 (atualizar): a linha aparece / muda.
-- Depois de 1.4 (deletar): a consulta volta vazia.
SELECT id, nome, logradouro, numero, bairro, cidade, estado, cep
  FROM tb_unidade_veterinario
 WHERE cep = '05422001';

-- Visão geral (as duas unidades iniciais são criadas no boot pela aplicação)
SELECT id, nome, cidade FROM tb_unidade_veterinario ORDER BY id;

-- ---------------------------------------------------------------------
-- CRUD 2 - VETERINÁRIO (TB_VETERINARIO)
-- ---------------------------------------------------------------------
-- Depois de 2.1 (inserir) e 2.3 (atualizar): a linha aparece / muda.
-- Depois de 2.4 (deletar): a consulta volta vazia.
-- A senha é gravada como hash BCrypt, nunca em texto puro.
SELECT id, nome, crmv, especialidade, email, telefone, ativo, SUBSTR(senha, 1, 7) || '...' AS senha_bcrypt
  FROM tb_veterinario
 WHERE crmv = 'SP-90002';

-- Visão geral
SELECT id, nome, crmv, especialidade FROM tb_veterinario ORDER BY id;
