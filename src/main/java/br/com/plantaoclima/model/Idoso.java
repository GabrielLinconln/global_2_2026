package br.com.plantaoclima.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa o idoso que mora sozinho protegido pela rede comunitaria.
 * Armazena apenas dados minimos (ano de nascimento) e registra consentimento LGPD.
 */
public class Idoso extends Pessoa implements AvaliavelRisco {
    private int anoNascimento;
    private String enderecoResumo;
    private boolean doencaCronica;
    private boolean mobilidadeReduzida;
    private boolean casaSemVentilacao;
    private boolean areaRisco;
    private boolean consentimentoLgpd;
    private LocalDateTime dataConsentimento;
    private String observacoes;
    private Long idRegiao;
    private String nomeRegiao;

    public Idoso() {
        super();
    }

    @Override
    public String getTipoPessoa() {
        return "IDOSO";
    }

    public int getIdadeEstimada() {
        return Math.max(0, LocalDate.now().getYear() - anoNascimento);
    }

    @Override
    public int calcularPontuacaoRisco(TipoAlerta tipoAlerta) {
        int pontos = 0;
        int idade = getIdadeEstimada();

        if (idade >= 80) {
            pontos += 25;
        } else if (idade >= 70) {
            pontos += 15;
        } else {
            pontos += 10;
        }

        if (tipoAlerta == TipoAlerta.CALOR) {
            // No calor extremo, pesam doenca cronica e casa sem ventilacao
            if (doencaCronica) {
                pontos += 35;
            }
            if (casaSemVentilacao) {
                pontos += 30;
            }
            if (mobilidadeReduzida) {
                pontos += 10;
            }
            if (areaRisco) {
                pontos += 5;
            }
        } else {
            // Na chuva intensa, pesam area de risco e mobilidade reduzida
            if (areaRisco) {
                pontos += 35;
            }
            if (mobilidadeReduzida) {
                pontos += 30;
            }
            if (doencaCronica) {
                pontos += 10;
            }
            if (casaSemVentilacao) {
                pontos += 5;
            }
        }

        return Math.min(100, pontos);
    }

    @Override
    public String classificarNivelRisco(TipoAlerta tipoAlerta) {
        int score = calcularPontuacaoRisco(tipoAlerta);
        if (score >= 75) {
            return "MUITO_ALTO";
        }
        if (score >= 45) {
            return "ALTO";
        }
        return "MODERADO";
    }

    public int getAnoNascimento() {
        return anoNascimento;
    }

    public void setAnoNascimento(int anoNascimento) {
        this.anoNascimento = anoNascimento;
    }

    public String getEnderecoResumo() {
        return enderecoResumo;
    }

    public void setEnderecoResumo(String enderecoResumo) {
        this.enderecoResumo = enderecoResumo;
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

    public boolean isConsentimentoLgpd() {
        return consentimentoLgpd;
    }

    public void setConsentimentoLgpd(boolean consentimentoLgpd) {
        this.consentimentoLgpd = consentimentoLgpd;
    }

    public LocalDateTime getDataConsentimento() {
        return dataConsentimento;
    }

    public void setDataConsentimento(LocalDateTime dataConsentimento) {
        this.dataConsentimento = dataConsentimento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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
