package br.com.plantaoclima.model;

/**
 * Entidade que representa o voluntario de confianca (vizinho, familiar, sindico, agente de saude).
 */
public class Voluntario extends Pessoa {
    private String relacaoComunidade;
    private String disponibilidade;
    private Long idRegiao;
    private String nomeRegiao;

    public Voluntario() {
        super();
    }

    @Override
    public String getTipoPessoa() {
        return "VOLUNTARIO";
    }

    public String getRelacaoComunidade() {
        return relacaoComunidade;
    }

    public void setRelacaoComunidade(String relacaoComunidade) {
        this.relacaoComunidade = relacaoComunidade;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }

    public void setDisponibilidade(String disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    public Long getIdRegiao() {
        return idRegiao;
    }

    public void setIdRegiao(Long idRegiao) {
        this.idRegiao = idRegiao;
    }

    public String getNomeRegiao() {
        return nomeRegiao;
    }

    public void setNomeRegiao(String nomeRegiao) {
        this.nomeRegiao = nomeRegiao;
    }
}
