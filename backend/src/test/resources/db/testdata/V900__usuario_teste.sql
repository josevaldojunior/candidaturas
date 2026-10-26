-- Usuário dono dos dados nos testes de integração (id alto para não colidir com os gerados).
-- Os testes entram com um JWT simulado (subject 1000); a senha não é usada, por isso o hash é inválido.
INSERT INTO usuario (id, nome, email, senha_hash, sem_retorno_dias, criado_em)
VALUES (1000, 'Pessoa de Teste', 'teste@exemplo.com', 'nao-usado-para-login', 30, TIMESTAMP '2026-10-01 09:00:00');
