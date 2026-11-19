package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.IdosoDao;
import br.com.plantaoclima.dao.VinculoCuidadoDao;
import br.com.plantaoclima.dao.VoluntarioDao;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.VinculoCuidado;

import java.util.List;

public class VinculoService {

    private final VinculoCuidadoDao vinculoDao;
    private final IdosoDao idosoDao;
    private final VoluntarioDao voluntarioDao;

    public VinculoService(VinculoCuidadoDao vinculoDao, IdosoDao idosoDao, VoluntarioDao voluntarioDao) {
        this.vinculoDao = vinculoDao;
        this.idosoDao = idosoDao;
        this.voluntarioDao = voluntarioDao;
    }

    public List<VinculoCuidado> listarTodos() {
        return vinculoDao.listarTodos();
    }

    public List<VinculoCuidado> listarPorIdoso(Long idIdoso) {
        return vinculoDao.listarPorIdoso(idIdoso);
    }

    public VinculoCuidado buscarPorId(Long id) {
        return vinculoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo de cuidado nao encontrado com ID " + id));
    }

    public VinculoCuidado criar(VinculoCuidado vinculo) {
        validar(vinculo);
        vinculo.setPrioridade(vinculo.getPapel().getPrioridadePadrao());
        return vinculoDao.inserir(vinculo);
    }

    public VinculoCuidado atualizar(Long id, VinculoCuidado vinculo) {
        buscarPorId(id);
        vinculo.setId(id);
        validar(vinculo);
        vinculo.setPrioridade(vinculo.getPapel().getPrioridadePadrao());
        return vinculoDao.atualizar(vinculo);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        vinculoDao.excluir(id);
    }

    private void validar(VinculoCuidado vinculo) {
        if (vinculo == null) {
            throw new ValidacaoException("Dados do vinculo sao obrigatorios.");
        }
        if (vinculo.getIdIdoso() == null || idosoDao.buscarPorId(vinculo.getIdIdoso()).isEmpty()) {
            throw new ValidacaoException("Selecione um idoso valido.");
        }
        if (vinculo.getIdVoluntario() == null || voluntarioDao.buscarPorId(vinculo.getIdVoluntario()).isEmpty()) {
            throw new ValidacaoException("Selecione um voluntario valido.");
        }
        if (vinculo.getPapel() == null) {
            throw new ValidacaoException("Selecione o papel do voluntario (TITULAR ou RESERVA).");
        }
    }
}
