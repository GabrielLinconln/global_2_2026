package br.com.plantaoclima.model;

public enum PapelVinculo {
    TITULAR(1),
    RESERVA(2);

    private final int prioridadePadrao;

    PapelVinculo(int prioridadePadrao) {
        this.prioridadePadrao = prioridadePadrao;
    }

    public int getPrioridadePadrao() {
        return prioridadePadrao;
    }
}
