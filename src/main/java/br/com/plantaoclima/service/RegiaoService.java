package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.RegiaoDao;
import br.com.plantaoclima.exception.RecursoNaoEncontradoException;
import br.com.plantaoclima.exception.ValidacaoException;
import br.com.plantaoclima.model.Regiao;

import java.util.List;
import java.util.Set;

public class RegiaoService {

    private static final Set<String> NIVEIS_VALIDOS = Set.of("BAIXO", "MODERADO", "ALTO");
    private final RegiaoDao regiaoDao;

    public RegiaoService(RegiaoDao regiaoDao) {
        this.regiaoDao = regiaoDao;
    }

    public List<Regiao> listarTodas() {
        return regiaoDao.listarTodos();
    }

    public Regiao buscarPorId(Long id) {
        return regiaoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Regiao nao encontrada com ID " + id));
    }

    public Regiao criar(Regiao regiao) {
        validar(regiao);
        return regiaoDao.inserir(regiao);
    }

    public Regiao atualizar(Long id, Regiao regiao) {
        buscarPorId(id);
        regiao.setId(id);
        validar(regiao);
        return regiaoDao.atualizar(regiao);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        regiaoDao.excluir(id);
    }

    private void validar(Regiao regiao) {
        if (regiao == null) {
            throw new ValidacaoException("Dados da regiao sao obrigatorios.");
        }
        if (regiao.getNome() == null || regiao.getNome().trim().length() < 3) {
            throw new ValidacaoException("Informe o nome da regiao (minimo 3 caracteres).");
        }
        if (regiao.getBairroZona() == null || regiao.getBairroZona().trim().isEmpty()) {
            throw new ValidacaoException("Informe o bairro ou zona da regiao.");
        }
        if (regiao.getRiscoCalor() == null || !NIVEIS_VALIDOS.contains(regiao.getRiscoCalor().toUpperCase())) {
            throw new ValidacaoException("Risco de calor deve ser BAIXO, MODERADO ou ALTO.");
        }
        if (regiao.getRiscoChuva() == null || !NIVEIS_VALIDOS.contains(regiao.getRiscoChuva().toUpperCase())) {
            throw new ValidacaoException("Risco de chuva deve ser BAIXO, MODERADO ou ALTO.");
        }
        if (regiao.getReferenciaUbs() == null || regiao.getReferenciaUbs().trim().isEmpty()) {
            throw new ValidacaoException("Informe a UBS de referencia da regiao.");
        }
        regiao.setNome(regiao.getNome().trim());
        regiao.setBairroZona(regiao.getBairroZona().trim());
        regiao.setRiscoCalor(regiao.getRiscoCalor().trim().toUpperCase());
        regiao.setRiscoChuva(regiao.getRiscoChuva().trim().toUpperCase());
        regiao.setReferenciaUbs(regiao.getReferenciaUbs().trim());
    }
}
