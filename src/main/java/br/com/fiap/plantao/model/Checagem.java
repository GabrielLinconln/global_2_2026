package br.com.fiap.plantao.model;

import java.time.LocalDateTime;

// uma ligacao da rodada: tentativa 1 = titular, 2 = reserva
public class Checagem {
    private Long id;
    private long idAlerta;
    private long idIdoso;
    private long idVoluntario;
    private int tentativa;
    private Resultado resultado = Resultado.PENDENTE;
    private String canal;
    private LocalDateTime dataHora;
    private String observacao;
    private boolean registradoDepois;
    // preenchidos na listagem da rodada
    private Idoso idoso;
    private String nomeVoluntario;
    private Risco risco;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getIdAlerta() { return idAlerta; }
    public void setIdAlerta(long idAlerta) { this.idAlerta = idAlerta; }
    public long getIdIdoso() { return idIdoso; }
    public void setIdIdoso(long idIdoso) { this.idIdoso = idIdoso; }
    public long getIdVoluntario() { return idVoluntario; }
    public void setIdVoluntario(long idVoluntario) { this.idVoluntario = idVoluntario; }
    public int getTentativa() { return tentativa; }
    public void setTentativa(int tentativa) { this.tentativa = tentativa; }
    public Resultado getResultado() { return resultado; }
    public void setResultado(Resultado resultado) { this.resultado = resultado; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public boolean isRegistradoDepois() { return registradoDepois; }
    public void setRegistradoDepois(boolean registradoDepois) { this.registradoDepois = registradoDepois; }
    public Idoso getIdoso() { return idoso; }
    public void setIdoso(Idoso idoso) { this.idoso = idoso; }
    public String getNomeVoluntario() { return nomeVoluntario; }
    public void setNomeVoluntario(String nomeVoluntario) { this.nomeVoluntario = nomeVoluntario; }
    public Risco getRisco() { return risco; }
    public void setRisco(Risco risco) { this.risco = risco; }
}
