export type Status =
  | 'INTERESSE' | 'CANDIDATADO' | 'TRIAGEM_RH' | 'TESTE_TECNICO' | 'ENTREVISTA_TECNICA'
  | 'ENTREVISTA_FINAL' | 'PROPOSTA' | 'ACEITA' | 'RECUSADA' | 'SEM_RETORNO';
export type Plataforma = 'LINKEDIN' | 'GUPY' | 'INDEED' | 'INDICACAO' | 'OUTRO';
export type Modalidade = 'REMOTO' | 'HIBRIDO' | 'PRESENCIAL';
export type TipoContrato = 'CLT' | 'PJ' | 'OUTRO';
export type Prioridade = 'ALTA' | 'MEDIA' | 'BAIXA';
export type Origem = 'MANUAL' | 'AGENTE' | 'IMPORTACAO';
export type TipoInteracao = 'EMAIL' | 'LIGACAO' | 'ENTREVISTA' | 'TESTE' | 'MENSAGEM' | 'MUDANCA_STATUS' | 'OUTRO';
export type CargoContato = 'RECRUTADOR' | 'TECH_LEAD' | 'GESTOR' | 'OUTRO';

export interface CandidaturaResumo {
  id: number;
  empresa: string;
  cargo: string;
  linkVaga: string | null;
  plataforma: Plataforma | null;
  modalidade: Modalidade | null;
  tipoContrato: TipoContrato | null;
  faixaSalarial: string | null;
  pretensaoSalarial: string | null;
  nota: number | null;
  stack: string[];
  dataCandidatura: string | null;
  status: Status;
  prioridade: Prioridade;
  origem: Origem;
  chaveExterna: string | null;
  ultimaInteracaoEm: string | null;
  atualizadoEm: string;
}

export interface CandidaturaDetalhe extends CandidaturaResumo {
  curriculoVersao: string | null;
  observacoes: string | null;
  criadoEm: string;
  contatos: Contato[];
  interacoes: Interacao[];
  historico: Historico[];
}

export interface CandidaturaRequest {
  empresa: string;
  cargo: string;
  linkVaga?: string | null;
  plataforma?: Plataforma | null;
  modalidade?: Modalidade | null;
  tipoContrato?: TipoContrato | null;
  faixaSalarial?: string | null;
  pretensaoSalarial?: string | null;
  nota?: number | null;
  stack?: string[];
  dataCandidatura?: string | null;
  status?: Status | null;
  prioridade?: Prioridade | null;
  curriculoVersao?: string | null;
  observacoes?: string | null;
  chaveExterna?: string | null;
  contatoIds?: number[] | null;
}

export interface Interacao {
  id: number;
  data: string;
  tipo: TipoInteracao;
  descricao: string | null;
  proximoPasso: string | null;
  dataFollowUp: string | null;
  followUpConcluido: boolean;
}

export interface InteracaoRequest {
  data?: string | null;
  tipo: TipoInteracao;
  descricao?: string | null;
  proximoPasso?: string | null;
  dataFollowUp?: string | null;
  followUpConcluido?: boolean | null;
}

export interface Historico {
  id: number;
  statusAnterior: Status | null;
  statusNovo: Status;
  data: string;
}

export interface Contato {
  id: number;
  nome: string;
  cargo: CargoContato;
  email: string | null;
  linkedin: string | null;
  telefone: string | null;
  empresa: string | null;
}

export type ContatoRequest = Omit<Contato, 'id'>;

export interface Pagina<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ColunaKanban {
  status: Status;
  candidaturas: CandidaturaResumo[];
}

export interface FiltrosCandidatura {
  status?: Status[];
  empresa?: string;
  plataforma?: Plataforma[];
  stack?: string;
  prioridade?: Prioridade[];
  de?: string;
  ate?: string;
  q?: string;
}

export interface ErroApi {
  status: number;
  erro: string;
  mensagem: string;
  caminho: string;
  campos: { campo: string; mensagem: string }[];
}

export interface EtapaFunil {
  etapa: Status;
  quantidade: number;
  conversaoDaAnterior: number;
  percentualDoTotal: number;
}

export interface Contagem {
  chave: string;
  quantidade: number;
}

export interface ContagemSemana {
  inicioSemana: string;
  quantidade: number;
}

export interface Dashboard {
  total: number;
  ativas: number;
  comResposta: number;
  taxaResposta: number;
  funil: EtapaFunil[];
  porStatus: Contagem[];
  porPlataforma: Contagem[];
  porSemana: ContagemSemana[];
}

export interface FollowUpItem {
  interacaoId: number;
  candidaturaId: number;
  empresa: string;
  cargo: string;
  status: Status;
  tipo: TipoInteracao;
  descricao: string | null;
  proximoPasso: string | null;
  dataFollowUp: string;
  diasParaVencer: number;
}

export interface FollowUps {
  vencidos: FollowUpItem[];
  proximos: FollowUpItem[];
}

export interface ResultadoSemRetorno {
  dias: number;
  marcadas: number;
  ids: number[];
  executadoEm: string;
}

export interface ResultadoImportacao {
  criadas: number;
  atualizadas: number;
  ignoradas: number;
  erros: string[];
}

export interface ResultadoInbox {
  arquivos: { arquivo: string; destino: string; resultado: ResultadoImportacao }[];
}
