package br.com.plantaoclima.exception;

/**
 * Excecao lancada quando dados de entrada ou regras de negocio sao invalidos.
 */
public class ValidacaoException extends RuntimeException {
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}
