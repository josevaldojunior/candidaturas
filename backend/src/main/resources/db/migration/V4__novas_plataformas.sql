-- Nova lista de plataformas: "LinkedIn" vira "LinkedIn Vagas" (as vagas do feed ganham LINKEDIN_FEED)
-- e "Indicação" deixa de existir, passando para OUTRO.
UPDATE candidatura SET plataforma = 'LINKEDIN_VAGAS' WHERE plataforma = 'LINKEDIN';
UPDATE candidatura SET plataforma = 'OUTRO' WHERE plataforma = 'INDICACAO';
