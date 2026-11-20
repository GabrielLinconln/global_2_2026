package br.com.fiap.plantao.model;

public enum Resultado {
    PENDENTE, OK, ATENCAO, URGENTE, NAO_ATENDEU;

    // conseguiu falar com o idoso
    public boolean houveContato() {
        return this == OK || this == ATENCAO || this == URGENTE;
    }
}
