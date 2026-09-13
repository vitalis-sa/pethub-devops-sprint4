-- Seed de Dados para Demonstração (Roteiro do Vídeo)
-- Senha para todos os usuários: 123456

-- 1. Unidade Veterinária (Clínica)
INSERT INTO tb_unidade_veterinario (id, nome, logradouro, numero, bairro, cidade, estado, cep) 
VALUES (sq_unidade_veterinario.nextval, 'Clínica PetHub Central', 'Av. Lins de Vasconcelos', '1222', 'Cambuci', 'São Paulo', 'SP', '01538001');

-- 2. Veterinário (Perfil: VET)
INSERT INTO tb_veterinario (id, nome, email, senha, crmv, especialidade, telefone, ativo, unidade_id) 
VALUES (sq_veterinario.nextval, 'Dr. Roberto', 'vet_video@pethub.com', '$2a$12$6hM0Usksexi1RLswTO9PeerN3l6aedfI9IV5n5.ihn5EAteU65RtG', 'CRMV-SP 77777', 'Clínico Geral', '11999999999', 1, sq_unidade_veterinario.currval);

-- 3. Tutor A (Seu login de demonstração)
INSERT INTO tb_responsavel (id, cpf, nome, email, senha, ativo, created_at) 
VALUES (sq_responsavel.nextval, '80315482088', 'João (Tutor A)', 'joao_video@demo.com', '$2a$12$6hM0Usksexi1RLswTO9PeerN3l6aedfI9IV5n5.ihn5EAteU65RtG', 1, CURRENT_TIMESTAMP);

-- 4. Pet do Tutor A (com histórico)
INSERT INTO tb_pet (id, nome, especie, raca, genero, peso, idade, responsavel_id) 
VALUES (sq_pet.nextval, 'Rex', 'Cachorro', 'Golden Retriever', 'Macho', 30.5, 3, sq_responsavel.currval);

-- Histórico do Pet do Tutor A (Consulta REALIZADA)
INSERT INTO tb_consulta (id, pet_id, veterinario_id, unidade_id, data_hora, status, tipo, observacoes) 
VALUES (sq_consulta.nextval, sq_pet.currval, sq_veterinario.currval, sq_unidade_veterinario.currval, TO_TIMESTAMP('2026-09-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'REALIZADA', 'PRESENCIAL', 'Consulta de rotina anual.');

INSERT INTO tb_diagnostico (id, consulta_id, pet_id, data, claudicacao, diarreia, dificuldade_respiratoria, lesoes_pele, perda_apetite, secrecao_nasal, secrecao_ocular, tosse, vomito, sintoma1, doenca_predita) 
VALUES (sq_diagnostico.nextval, sq_consulta.currval, sq_pet.currval, CURRENT_TIMESTAMP, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'Apatia leve', 'Saudável');

INSERT INTO tb_pedido_medico (id, consulta_id, pet_id, tipo, descricao, status, created_at) 
VALUES (sq_pedido_medico.nextval, sq_consulta.currval, sq_pet.currval, 'EXAME', 'Hemograma Completo', 'CONCLUIDO', CURRENT_TIMESTAMP);

-- 5. Tutor B (Isolamento por posse)
INSERT INTO tb_responsavel (id, cpf, nome, email, senha, ativo, created_at) 
VALUES (sq_responsavel.nextval, '06950644004', 'Maria (Tutor B)', 'maria_video@demo.com', '$2a$12$6hM0Usksexi1RLswTO9PeerN3l6aedfI9IV5n5.ihn5EAteU65RtG', 1, CURRENT_TIMESTAMP);

-- 6. Pet do Tutor B
INSERT INTO tb_pet (id, nome, especie, raca, genero, peso, idade, responsavel_id) 
VALUES (sq_pet.nextval, 'Mimi', 'Gato', 'Siamês', 'Fêmea', 4.2, 2, sq_responsavel.currval);

-- 7. Consulta AGENDADA (para demonstrar conflito no vídeo) - Marcada para o Pet B, com o Vet
INSERT INTO tb_consulta (id, pet_id, veterinario_id, unidade_id, data_hora, status, tipo, observacoes) 
VALUES (sq_consulta.nextval, sq_pet.currval, sq_veterinario.currval, sq_unidade_veterinario.currval, TO_TIMESTAMP('2026-10-10 14:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'AGENDADA', 'PRESENCIAL', 'Check-up geral.');
