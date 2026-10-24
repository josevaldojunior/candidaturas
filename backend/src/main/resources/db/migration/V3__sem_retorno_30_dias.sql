-- Prazo padrão do "sem retorno" passa de 21 para 30 dias (o valor fica ajustável na tela de Configurações).
UPDATE configuracao SET valor = '30' WHERE chave = 'sem_retorno.dias';
