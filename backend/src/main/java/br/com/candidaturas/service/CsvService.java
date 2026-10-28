package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Contato;
import br.com.candidaturas.domain.Interacao;
import br.com.candidaturas.domain.TipoInteracao;
import br.com.candidaturas.dto.IntegracaoItem;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Leitura e escrita do CSV de candidaturas. O mesmo cabeçalho serve para exportar,
 * importar pela tela e para os arquivos da pasta inbox.
 */
@Service
public class CsvService {

    public static final String[] CABECALHO = {
            "chave_externa", "empresa", "cargo", "link_vaga", "plataforma", "modalidade", "tipo_contrato",
            "faixa_salarial", "pretensao_salarial", "nota", "data_candidatura", "status", "prioridade",
            "curriculo_versao", "stack", "observacoes", "contato_nome", "contato_email", "contato_telefone",
            "tipo_interacao", "descricao_interacao", "proximo_passo", "data_follow_up"};

    private static final char BOM = '﻿';

    /** Lê o CSV (UTF-8, com ou sem BOM; separador "," ou ";" detectado pelo cabeçalho). */
    public List<IntegracaoItem> ler(InputStream entrada) {
        String conteudo;
        try {
            conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo", e);
        }
        if (!conteudo.isEmpty() && conteudo.charAt(0) == BOM) {
            conteudo = conteudo.substring(1);
        }
        if (conteudo.isBlank()) {
            throw new IllegalArgumentException("Arquivo CSV vazio");
        }
        String primeiraLinha = conteudo.lines().findFirst().orElse("");
        char separador = contar(primeiraLinha, ';') > contar(primeiraLinha, ',') ? ';' : ',';

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(separador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setAllowMissingColumnNames(true)
                .build();
        try (CSVParser parser = formato.parse(new StringReader(conteudo))) {
            Set<String> colunas = new HashSet<>();
            parser.getHeaderNames().forEach(h -> colunas.add(h.trim().toLowerCase(Locale.ROOT)));
            if (!colunas.contains("empresa") || !colunas.contains("cargo")) {
                throw new IllegalArgumentException("Cabeçalho inválido: as colunas 'empresa' e 'cargo' são obrigatórias. "
                        + "Esperado: " + String.join(",", CABECALHO));
            }
            List<IntegracaoItem> itens = new ArrayList<>();
            for (CSVRecord r : parser) {
                itens.add(item(r));
            }
            return itens;
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalArgumentException("CSV mal formatado: " + e.getMessage(), e);
        }
    }

    /** Escreve o CSV com BOM (para o Excel abrir os acentos) e o mesmo cabeçalho aceito na importação. */
    public void escrever(Writer saida, List<Candidatura> candidaturas) throws IOException {
        saida.write(BOM);
        CSVFormat formato = CSVFormat.DEFAULT.builder().setHeader(CABECALHO).setRecordSeparator("\r\n").build();
        try (CSVPrinter printer = new CSVPrinter(saida, formato)) {
            for (Candidatura c : candidaturas) {
                Contato contato = c.getContatos().stream().min(Comparator.comparing(Contato::getId)).orElse(null);
                Interacao ultima = c.getInteracoes().stream()
                        .filter(i -> i.getTipo() != TipoInteracao.MUDANCA_STATUS)
                        .max(Comparator.comparing(Interacao::getData))
                        .orElse(null);
                printer.printRecord(
                        c.getChaveExterna(), c.getEmpresa(), c.getCargo(), c.getLinkVaga(), c.getPlataforma(),
                        nome(c.getModalidade()), nome(c.getTipoContrato()), c.getFaixaSalarial(),
                        c.getPretensaoSalarial(), c.getNota(), c.getDataCandidatura(), nome(c.getStatus()),
                        nome(c.getPrioridade()), c.getCurriculoVersao(),
                        String.join(";", new TreeSet<>(c.getStack())), c.getObservacoes(),
                        contato == null ? null : contato.getNome(),
                        contato == null ? null : contato.getEmail(),
                        contato == null ? null : contato.getTelefone(),
                        ultima == null ? null : nome(ultima.getTipo()),
                        ultima == null ? null : ultima.getDescricao(),
                        ultima == null ? null : ultima.getProximoPasso(),
                        ultima == null ? null : ultima.getDataFollowUp());
            }
        }
    }

    private static IntegracaoItem item(CSVRecord r) {
        String stack = valor(r, "stack");
        return new IntegracaoItem(
                valor(r, "chave_externa"), valor(r, "empresa"), valor(r, "cargo"), valor(r, "link_vaga"),
                valor(r, "plataforma"), valor(r, "modalidade"), valor(r, "tipo_contrato"),
                valor(r, "faixa_salarial"), valor(r, "pretensao_salarial"), valor(r, "nota"),
                valor(r, "data_candidatura"), valor(r, "status"), valor(r, "prioridade"),
                valor(r, "curriculo_versao"), valor(r, "observacoes"),
                stack == null ? null : List.of(stack),
                valor(r, "contato_nome"), valor(r, "contato_email"), valor(r, "contato_telefone"),
                valor(r, "tipo_interacao"), valor(r, "descricao_interacao"), valor(r, "proximo_passo"),
                valor(r, "data_follow_up"));
    }

    private static String valor(CSVRecord r, String coluna) {
        if (!r.isMapped(coluna) || !r.isSet(coluna)) {
            return null;
        }
        String v = r.get(coluna);
        return v == null || v.isBlank() ? null : v;
    }

    private static String nome(Enum<?> e) {
        return e == null ? null : e.name();
    }

    private static long contar(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
