package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.RegiaoDao;
import br.com.plantaoclima.dao.VoluntarioDao;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.Voluntario;

import java.util.List;

public class VoluntarioService {

    private final VoluntarioDao voluntarioDao;
    private final RegiaoDao regiaoDao;

    public VoluntarioService(VoluntarioDao voluntarioDao, RegiaoDao regiaoDao) {
        this.voluntarioDao = voluntarioDao;
        this.regiaoDao = regiaoDao;
    }

    public List<Voluntario> listarTodos() {
        return voluntarioDao.listarTodos();
    }

    public Voluntario buscarPorId(Long id) {
        return voluntarioDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Voluntario nao encontrado com ID " + id));
    }

    public Voluntario criar(Voluntario voluntario) {
        validar(voluntario);
        return voluntarioDao.inserir(voluntario);
    }

    public Voluntario atualizar(Long id, Voluntario voluntario) {
        buscarPorId(id);
        voluntario.setId(id);
        validar(voluntario);
        return voluntarioDao.atualizar(voluntario);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        voluntarioDao.excluir(id);
    }

    private void validar(Voluntario voluntario) {
        if (voluntario == null) {
            throw new ValidacaoException("Dados do voluntario sao obrigatorios.");
        }
        if (voluntario.getNome() == null || voluntario.getNome().trim().length() < 3) {
            throw new ValidacaoException("Informe o nome do voluntario (minimo 3 caracteres).");
        }
        if (voluntario.getTelefone() == null || voluntario.getTelefone().replaceAll("\\D", "").length() < 8) {
            throw new ValidacaoException("Informe um telefone valido para o voluntario.");
        }
        if (voluntario.getRelacaoComunidade() == null || voluntario.getRelacaoComunidade().trim().isEmpty()) {
            throw new ValidacaoException("Informe a relacao comunitaria (ex.: Vizinho, Familiar, Sindico, Agente de Saude).");
        }
        if (voluntario.getDisponibilidade() == null || voluntario.getDisponibilidade().trim().isEmpty()) {
            throw new ValidacaoException("Informe a disponibilidade do voluntario.");
        }
        if (voluntario.getIdRegiao() == null || regiaoDao.buscarPorId(voluntario.getIdRegiao()).isEmpty()) {
            throw new ValidacaoException("Selecione uma regiao valida para o voluntario.");
        }

        voluntario.setNome(voluntario.getNome().trim());
        voluntario.setTelefone(voluntario.getTelefone().trim());
        voluntario.setRelacaoComunidade(voluntario.getRelacaoComunidade().trim());
        voluntario.setDisponibilidade(voluntario.getDisponibilidade().trim());
    }
}
