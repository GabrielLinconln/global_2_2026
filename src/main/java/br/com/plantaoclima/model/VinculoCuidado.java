package br.com.plantaoclima.model;

import java.time.LocalDateTime;

/**
 * Entidade associativa (N:N) que vincula um Idoso a um Voluntario como TITULAR ou RESERVA.
 */
public class VinculoCuidado {
    private Long id;
    private Long idIdoso;
    private Long idVoluntario;
    private PapelVinculo papel;
    private int prioridade;
    private LocalDateTime criadoEm;

    // Campos de exibicao preenchidos via JOIN
    private String nomeIdoso;
    private String telefoneIdoso;
    private String nomeVoluntario;
    private String telefoneVoluntario;
    private String relacaoVoluntario;

    public VinculoCuidado() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdIdoso() {
        return idIdoso;
    }

    public void setIdIdoso(Long idIdoso) {
        this.idIdoso = idIdoso;
    }

    public Long getIdVoluntario() {
        return idVoluntario;
    }

    public void setIdVoluntario(Long idVoluntario) {
        this.idVoluntario = idVoluntario;
    }

    public PapelVinculo getPapel() {
        return papel;
    }

    public void setPapel(PapelVinculo papel) {
        this.papel = papel;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(int prioridade) {
        this.prioridade = prioridade;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getNomeIdoso() {
        return nomeIdoso;
    }

    public void setNomeIdoso(String nomeIdoso) {
        this.nomeIdoso = nomeIdoso;
    }

    public String getTelefoneIdoso() {
        return telefoneIdoso;
    }

    public void setTelefoneIdoso(String telefoneIdoso) {
        this.telefoneIdoso = telefoneIdoso;
    }

    public String getNomeVoluntario() {
        return nomeVoluntario;
    }

    public void setNomeVoluntario(String nomeVoluntario) {
        this.nomeVoluntario = nomeVoluntario;
    }

    public String getTelefoneVoluntario() {
        return telefoneVoluntario;
    }

    public void setTelefoneVoluntario(String telefoneVoluntario) {
        this.telefoneVoluntario = telefoneVoluntario;
    }

    public String getRelacaoVoluntario() {
        return relacaoVoluntario;
    }

    public void setRelacaoVoluntario(String relacaoVoluntario) {
        this.relacaoVoluntario = relacaoVoluntario;
    }
}
