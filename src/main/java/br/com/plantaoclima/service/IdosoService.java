package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.IdosoDao;
import br.com.plantaoclima.dao.RegiaoDao;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.Idoso;

import java.util.List;

public class IdosoService {

    private final IdosoDao idosoDao;
    private final RegiaoDao regiaoDao;

    public IdosoService(IdosoDao idosoDao, RegiaoDao regiaoDao) {
        this.idosoDao = idosoDao;
        this.regiaoDao = regiaoDao;
    }

    public List<Idoso> listarTodos() {
        return idosoDao.listarTodos();
    }

    public Idoso buscarPorId(Long id) {
        return idosoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso nao encontrado com ID " + id));
    }

    public Idoso criar(Idoso idoso) {
        validar(idoso);
        return idosoDao.inserir(idoso);
    }

    public Idoso atualizar(Long id, Idoso idoso) {
        buscarPorId(id);
        idoso.setId(id);
        validar(idoso);
        return idosoDao.atualizar(idoso);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        idosoDao.excluir(id);
    }

    private void validar(Idoso idoso) {
        if (idoso == null) {
            throw new ValidacaoException("Dados do idoso sao obrigatorios.");
        }
        if (idoso.getNome() == null || idoso.getNome().trim().length() < 3) {
            throw new ValidacaoException("Informe o nome do idoso (minimo 3 caracteres).");
        }
        if (idoso.getTelefone() == null || idoso.getTelefone().replaceAll("\\D", "").length() < 8) {
            throw new ValidacaoException("Informe um telefone valido com DDD (minimo 8 digitos).");
        }
        if (idoso.getAnoNascimento() < 1900 || idoso.getAnoNascimento() > 1975) {
            throw new ValidacaoException("Ano de nascimento invalido (permitido entre 1900 e 1975).");
        }
        if (idoso.getEnderecoResumo() == null || idoso.getEnderecoResumo().trim().length() < 5) {
            throw new ValidacaoException("Informe um endereco resumido para eventual visita presencial.");
        }
        if (!idoso.isConsentimentoLgpd()) {
            throw new ValidacaoException("O consentimento registrado do idoso (LGPD) e obrigatorio para o cadastro.");
        }
        if (idoso.getIdRegiao() == null || regiaoDao.buscarPorId(idoso.getIdRegiao()).isEmpty()) {
            throw new ValidacaoException("Selecione uma regiao valida para o idoso.");
        }

        idoso.setNome(idoso.getNome().trim());
        idoso.setTelefone(idoso.getTelefone().trim());
        idoso.setEnderecoResumo(idoso.getEnderecoResumo().trim());
        if (idoso.getObservacoes() != null) {
            idoso.setObservacoes(idoso.getObservacoes().trim());
        }
    }
}
