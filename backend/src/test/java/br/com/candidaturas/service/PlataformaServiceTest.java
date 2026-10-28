package br.com.candidaturas.service;

import br.com.candidaturas.domain.Plataforma;
import br.com.candidaturas.dto.PlataformaDtos.Request;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.PlataformaRepository;
import br.com.candidaturas.support.UsuarioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlataformaServiceTest {
    private final PlataformaRepository repository = mock(PlataformaRepository.class);
    private final CandidaturaRepository candidaturas = mock(CandidaturaRepository.class);
    private final PlataformaService service = new PlataformaService(repository, candidaturas, new UsuarioFixo());
    private Plataforma plataforma;

    @BeforeEach void preparar() {
        plataforma = new Plataforma();
        plataforma.setId(10L); plataforma.setUsuarioId(UsuarioFixo.ID);
        plataforma.setCodigo("PORTAL"); plataforma.setNome("Portal São João");
    }
    @Test void resolveNomePersonalizadoSemAcentosECodigoEstavel() {
        when(repository.findByUsuarioIdOrderByOrdemAscIdAsc(UsuarioFixo.ID)).thenReturn(List.of(plataforma));
        assertThat(service.resolver("portal sao joao")).isEqualTo("PORTAL");
        assertThat(service.resolver("portal")).isEqualTo("PORTAL");
        assertThat(service.resolver("origem desconhecida")).isEqualTo("OUTRO");
    }
    @Test void desativadaPermiteSomenteManterAssociacaoExistente() {
        plataforma.setAtiva(false);
        when(repository.findByUsuarioIdAndCodigo(UsuarioFixo.ID,"PORTAL")).thenReturn(Optional.of(plataforma));
        assertThat(service.validar("PORTAL","PORTAL")).isEqualTo("PORTAL");
        assertThatThrownBy(() -> service.validar("PORTAL",null)).isInstanceOf(RegraNegocioException.class);
    }
    @Test void impedeExclusaoQuandoHaCandidaturas() {
        when(repository.findByIdAndUsuarioId(10L,UsuarioFixo.ID)).thenReturn(Optional.of(plataforma));
        when(repository.findByUsuarioIdAndCodigo(UsuarioFixo.ID,"PORTAL")).thenReturn(Optional.of(plataforma));
        when(candidaturas.existsByUsuarioIdAndPlataforma(UsuarioFixo.ID,"PORTAL")).thenReturn(true);
        assertThatThrownBy(() -> service.excluir(10L)).isInstanceOf(RegraNegocioException.class).hasMessageContaining("Desative");
        verify(repository,never()).delete(any());
    }
    @Test void rejeitaNomeDuplicadoMesmoComAcentosDiferentes() {
        when(repository.existsByUsuarioIdAndNomeChaveAndIdNot(UsuarioFixo.ID,"PORTALSAOJOAO",-1L)).thenReturn(true);
        assertThatThrownBy(() -> service.criar(new Request("Portal São João",0,true)))
            .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
    }
    @Test void inicializaCatalogoIncluindoSolidesParaNovaConta() {
        service.inicializar(25L);
        var captor = org.mockito.ArgumentCaptor.forClass(Plataforma.class);
        verify(repository,times(13)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(p -> p.getUsuarioId().equals(25L));
        assertThat(captor.getAllValues()).extracting(Plataforma::getCodigo).contains("SOLIDES","OUTRO");
    }
}
