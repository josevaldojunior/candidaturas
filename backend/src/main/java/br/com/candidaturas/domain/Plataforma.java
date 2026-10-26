package br.com.candidaturas.domain;

/**
 * Onde a vaga foi encontrada. OUTRO cobre e-mail, WhatsApp, sites próprios e valores desconhecidos do agente.
 * A ordem das constantes é a ordem de exibição (filtros, formulário e dashboard); no banco fica gravado o nome.
 */
public enum Plataforma {
    GUPY,
    LINKEDIN_VAGAS,
    LINKEDIN_FEED,
    INDEED,
    PROGRAMATHOR,
    REMOTAR,
    INFOJOBS,
    COODESH,
    GEEKHUNTER,
    CATHO,
    GITHUB,
    OUTRO
}
