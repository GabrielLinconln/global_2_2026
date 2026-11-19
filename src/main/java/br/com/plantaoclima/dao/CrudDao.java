package br.com.plantaoclima.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interface generica de acesso a dados (padrao DAO) para operacoes CRUD via JDBC.
 */
public interface CrudDao<T> {
    T inserir(T entidade);

    List<T> listarTodos();

    Optional<T> buscarPorId(Long id);

    T atualizar(T entidade);

    boolean excluir(Long id);
}
