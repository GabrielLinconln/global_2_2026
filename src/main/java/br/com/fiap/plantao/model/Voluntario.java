package br.com.fiap.plantao.model;

import br.com.fiap.plantao.exception.RegraNegocioException;

import java.util.Arrays;
import java.util.List;

public class Voluntario extends Pessoa {
    private static final List<String> TIPOS = Arrays.asList("VIZINHO", "FAMILIAR", "SINDICO", "IGREJA", "ACS", "OUTRO");

    private long idRegiao;
    private String tipo;

    @Override
    public void validar() {
        validarNome();
        if (!telefoneValido(getTelefone())) {
            throw new RegraNegocioException("Telefone do volunt\u00e1rio \u00e9 obrigat\u00f3rio: DDD + n\u00famero, s\u00f3 d\u00edgitos.");
        }
        if (!TIPOS.contains(tipo)) {
            throw new RegraNegocioException("Tipo de volunt\u00e1rio inv\u00e1lido.");
        }
    }

    public long getIdRegiao() { return idRegiao; }
    public void setIdRegiao(long idRegiao) { this.idRegiao = idRegiao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
