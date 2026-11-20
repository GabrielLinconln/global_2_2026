package br.com.fiap.plantao.model;

import br.com.fiap.plantao.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.Year;

public class Idoso extends Pessoa implements AvaliavelRisco {
    private long idRegiao;
    private int anoNascimento;
    private String telefoneFixo;
    private String enderecoRef;
    private String contatoFamiliar;
    private boolean moraSozinho = true;
    private boolean condicaoCronica;
    private boolean semVentilacao;
    private boolean mobilidadeReduzida;
    private boolean areaRisco;
    private LocalDate consentimentoEm;
    private String consentidoPor;

    public int getIdade() {
        return Year.now().getValue() - anoNascimento;
    }

    @Override
    public void validar() {
        validarNome();
        if (anoNascimento < 1900 || anoNascimento > Year.now().getValue()) {
            throw new RegraNegocioException("Ano de nascimento inv\u00e1lido.");
        }
        if (getIdade() < 60) {
            throw new RegraNegocioException("O Plant\u00e3o Clima atende pessoas com 60 anos ou mais.");
        }
        boolean temCel = getTelefone() != null && !getTelefone().isEmpty();
        boolean temFixo = telefoneFixo != null && !telefoneFixo.isEmpty();
        if (!temCel && !temFixo) {
            throw new RegraNegocioException("Informe ao menos um telefone (celular ou fixo).");
        }
        if ((temCel && !telefoneValido(getTelefone())) || (temFixo && !telefoneValido(telefoneFixo))) {
            throw new RegraNegocioException("Telefone deve ter DDD + n\u00famero, s\u00f3 d\u00edgitos.");
        }
        if (consentimentoEm == null || consentimentoEm.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("Registre a data do consentimento (LGPD).");
        }
        if (!"PROPRIO".equals(consentidoPor) && !"RESPONSAVEL".equals(consentidoPor)) {
            throw new RegraNegocioException("Informe quem consentiu: a pr\u00f3pria pessoa ou o respons\u00e1vel.");
        }
    }

    // calor pesa saude e casa quente; chuva pesa area de risco e dificuldade de sair de casa
    @Override
    public Risco calcularRisco(TipoAlerta tipo) {
        int pontos = getIdade() >= 80 ? 1 : 0;
        if (mobilidadeReduzida) pontos++;
        if (tipo == TipoAlerta.CALOR) {
            if (condicaoCronica) pontos++;
            if (semVentilacao) pontos++;
        } else if (areaRisco) {
            pontos += 2;
        }
        return pontos >= 2 ? Risco.ALTO : pontos == 1 ? Risco.MEDIO : Risco.BAIXO;
    }

    // para a tela mostrar o risco em texto
    public Risco getRiscoCalor() { return calcularRisco(TipoAlerta.CALOR); }
    public Risco getRiscoChuva() { return calcularRisco(TipoAlerta.CHUVA); }

    public long getIdRegiao() { return idRegiao; }
    public void setIdRegiao(long idRegiao) { this.idRegiao = idRegiao; }
    public int getAnoNascimento() { return anoNascimento; }
    public void setAnoNascimento(int anoNascimento) { this.anoNascimento = anoNascimento; }
    public String getTelefoneFixo() { return telefoneFixo; }
    public void setTelefoneFixo(String telefoneFixo) { this.telefoneFixo = telefoneFixo; }
    public String getEnderecoRef() { return enderecoRef; }
    public void setEnderecoRef(String enderecoRef) { this.enderecoRef = enderecoRef; }
    public String getContatoFamiliar() { return contatoFamiliar; }
    public void setContatoFamiliar(String contatoFamiliar) { this.contatoFamiliar = contatoFamiliar; }
    public boolean isMoraSozinho() { return moraSozinho; }
    public void setMoraSozinho(boolean moraSozinho) { this.moraSozinho = moraSozinho; }
    public boolean isCondicaoCronica() { return condicaoCronica; }
    public void setCondicaoCronica(boolean condicaoCronica) { this.condicaoCronica = condicaoCronica; }
    public boolean isSemVentilacao() { return semVentilacao; }
    public void setSemVentilacao(boolean semVentilacao) { this.semVentilacao = semVentilacao; }
    public boolean isMobilidadeReduzida() { return mobilidadeReduzida; }
    public void setMobilidadeReduzida(boolean mobilidadeReduzida) { this.mobilidadeReduzida = mobilidadeReduzida; }
    public boolean isAreaRisco() { return areaRisco; }
    public void setAreaRisco(boolean areaRisco) { this.areaRisco = areaRisco; }
    public LocalDate getConsentimentoEm() { return consentimentoEm; }
    public void setConsentimentoEm(LocalDate consentimentoEm) { this.consentimentoEm = consentimentoEm; }
    public String getConsentidoPor() { return consentidoPor; }
    public void setConsentidoPor(String consentidoPor) { this.consentidoPor = consentidoPor; }
}
