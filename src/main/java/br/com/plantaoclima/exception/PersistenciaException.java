package br.com.plantaoclima.exception;

/**
 * Excecao lancada para falhas de persistencia ou violacoes de integridade no JDBC.
 */
public class PersistenciaException extends RuntimeException {
    private final boolean conflito;

    public PersistenciaException(String mensagem, Throwable causa) {
        this(mensagem, causa, false);
    }

    public PersistenciaException(String mensagem, Throwable causa, boolean conflito) {
        super(mensagem, causa);
        this.conflito = conflito;
    }

    public boolean isConflito() {
        return conflito;
    }
}
