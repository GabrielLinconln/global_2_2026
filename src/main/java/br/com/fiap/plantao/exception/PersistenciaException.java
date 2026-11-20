package br.com.fiap.plantao.exception;

// falha de banco (conexao, SQL): vira HTTP 500
public class PersistenciaException extends RuntimeException {
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
