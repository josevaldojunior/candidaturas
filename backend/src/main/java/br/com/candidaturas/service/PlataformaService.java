package br.com.candidaturas.service;
import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.PlataformaDtos;
import br.com.candidaturas.dto.PlataformaDtos.*;
import br.com.candidaturas.repository.*;
import br.com.candidaturas.seguranca.UsuarioAtual;
import br.com.candidaturas.util.Normalizacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PlataformaService {
    private final PlataformaRepository repository;
    private final CandidaturaRepository candidaturas;
    private final UsuarioAtual atual;
    public PlataformaService(PlataformaRepository repository, CandidaturaRepository candidaturas, UsuarioAtual atual) {
        this.repository=repository; this.candidaturas=candidaturas; this.atual=atual;
    }
    @Transactional(readOnly=true)
    public List<Response> listar() {
        return repository.findByUsuarioIdOrderByOrdemAscIdAsc(atual.id()).stream().map(PlataformaDtos::response).toList();
    }
    @Transactional(readOnly=true)
    public Response buscar(Long id) { return PlataformaDtos.response(obter(id)); }
    @Transactional
    public Response criar(Request r) {
        String base=Normalizacao.chave(r.nome());
        if(base.isBlank()) throw new RegraNegocioException("Informe um nome com letras ou números");
        base=base.substring(0,Math.min(base.length(),32));
        if(base.equals("NAOINFORMADA")) throw new RegraNegocioException("Nome reservado");
        String codigo=base;
        for(int n=2;repository.existsByUsuarioIdAndCodigo(atual.id(),codigo);n++) codigo=base+"_"+n;
        Plataforma p=new Plataforma(); p.setUsuarioId(atual.id()); p.setCodigo(codigo);
        preencher(p,r); return PlataformaDtos.response(repository.saveAndFlush(p));
    }
    @Transactional
    public Response atualizar(Long id, Request r) {
        Plataforma p=obter(id);
        p=repository.findByUsuarioIdAndCodigo(atual.id(),p.getCodigo()).orElseThrow();
        if(p.getCodigo().equals("OUTRO") && !r.ativa()) throw new RegraNegocioException("Outros deve permanecer ativa para a integração");
        preencher(p,r); return PlataformaDtos.response(repository.saveAndFlush(p));
    }
    @Transactional
    public void excluir(Long id) {
        Plataforma p=obter(id);
        p=repository.findByUsuarioIdAndCodigo(atual.id(),p.getCodigo()).orElseThrow();
        if(p.getCodigo().equals("OUTRO")) throw new RegraNegocioException("A plataforma Outros é necessária para a integração");
        if(candidaturas.existsByUsuarioIdAndPlataforma(atual.id(),p.getCodigo()))
            throw new RegraNegocioException("Esta plataforma tem candidaturas. Desative-a para preservar o histórico.");
        repository.delete(p); repository.flush();
    }
    private Plataforma obter(Long id) {
        return repository.findByIdAndUsuarioId(id,atual.id()).orElseThrow(()->new RecursoNaoEncontradoException("Plataforma",id));
    }
    private void preencher(Plataforma p, Request r) {
        String chave=Normalizacao.chave(r.nome());
        if(chave.isBlank()) throw new RegraNegocioException("Informe um nome com letras ou números");
        if(repository.existsByUsuarioIdAndNomeChaveAndIdNot(atual.id(),chave,p.getId()==null?-1L:p.getId()))
            throw new RegraNegocioException("Já existe uma plataforma com esse nome");
        p.setNome(r.nome().strip()); p.setNomeChave(chave); p.setOrdem(r.ordem()); p.setAtiva(r.ativa());
    }
    /** Valida o código sem impedir a edição de candidaturas que já usam uma plataforma desativada. */
    @Transactional
    public String validar(String codigo, String anterior) {
        if(codigo==null || codigo.isBlank()) return null;
        Plataforma p=repository.findByUsuarioIdAndCodigo(atual.id(),codigo)
            .orElseThrow(()->new RegraNegocioException("Plataforma não encontrada: "+codigo));
        if(!p.isAtiva() && !codigo.equals(anterior)) throw new RegraNegocioException("Plataforma desativada: "+p.getNome());
        return p.getCodigo();
    }
    /** Nomes/códigos cadastrados têm precedência; mantém os apelidos históricos do agente. */
    @Transactional
    public String resolver(String texto) {
        if(Normalizacao.vazio(texto)) return null;
        String chave=Normalizacao.chave(texto);
        var lista=listar();
        String codigo=lista.stream().filter(p->Normalizacao.chave(p.codigo()).equals(chave)
            || Normalizacao.chave(p.nome()).equals(chave)).map(Response::codigo).findFirst()
            .orElseGet(()->Normalizacao.plataforma(texto));
        return lista.stream().anyMatch(p->p.codigo().equals(codigo))?codigo:"OUTRO";
    }
    @Transactional
    public void inicializar(Long usuarioId) {
        String[] nomes={"Gupy","LinkedIn Vagas","LinkedIn Feed","Indeed","Programathor","Remotar","InfoJobs","Coodesh","GeekHunter","Catho","Repositórios GitHub","Sólides","Outros"};
        int ordem=0;
        for(PlataformaPadrao padrao:PlataformaPadrao.values()) {
            Plataforma p=new Plataforma(); p.setUsuarioId(usuarioId); p.setCodigo(padrao.name());
            p.setNome(nomes[ordem]); p.setNomeChave(Normalizacao.chave(p.getNome())); p.setOrdem(ordem++);
            repository.save(p);
        }
    }
}
