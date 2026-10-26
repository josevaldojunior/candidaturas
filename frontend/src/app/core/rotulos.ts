import { CargoContato, Modalidade, Origem, Plataforma, Prioridade, Status, TipoContrato, TipoInteracao } from './models';

export const STATUS: Status[] = [
  'INTERESSE', 'CANDIDATADO', 'TRIAGEM_RH', 'TESTE_TECNICO', 'ENTREVISTA_TECNICA',
  'ENTREVISTA_FINAL', 'PROPOSTA', 'ACEITA', 'RECUSADA', 'SEM_RETORNO',
];

export const ROTULO_STATUS: Record<Status, string> = {
  INTERESSE: 'Interesse',
  CANDIDATADO: 'Candidatado',
  TRIAGEM_RH: 'Triagem RH',
  TESTE_TECNICO: 'Teste técnico',
  ENTREVISTA_TECNICA: 'Entrevista técnica',
  ENTREVISTA_FINAL: 'Entrevista final',
  PROPOSTA: 'Proposta',
  ACEITA: 'Aceita',
  RECUSADA: 'Recusada',
  SEM_RETORNO: 'Sem retorno',
};

/** Ordem de exibição (a mesma do enum Plataforma no backend). */
export const PLATAFORMAS: Plataforma[] = [
  'GUPY', 'LINKEDIN_VAGAS', 'LINKEDIN_FEED', 'INDEED', 'PROGRAMATHOR', 'REMOTAR', 'INFOJOBS', 'COODESH',
  'GEEKHUNTER', 'CATHO', 'GITHUB', 'OUTRO',
];
export const ROTULO_PLATAFORMA: Record<Plataforma, string> = {
  LINKEDIN_VAGAS: 'LinkedIn Vagas',
  LINKEDIN_FEED: 'LinkedIn Feed',
  GUPY: 'Gupy',
  INDEED: 'Indeed',
  PROGRAMATHOR: 'Programathor',
  REMOTAR: 'Remotar',
  INFOJOBS: 'InfoJobs',
  GITHUB: 'Repositórios GitHub',
  COODESH: 'Coodesh',
  GEEKHUNTER: 'GeekHunter',
  CATHO: 'Catho',
  OUTRO: 'Outros',
};

export const MODALIDADES: Modalidade[] = ['REMOTO', 'HIBRIDO', 'PRESENCIAL'];
export const ROTULO_MODALIDADE: Record<Modalidade, string> = {
  REMOTO: 'Remoto', HIBRIDO: 'Híbrido', PRESENCIAL: 'Presencial',
};

export const TIPOS_CONTRATO: TipoContrato[] = ['CLT', 'PJ', 'OUTRO'];
export const ROTULO_TIPO_CONTRATO: Record<TipoContrato, string> = { CLT: 'CLT', PJ: 'PJ', OUTRO: 'Outro' };

export const PRIORIDADES: Prioridade[] = ['ALTA', 'MEDIA', 'BAIXA'];
export const ROTULO_PRIORIDADE: Record<Prioridade, string> = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };

export const ROTULO_ORIGEM: Record<Origem, string> = { MANUAL: 'Manual', AGENTE: 'Agente', IMPORTACAO: 'Importação' };

export const TIPOS_INTERACAO: TipoInteracao[] = ['EMAIL', 'LIGACAO', 'ENTREVISTA', 'TESTE', 'MENSAGEM', 'OUTRO'];
export const ROTULO_TIPO_INTERACAO: Record<TipoInteracao, string> = {
  EMAIL: 'E-mail', LIGACAO: 'Ligação', ENTREVISTA: 'Entrevista', TESTE: 'Teste', MENSAGEM: 'Mensagem',
  MUDANCA_STATUS: 'Mudança de status', OUTRO: 'Outro',
};

export const CARGOS_CONTATO: CargoContato[] = ['RECRUTADOR', 'TECH_LEAD', 'GESTOR', 'OUTRO'];
export const ROTULO_CARGO_CONTATO: Record<CargoContato, string> = {
  RECRUTADOR: 'Recrutador(a)', TECH_LEAD: 'Tech lead', GESTOR: 'Gestor(a)', OUTRO: 'Outro',
};
