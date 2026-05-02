package com.siglocc.repository;

import com.siglocc.entity.FotoContenedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Repositorio de acceso a la tabla {@code fotos_contenedor}. */
public interface FotoContenedorRepository extends JpaRepository<FotoContenedor, Integer> {

    /** Lista las fotos de una recepción de contenedor ordenadas por su posición. */
    List<FotoContenedor> findByRecepcionIdOrderByOrden(Integer recepcionId);

    /** Cuenta las fotos existentes para una recepción (máximo 4 permitidas). */
    long countByRecepcionId(Integer recepcionId);

    /** Elimina todas las fotos de una recepción (para limpieza). */
    @Transactional
    void deleteByRecepcionId(Integer recepcionId);
}
