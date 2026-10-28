package br.com.candidaturas.support;
import br.com.candidaturas.service.PlataformaService;
import br.com.candidaturas.domain.PlataformaPadrao;
import br.com.candidaturas.dto.PlataformaDtos.Response;
import br.com.candidaturas.util.Normalizacao;
import java.util.Arrays;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
public final class PlataformasMock {
    public static PlataformaService criar() {
        var p=mock(PlataformaService.class);
        lenient().when(p.validar(any(),any())).thenAnswer(i->i.getArgument(0));
        lenient().when(p.resolver(any())).thenAnswer(i->Normalizacao.plataforma(i.getArgument(0)));
        lenient().when(p.listar()).thenReturn(Arrays.stream(PlataformaPadrao.values())
            .map(v->new Response((long)v.ordinal(),v.name(),v.name(),v.ordinal(),true)).toList());
        return p;
    }
}
