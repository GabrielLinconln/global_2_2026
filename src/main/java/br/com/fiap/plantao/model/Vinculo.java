package br.com.fiap.plantao.model;

import br.com.fiap.plantao.exception.RegraNegocioException;

import java.time.LocalDate;

// associacao N:N entre idoso e voluntario
public class Vinculo {
    private long idIdoso;
    private long idVoluntario;
    private String papel;
    private LocalDate desde = LocalDate.now();

    public void validar() {
        if (!"TITULAR".equals(papel) && !"RESERVA".equals(papel)) {
            throw new RegraNegocioException("Papel deve ser TITULAR ou RESERVA.");
        }
    }

    public long getIdIdoso() { return idIdoso; }
    public void setIdIdoso(long idIdoso) { this.idIdoso = idIdoso; }
    public long getIdVoluntario() { return idVoluntario; }
    public void setIdVoluntario(long idVoluntario) { this.idVoluntario = idVoluntario; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
}
