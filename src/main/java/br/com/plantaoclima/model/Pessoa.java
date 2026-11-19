package br.com.plantaoclima.model;

import java.time.LocalDateTime;

/**
 * Superclasse abstrata para pessoas cadastradas na rede comunitaria (Idoso e Voluntario).
 */
public abstract class Pessoa {
    private Long id;
    private String nome;
    private String telefone;
    private boolean ativo = true;
    private LocalDateTime criadoEm;

    protected Pessoa() {
    }

    protected Pessoa(Long id, String nome, String telefone, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.telefone = telefone;
        this.ativo = ativo;
    }

    public abstract String getTipoPessoa();

    public String getResumoContato() {
        return nome + " (" + telefone + ")";
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
