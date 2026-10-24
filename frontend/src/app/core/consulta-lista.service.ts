import { Injectable, signal } from '@angular/core';
import { Params } from '@angular/router';

/**
 * Lembra a última consulta da lista de candidaturas (filtros, página e ordenação, no formato
 * dos query params da URL), para o detalhe e o formulário voltarem para a lista como ela estava.
 */
@Injectable({ providedIn: 'root' })
export class ConsultaListaService {
  readonly ultima = signal<Params>({});
}
