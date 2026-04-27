package com.siglocc.repository;

import com.siglocc.entity.FotoEntregaIglesia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Repositorio de acceso a la tabla {@code fotos_entrega_iglesia}. */
public interface FotoEntregaIglesiaRepository extends JpaRepository<FotoEntregaIglesia, Integer> {

    /** Lista las fotos de una entrega ordenadas por posición. */
    List<FotoEntregaIglesia> findByEntregaIdOrderByOrden(Integer entregaId);

    /** Cuenta las fotos de una entrega. */
    long countByEntregaId(Integer entregaId);

    /** Elimina todas las fotos de una entrega. */
    @Transactional
    void deleteByEntregaId(Integer entregaId);
}
