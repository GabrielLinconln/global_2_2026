package br.com.plantaoclima.model;

import java.time.LocalDateTime;

/**
 * Registra cada etapa da rodada de ligacoes de um Alerta para um Idoso.
 * Permite escalonamento automatico: TITULAR -> RESERVA -> COORDENACAO (CRITICO).
 */
public class Checagem {
    private Long id;
    private Long idAlerta;
    private Long idIdoso;
    private Long idVoluntario;
    private PapelChecagem papelAtual;
    private int pontuacaoRisco;
    private StatusChecagem status = StatusChecagem.PENDENTE;
    private int tentativas;
    private String checklistResumo;
    private String observacao;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // Campos de exibicao preenchidos via JOIN
    private String tituloAlerta;
    private TipoAlerta tipoAlerta;
    private String nomeIdoso;
    private String telefoneIdoso;
    private int anoNascimentoIdoso;
    private String enderecoIdoso;
    private String nomeRegiao;
    private String nomeVoluntario;
    private String telefoneVoluntario;
    private boolean doencaCronica;
    private boolean mobilidadeReduzida;
    private boolean casaSemVentilacao;
    private boolean areaRisco;

    public Checagem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(Long idAlerta) {
        this.idAlerta = idAlerta;
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

    public PapelChecagem getPapelAtual() {
        return papelAtual;
    }

    public void setPapelAtual(PapelChecagem papelAtual) {
        this.papelAtual = papelAtual;
    }

    public int getPontuacaoRisco() {
        return pontuacaoRisco;
    }

    public void setPontuacaoRisco(int pontuacaoRisco) {
        this.pontuacaoRisco = pontuacaoRisco;
    }

    public StatusChecagem getStatus() {
        return status;
    }

    public void setStatus(StatusChecagem status) {
        this.status = status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int tentativas) {
        this.tentativas = tentativas;
    }

    public String getChecklistResumo() {
        return checklistResumo;
    }

    public void setChecklistResumo(String checklistResumo) {
        this.checklistResumo = checklistResumo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String getTituloAlerta() {
        return tituloAlerta;
    }

    public void setTituloAlerta(String tituloAlerta) {
        this.tituloAlerta = tituloAlerta;
    }

    public TipoAlerta getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(TipoAlerta tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
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

    public int getAnoNascimentoIdoso() {
        return anoNascimentoIdoso;
    }

    public void setAnoNascimentoIdoso(int anoNascimentoIdoso) {
        this.anoNascimentoIdoso = anoNascimentoIdoso;
    }

    public String getEnderecoIdoso() {
        return enderecoIdoso;
    }

    public void setEnderecoIdoso(String enderecoIdoso) {
        this.enderecoIdoso = enderecoIdoso;
    }

    public String getNomeRegiao() {
        return nomeRegiao;
    }

    public void setNomeRegiao(String nomeRegiao) {
        this.nomeRegiao = nomeRegiao;
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

    public boolean isDoencaCronica() {
        return doencaCronica;
    }

    public void setDoencaCronica(boolean doencaCronica) {
        this.doencaCronica = doencaCronica;
    }

    public boolean isMobilidadeReduzida() {
        return mobilidadeReduzida;
    }

    public void setMobilidadeReduzida(boolean mobilidadeReduzida) {
        this.mobilidadeReduzida = mobilidadeReduzida;
    }

    public boolean isCasaSemVentilacao() {
        return casaSemVentilacao;
    }

    public void setCasaSemVentilacao(boolean casaSemVentilacao) {
        this.casaSemVentilacao = casaSemVentilacao;
    }

    public boolean isAreaRisco() {
        return areaRisco;
    }

    public void setAreaRisco(boolean areaRisco) {
        this.areaRisco = areaRisco;
    }
}
