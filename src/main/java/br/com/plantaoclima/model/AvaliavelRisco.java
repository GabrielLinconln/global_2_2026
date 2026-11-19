package br.com.plantaoclima.model;

/**
 * Contrato de avaliacao de risco climatico para entidades protegidas.
 */
public interface AvaliavelRisco {
    int calcularPontuacaoRisco(TipoAlerta tipoAlerta);

    String classificarNivelRisco(TipoAlerta tipoAlerta);
}
