package com.siglocc.repository;

import com.siglocc.entity.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositorio de acceso a la tabla {@code tipos_item}. */
public interface TipoItemRepository extends JpaRepository<TipoItem, Integer> {

    /** Busca un tipo de ítem por su código abreviado (OE, FOLLETO, GM, etc.). */
    Optional<TipoItem> findByCodigo(String codigo);

    /**
     * Retorna el catálogo completo ordenado por momento operativo y luego por id,
     * para que el front lo muestre siempre FOLLETO → GM → MPG → EMR → LGA → NT.
     */
    List<TipoItem> findAllByOrderByMomentoAscIdAsc();
}
