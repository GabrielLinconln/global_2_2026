package br.com.fiap.plantao.model;

import br.com.fiap.plantao.exception.RegraNegocioException;

public abstract class Pessoa {
    private Long id;
    private String nome;
    private String telefone;
    private boolean ativo = true;

    // cada tipo de pessoa tem as proprias regras de cadastro (polimorfismo)
    public abstract void validar();

    protected void validarNome() {
        if (nome == null || nome.trim().length() < 2) {
            throw new RegraNegocioException("Informe o nome (m\u00ednimo 2 letras).");
        }
    }

    // DDD + numero, so digitos: 10 (fixo) ou 11 (celular)
    protected static boolean telefoneValido(String tel) {
        return tel != null && tel.matches("\\d{10,11}");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
