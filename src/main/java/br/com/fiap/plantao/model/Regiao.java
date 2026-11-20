package br.com.fiap.plantao.model;

import br.com.fiap.plantao.exception.RegraNegocioException;

public class Regiao {
    private Long id;
    private String nome;
    private String cidade;
    private String uf;
    private Double latitude;
    private Double longitude;

    public void validar() {
        if (nome == null || nome.trim().isEmpty() || cidade == null || cidade.trim().isEmpty()) {
            throw new RegraNegocioException("Informe nome e cidade da regi\u00e3o.");
        }
        if (uf == null || !uf.matches("[A-Z]{2}")) {
            throw new RegraNegocioException("UF deve ter 2 letras mai\u00fasculas.");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
