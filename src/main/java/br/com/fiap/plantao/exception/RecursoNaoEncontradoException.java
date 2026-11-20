package br.com.fiap.plantao.exception;

// registro ou rota inexistente: vira HTTP 404
public class RecursoNaoEncontradoException extends RegraNegocioException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
