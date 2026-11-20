package br.com.fiap.plantao.exception;

// erro de regra ou de dado invalido: vira HTTP 400 com a mensagem para o usuario
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
