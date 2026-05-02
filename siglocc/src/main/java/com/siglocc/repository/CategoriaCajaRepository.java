package com.siglocc.repository;

import com.siglocc.entity.CategoriaCaja;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de acceso a la tabla {@code categorias_caja}. */
public interface CategoriaCajaRepository extends JpaRepository<CategoriaCaja, Integer> {}
