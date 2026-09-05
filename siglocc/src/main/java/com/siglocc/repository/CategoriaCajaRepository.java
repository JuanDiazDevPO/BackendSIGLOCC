package com.siglocc.repository;

import com.siglocc.entity.CategoriaCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositorio de acceso a la tabla {@code categorias_caja}. */
public interface CategoriaCajaRepository extends JpaRepository<CategoriaCaja, Integer> {

    /** Retorna las 6 categorías estándar SP ordenadas por id, para un orden determinista. */
    List<CategoriaCaja> findAllByOrderById();
}
