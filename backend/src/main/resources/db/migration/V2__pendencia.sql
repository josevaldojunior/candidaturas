-- Pendência da candidatura (texto livre, opcional). Preenchida à mão ou pelo agente
-- quando as observações começam com "PENDENTE:".
ALTER TABLE candidatura ADD COLUMN pendencia VARCHAR(1000);
