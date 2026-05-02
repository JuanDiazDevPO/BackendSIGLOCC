package com.siglocc.repository;

import com.siglocc.entity.DetalleEntregaIglesia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Repositorio de acceso a la tabla {@code detalle_entrega_iglesia}. */
public interface DetalleEntregaIglesiaRepository extends JpaRepository<DetalleEntregaIglesia, Integer> {

    /** Lista todos los detalles de un acta de entrega. */
    List<DetalleEntregaIglesia> findByEntregaId(Integer entregaId);

    /** Elimina todos los detalles de un acta (para reemplazar en caso de corrección). */
    @Transactional
    void deleteByEntregaId(Integer entregaId);
}
