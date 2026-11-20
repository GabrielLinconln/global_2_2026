package br.com.fiap.plantao.model;

import br.com.fiap.plantao.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

public class Alerta {
    private Long id;
    private long idRegiao;
    private TipoAlerta tipo;
    private LocalDate dataAlerta;
    private String nivel;
    private Double tempMax;
    private String origem;
    private String fonteAviso;
    private LocalDateTime criadoEm;

    public void validar() {
        if (tipo == null || dataAlerta == null) {
            throw new RegraNegocioException("Informe o tipo e a data do alerta.");
        }
        if (!Arrays.asList("OBSERVACAO", "ATENCAO", "ALERTA", "MAXIMO").contains(nivel)) {
            throw new RegraNegocioException("N\u00edvel inv\u00e1lido.");
        }
        if (!Arrays.asList("INMET", "DEFESA_CIVIL", "OPEN_METEO", "FLOODGUARD", "MANUAL").contains(origem)) {
            throw new RegraNegocioException("Origem inv\u00e1lida.");
        }
        if (tempMax != null && (tempMax < -10 || tempMax > 60)) {
            throw new RegraNegocioException("Temperatura fora da faixa poss\u00edvel.");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getIdRegiao() { return idRegiao; }
    public void setIdRegiao(long idRegiao) { this.idRegiao = idRegiao; }
    public TipoAlerta getTipo() { return tipo; }
    public void setTipo(TipoAlerta tipo) { this.tipo = tipo; }
    public LocalDate getDataAlerta() { return dataAlerta; }
    public void setDataAlerta(LocalDate dataAlerta) { this.dataAlerta = dataAlerta; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public Double getTempMax() { return tempMax; }
    public void setTempMax(Double tempMax) { this.tempMax = tempMax; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getFonteAviso() { return fonteAviso; }
    public void setFonteAviso(String fonteAviso) { this.fonteAviso = fonteAviso; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
