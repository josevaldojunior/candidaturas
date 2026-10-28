-- Usuário dono dos dados nos testes de integração (id alto para não colidir com os gerados).
-- Os testes entram com um JWT simulado (subject 1000); a senha não é usada, por isso o hash é inválido.
INSERT INTO usuario (id, nome, email, senha_hash, sem_retorno_dias, criado_em)
VALUES (1000, 'Pessoa de Teste', 'teste@exemplo.com', 'nao-usado-para-login', 30, TIMESTAMP '2026-10-01 09:00:00');

INSERT INTO plataforma (usuario_id,codigo,nome,nome_chave,ordem,ativa) SELECT u.id,p.codigo,p.nome,p.nome_chave,p.ordem,TRUE FROM usuario u CROSS JOIN (VALUES
('GUPY','Gupy','GUPY',0),
('LINKEDIN_VAGAS','LinkedIn Vagas','LINKEDINVAGAS',1),
('LINKEDIN_FEED','LinkedIn Feed','LINKEDINFEED',2),
('INDEED','Indeed','INDEED',3),
('PROGRAMATHOR','Programathor','PROGRAMATHOR',4),
('REMOTAR','Remotar','REMOTAR',5),
('INFOJOBS','InfoJobs','INFOJOBS',6),
('COODESH','Coodesh','COODESH',7),
('GEEKHUNTER','GeekHunter','GEEKHUNTER',8),
('CATHO','Catho','CATHO',9),
('GITHUB','Repositórios GitHub','REPOSITORIOSGITHUB',10),
('SOLIDES','Sólides','SOLIDES',11),
('OUTRO','Outros','OUTROS',12)
) AS p(codigo,nome,nome_chave,ordem);
