package br.com.plantaoclima.model;

/**
 * Representa uma regiao/territorio comunitario monitorado.
 */
public class Regiao {
    private Long id;
    private String nome;
    private String bairroZona;
    private String riscoCalor;
    private String riscoChuva;
    private String referenciaUbs;
    private boolean ativo = true;

    public Regiao() {
    }

    public Regiao(Long id, String nome, String bairroZona, String riscoCalor,
                  String riscoChuva, String referenciaUbs, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.bairroZona = bairroZona;
        this.riscoCalor = riscoCalor;
        this.riscoChuva = riscoChuva;
        this.referenciaUbs = referenciaUbs;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getBairroZona() {
        return bairroZona;
    }

    public void setBairroZona(String bairroZona) {
        this.bairroZona = bairroZona;
    }

    public String getRiscoCalor() {
        return riscoCalor;
    }

    public void setRiscoCalor(String riscoCalor) {
        this.riscoCalor = riscoCalor;
    }

    public String getRiscoChuva() {
        return riscoChuva;
    }

    public void setRiscoChuva(String riscoChuva) {
        this.riscoChuva = riscoChuva;
    }

    public String getReferenciaUbs() {
        return referenciaUbs;
    }

    public void setReferenciaUbs(String referenciaUbs) {
        this.referenciaUbs = referenciaUbs;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
