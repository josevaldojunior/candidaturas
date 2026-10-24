import { Pipe, PipeTransform } from '@angular/core';
import {
  ROTULO_CARGO_CONTATO, ROTULO_MODALIDADE, ROTULO_ORIGEM, ROTULO_PLATAFORMA, ROTULO_PRIORIDADE, ROTULO_STATUS,
  ROTULO_TIPO_CONTRATO, ROTULO_TIPO_INTERACAO,
} from '../core/rotulos';

const TABELAS: Record<string, Record<string, string>> = {
  status: ROTULO_STATUS,
  plataforma: ROTULO_PLATAFORMA,
  modalidade: ROTULO_MODALIDADE,
  contrato: ROTULO_TIPO_CONTRATO,
  prioridade: ROTULO_PRIORIDADE,
  origem: ROTULO_ORIGEM,
  interacao: ROTULO_TIPO_INTERACAO,
  cargoContato: ROTULO_CARGO_CONTATO,
};

/** Converte o valor do enum no rótulo em português: {{ c.status | rotulo: 'status' }}. */
@Pipe({ name: 'rotulo', standalone: true })
export class RotuloPipe implements PipeTransform {
  transform(valor: string | null | undefined, tabela: keyof typeof TABELAS | string): string {
    if (!valor) {
      return '—';
    }
    return TABELAS[tabela]?.[valor] ?? valor;
  }
}
