package br.com.fiap.plantao.model;

// quem pode ser classificado por risco diante de um alerta de calor ou chuva
public interface AvaliavelRisco {
    Risco calcularRisco(TipoAlerta tipo);
}
