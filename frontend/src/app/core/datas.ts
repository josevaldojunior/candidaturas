/** Date → 'yyyy-MM-dd' no fuso local (o backend trabalha com LocalDate). */
export function paraIsoData(d: Date | null | undefined): string | null {
  if (!d) {
    return null;
  }
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

/** 'yyyy-MM-dd' → Date local (evita o deslocamento de fuso do construtor com string). */
export function deIsoData(s: string | null | undefined): Date | null {
  if (!s) {
    return null;
  }
  const [a, m, d] = s.substring(0, 10).split('-').map(Number);
  return new Date(a, m - 1, d);
}
