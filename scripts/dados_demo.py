"""Cria uma conta de demonstração com candidaturas fictícias, para testar e tirar prints das telas.

Uso (com a API em perfil dev, de preferência num banco H2 separado):
    python scripts/dados_demo.py                      # API em http://localhost:8081
    python scripts/dados_demo.py http://localhost:9090

Conta criada: demo@canditrack.test / demo-canditrack
As empresas são inventadas. Não rode contra o banco com os seus dados reais.
"""
import json
import random
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

API = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081").rstrip("/")
CONTA = {"nome": "Admin Demo", "email": "demo@canditrack.test", "senha": "demo-canditrack"}

# Ordem do processo; cada candidatura percorre as etapas até a etapa final dela.
FLUXO = ["CANDIDATADO", "TRIAGEM_RH", "TESTE_TECNICO", "ENTREVISTA_TECNICA", "ENTREVISTA_FINAL", "PROPOSTA", "ACEITA"]

# empresa, cargo, plataforma, etapa final, encerramento (None, RECUSADA ou SEM_RETORNO), dias atrás
VAGAS = [
    ("Brisa Pagamentos", "Desenvolvedor Java Pleno", "GUPY", "PROPOSTA", None, 58),
    ("Lumen Saúde", "Engenheiro de Software Backend", "LINKEDIN_VAGAS", "ENTREVISTA_FINAL", None, 41),
    ("Orbita Logística", "Desenvolvedor Full Stack Java/Angular", "PROGRAMATHOR", "ENTREVISTA_TECNICA", None, 30),
    ("Cajuína Tech", "Desenvolvedor Backend Java", "GUPY", "ENTREVISTA_TECNICA", "RECUSADA", 64),
    ("Tarrafa Seguros", "Desenvolvedor Java Sênior", "LINKEDIN_VAGAS", "TESTE_TECNICO", None, 19),
    ("Pinhão Educação", "Desenvolvedor Spring Boot", "REMOTAR", "TESTE_TECNICO", None, 12),
    ("Mandacaru Energia", "Engenheiro de Software Java", "COODESH", "TESTE_TECNICO", "RECUSADA", 47),
    ("Vereda Bank", "Desenvolvedor Backend Pleno", "LINKEDIN_FEED", "TRIAGEM_RH", None, 9),
    ("Nuvem Rasa", "Desenvolvedor Full Stack", "GEEKHUNTER", "TRIAGEM_RH", None, 15),
    ("Aroeira Varejo", "Desenvolvedor Java", "GUPY", "TRIAGEM_RH", "SEM_RETORNO", 70),
    ("Jangada Mobilidade", "Desenvolvedor Angular/Java", "INDEED", "TRIAGEM_RH", "RECUSADA", 52),
    ("Bromélia RH", "Desenvolvedor Java Pleno", "INFOJOBS", "CANDIDATADO", None, 3),
    ("Sertão Dados", "Engenheiro Backend", "LINKEDIN_VAGAS", "CANDIDATADO", None, 5),
    ("Coqueiral Turismo", "Desenvolvedor Spring", "GUPY", "CANDIDATADO", None, 6),
    ("Ipê Fintech", "Desenvolvedor Java Sênior", "LINKEDIN_VAGAS", "CANDIDATADO", None, 8),
    ("Maré Alta Games", "Desenvolvedor Backend", "PROGRAMATHOR", "CANDIDATADO", None, 11),
    ("Caatinga Agro", "Desenvolvedor Java", "CATHO", "CANDIDATADO", "SEM_RETORNO", 44),
    ("Farol Jurídico", "Desenvolvedor Full Stack Pleno", "GUPY", "CANDIDATADO", "SEM_RETORNO", 49),
    ("Rede Cacau", "Desenvolvedor Java/Kotlin", "REMOTAR", "CANDIDATADO", "SEM_RETORNO", 66),
    ("Trilha Cloud", "Engenheiro de Plataforma Java", "LINKEDIN_FEED", "CANDIDATADO", "RECUSADA", 35),
    ("Quintal Imóveis", "Desenvolvedor Backend Java", "INDEED", "CANDIDATADO", None, 22),
    ("Siriema Saúde", "Desenvolvedor Java Pleno", "GUPY", "CANDIDATADO", None, 26),
    ("Pé de Serra Labs", "Desenvolvedor Java", "GITHUB", "INTERESSE", None, 1),
    ("Algaroba Analytics", "Engenheiro de Dados (Java)", "LINKEDIN_VAGAS", "INTERESSE", None, 2),
]

