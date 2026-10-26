package br.com.candidaturas.support;

import br.com.candidaturas.seguranca.UsuarioAtual;

/** Usuário atual fixo para testes unitários (sem Spring Security). */
public class UsuarioFixo extends UsuarioAtual {

    public static final Long ID = 1L;

    @Override
    public Long id() {
        return ID;
    }
}
