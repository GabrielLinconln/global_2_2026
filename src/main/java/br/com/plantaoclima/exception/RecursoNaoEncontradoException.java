package br.com.plantaoclima.exception;

/**
 * Excecao lancada quando um registro solicitado nao e encontrado no banco de dados.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