PENDENCIAS = {
    "Tarrafa Seguros": "Entregar o teste técnico (API de apólices) até sexta",
    "Pinhão Educação": "Gravar o vídeo de apresentação pedido no teste",
    "Lumen Saúde": "Enviar referências para o RH",
    "Pé de Serra Labs": "Adaptar o currículo para a vaga",
}
STACKS = [["Java", "Spring Boot", "PostgreSQL"], ["Java", "Angular", "Docker"], ["Java", "Kafka", "AWS"],
          ["Java", "Spring Boot", "Microsserviços"], ["Kotlin", "Java", "Kubernetes"]]


def chamar(metodo, caminho, corpo=None, token=None):
    req = urllib.request.Request(API + caminho, method=metodo,
                                 data=json.dumps(corpo).encode() if corpo is not None else None,
                                 headers={"Content-Type": "application/json"})
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode()
            return json.loads(texto) if texto else None
    except urllib.error.HTTPError as e:
        raise SystemExit(f"{metodo} {caminho} -> {e.code}: {e.read().decode()}")


def entrar():
    try:
        return chamar("POST", "/api/auth/login", {"email": CONTA["email"], "senha": CONTA["senha"]})["token"]
    except SystemExit:
        return chamar("POST", "/api/auth/cadastro", CONTA)["token"]


def main():
    random.seed(7)
    token = entrar()
    if chamar("GET", "/api/candidaturas?size=1", token=token)["totalElements"]:
        raise SystemExit("A conta demo já tem candidaturas; apague o banco de demonstração para recriar.")

    hoje = date.today()
    for i, (empresa, cargo, plataforma, etapa, fim, dias) in enumerate(VAGAS):
        enviada = hoje - timedelta(days=dias)
        c = chamar("POST", "/api/candidaturas", {
            "empresa": empresa, "cargo": cargo, "plataforma": plataforma,
            "linkVaga": f"https://vagas.exemplo.test/{i + 1}",
            "modalidade": random.choice(["REMOTO", "REMOTO", "HIBRIDO"]),
            "tipoContrato": random.choice(["CLT", "PJ"]),
            "faixaSalarial": random.choice(["R$ 9.000 a 12.000", "R$ 12.000 a 15.000", None]),
            "nota": random.randint(6, 10), "stack": STACKS[i % len(STACKS)],
            "dataCandidatura": enviada.isoformat(),
            "status": "INTERESSE" if etapa == "INTERESSE" else "CANDIDATADO",
            "prioridade": random.choice(["ALTA", "MEDIA", "MEDIA", "BAIXA"]),
            "pendencia": PENDENCIAS.get(empresa),
        }, token)

        # avança pelas etapas, registrando uma interação em cada uma
        if etapa != "INTERESSE":
            alvo = FLUXO.index(etapa)
            for passo, status in enumerate(FLUXO[1:alvo + 1], start=1):
                chamar("PATCH", f"/api/candidaturas/{c['id']}/status", {"status": status}, token)
                quando = datetime.combine(enviada + timedelta(days=4 * passo), datetime.min.time()) + timedelta(hours=10)
                chamar("POST", f"/api/candidaturas/{c['id']}/interacoes", {
                    "data": min(quando, datetime.now()).isoformat(timespec="seconds"),
                    "tipo": {"TESTE_TECNICO": "TESTE"}.get(status, "ENTREVISTA" if "ENTREVISTA" in status else "EMAIL"),
                    "descricao": f"Avançou para {status.replace('_', ' ').lower()}",
                }, token)
            if fim:
                chamar("PATCH", f"/api/candidaturas/{c['id']}/status", {"status": fim}, token)

        # follow-ups: alguns vencidos, outros para os próximos dias
        if not fim and etapa not in ("INTERESSE", "ACEITA") and i % 3 != 2:
            chamar("POST", f"/api/candidaturas/{c['id']}/interacoes", {
                "tipo": "EMAIL", "descricao": "Mensagem para o recrutador",
                "proximoPasso": "Cobrar retorno do processo",
                "dataFollowUp": (hoje + timedelta(days=random.choice([-3, -1, 0, 2, 5]))).isoformat(),
            }, token)
        print(f"{empresa}: {fim or etapa}")

    print(f"\n{len(VAGAS)} candidaturas criadas. Entre com {CONTA['email']} / {CONTA['senha']}")


if __name__ == "__main__":
    main()
