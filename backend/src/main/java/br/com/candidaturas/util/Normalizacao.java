package br.com.candidaturas.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Normalização de textos vindos do agente/CSV: enums, datas, números, stack. */
public final class Normalizacao {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    /** Apelidos comuns (já normalizados) → nome do enum. */
    private static final Map<String, String> APELIDOS = Map.ofEntries(
            Map.entry("REMOTE", "REMOTO"),
            Map.entry("HOMEOFFICE", "REMOTO"),
            Map.entry("HYBRID", "HIBRIDO"),
            Map.entry("ONSITE", "PRESENCIAL"),
            Map.entry("REFERRAL", "INDICACAO"),
            Map.entry("ALTO", "ALTA"),
            Map.entry("MEDIO", "MEDIA"),
            Map.entry("BAIXO", "BAIXA"),
            Map.entry("TELEFONE", "LIGACAO"),
            Map.entry("WHATSAPP", "MENSAGEM"),
            Map.entry("RH", "TRIAGEM_RH"),
            Map.entry("TRIAGEM", "TRIAGEM_RH"),
            Map.entry("TESTE", "TESTE_TECNICO"),
            Map.entry("CANDIDATURA", "CANDIDATADO"),
            Map.entry("APLICADO", "CANDIDATADO"));

    private Normalizacao() {
    }

    /** Remove acentos e tudo que não for letra ou dígito, em caixa alta: "Triagem RH" → "TRIAGEMRH". */
    public static String chave(String valor) {
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Converte texto livre para o enum. Retorna null se o texto for vazio.
     *
     * @throws IllegalArgumentException se o valor não corresponder a nenhuma constante
     */
    public static <E extends Enum<E>> E paraEnum(Class<E> tipo, String valor, String campo) {
        if (vazio(valor)) {
            return null;
        }
        String chave = chave(valor);
        for (E constante : tipo.getEnumConstants()) {
            if (chave(constante.name()).equals(chave)) {
                return constante;
            }
        }
        String apelido = APELIDOS.get(chave);
        if (apelido != null) {
            for (E constante : tipo.getEnumConstants()) {
                if (constante.name().equals(apelido)) {
                    return constante;
                }
            }
        }
        throw new IllegalArgumentException("valor inválido para " + campo + ": '" + valor + "'");
    }

    /** Igual a {@link #paraEnum}, mas devolve {@code padrao} quando o valor não é reconhecido. */
    public static <E extends Enum<E>> E paraEnumOu(Class<E> tipo, String valor, String campo, E padrao) {
        try {
            return paraEnum(tipo, valor, campo);
        } catch (IllegalArgumentException e) {
            return padrao;
        }
    }

    /** Aceita yyyy-MM-dd ou dd/MM/yyyy. */
    public static LocalDate data(String valor, String campo) {
        if (vazio(valor)) {
            return null;
        }
        String v = valor.trim();
        try {
            return v.contains("/") ? LocalDate.parse(v, BR) : LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(campo + " inválida: '" + valor + "' (use yyyy-MM-dd ou dd/MM/yyyy)");
        }
    }

    public static Integer nota(String valor) {
        if (vazio(valor)) {
            return null;
        }
        int n;
        try {
            n = (int) Math.round(Double.parseDouble(valor.trim().replace(',', '.')));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("nota inválida: '" + valor + "'");
        }
        if (n < 1 || n > 10) {
            throw new IllegalArgumentException("nota deve estar entre 1 e 10: '" + valor + "'");
        }
        return n;
    }

    /** Junta tags, separando também por ";" ou "," dentro de cada item, sem duplicar (ignorando caixa). */
    public static Set<String> stack(List<String> itens) {
        Set<String> resultado = new LinkedHashSet<>();
        if (itens == null) {
            return resultado;
        }
        Set<String> vistos = new java.util.HashSet<>();
        for (String item : itens) {
            if (item == null) {
                continue;
            }
            for (String tag : item.split("[;,]")) {
                String t = tag.trim();
                if (!t.isEmpty() && t.length() <= 60 && vistos.add(t.toLowerCase(Locale.ROOT))) {
                    resultado.add(t);
                }
            }
        }
        return resultado;
    }

    public static String texto(String valor) {
        return vazio(valor) ? null : valor.trim();
    }

    public static String digitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
