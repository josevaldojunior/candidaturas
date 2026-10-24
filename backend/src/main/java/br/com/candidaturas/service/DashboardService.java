package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.HistoricoStatus;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.dto.DashboardDtos.*;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.HistoricoStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Indicadores do painel.
 * <ul>
 *   <li>Candidatura "enviada": já passou de INTERESSE (inclui encerradas como RECUSADA/SEM_RETORNO).</li>
 *   <li>"Com resposta": enviada que chegou pelo menos a TRIAGEM_RH ou foi RECUSADA.</li>
 *   <li>Funil: para cada etapa, quantas candidaturas chegaram até ela (considerando o histórico).</li>
 * </ul>
 */
@Service
public class DashboardService {

    static final int SEMANAS = 12;

    private final CandidaturaRepository candidaturaRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final Clock clock;

    public DashboardService(CandidaturaRepository candidaturaRepository, HistoricoStatusRepository historicoRepository,
                            Clock clock) {
        this.candidaturaRepository = candidaturaRepository;
        this.historicoRepository = historicoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        List<Candidatura> todas = candidaturaRepository.findAll();
        Map<Long, List<Status>> historicoPorCandidatura = historicoRepository.findAll().stream()
                .collect(Collectors.groupingBy(h -> h.getCandidatura().getId(),
                        Collectors.mapping(HistoricoStatus::getStatusNovo, Collectors.toList())));

        Map<Long, Integer> etapaMaxima = new HashMap<>();
        for (Candidatura c : todas) {
            etapaMaxima.put(c.getId(), etapaMaxima(c, historicoPorCandidatura.getOrDefault(c.getId(), List.of())));
        }

        long total = todas.size();
        long enviadas = todas.stream().filter(c -> etapaMaxima.get(c.getId()) >= Status.CANDIDATADO.getEtapa()).count();
        long comResposta = todas.stream()
                .filter(c -> etapaMaxima.get(c.getId()) >= Status.TRIAGEM_RH.getEtapa() || c.getStatus() == Status.RECUSADA)
                .count();
        long ativas = todas.stream()
                .filter(c -> Status.AGUARDANDO_RETORNO.contains(c.getStatus()) || c.getStatus() == Status.PROPOSTA)
                .count();

        return new DashboardResponse(total, ativas, comResposta, percentual(comResposta, enviadas),
                funil(etapaMaxima.values(), total), porStatus(todas), porPlataforma(todas), porSemana(todas));
    }

    /** Maior etapa do funil alcançada. Status finais sem etapa contam como "enviada" (CANDIDATADO). */
    static int etapaMaxima(Candidatura c, List<Status> historico) {
        int max = -1;
        List<Status> todos = new ArrayList<>(historico);
        todos.add(c.getStatus());
        for (Status s : todos) {
            int etapa = s.isEtapaDoFunil() ? s.getEtapa() : Status.CANDIDATADO.getEtapa();
            max = Math.max(max, etapa);
        }
        return max;
    }

    private static List<EtapaFunil> funil(Collection<Integer> etapas, long total) {
        List<EtapaFunil> funil = new ArrayList<>();
        long anterior = total;
        for (Status etapa : Status.etapasDoFunil()) {
            long qtd = etapas.stream().filter(e -> e >= etapa.getEtapa()).count();
            funil.add(new EtapaFunil(etapa, qtd, percentual(qtd, anterior), percentual(qtd, total)));
            anterior = qtd;
        }
        return funil;
    }

    private static List<Contagem> porStatus(List<Candidatura> todas) {
        Map<Status, Long> contagem = todas.stream()
                .collect(Collectors.groupingBy(Candidatura::getStatus, () -> new EnumMap<>(Status.class), Collectors.counting()));
        return Arrays.stream(Status.values())
                .map(s -> new Contagem(s.name(), contagem.getOrDefault(s, 0L)))
                .toList();
    }

    private static List<Contagem> porPlataforma(List<Candidatura> todas) {
        return todas.stream()
                .collect(Collectors.groupingBy(c -> c.getPlataforma() == null ? "NAO_INFORMADA" : c.getPlataforma().name(),
                        TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Contagem(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Contagem::quantidade).reversed())
                .toList();
    }

    /** Últimas 12 semanas (segunda a domingo), contando candidaturas enviadas pela data da candidatura. */
    private List<ContagemSemana> porSemana(List<Candidatura> todas) {
        LocalDate inicioAtual = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primeira = inicioAtual.minusWeeks(SEMANAS - 1L);
        Map<LocalDate, Long> contagem = new TreeMap<>();
        for (int i = 0; i < SEMANAS; i++) {
            contagem.put(primeira.plusWeeks(i), 0L);
        }
        for (Candidatura c : todas) {
            if (c.getStatus() == Status.INTERESSE) {
                continue;
            }
            LocalDate data = c.getDataCandidatura() != null ? c.getDataCandidatura()
                    : c.getCriadoEm() != null ? c.getCriadoEm().toLocalDate() : null;
            if (data == null) {
                continue;
            }
            LocalDate semana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            contagem.computeIfPresent(semana, (k, v) -> v + 1);
        }
        return contagem.entrySet().stream().map(e -> new ContagemSemana(e.getKey(), e.getValue())).toList();
    }

    static double percentual(long parte, long todo) {
        if (todo == 0) {
            return 0.0;
        }
        return Math.round(parte * 1000.0 / todo) / 10.0;
    }
}
