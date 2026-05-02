package com.siglocc.repository;

import com.siglocc.entity.FotoEntregaNinos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Repositorio de acceso a la tabla {@code fotos_entrega_ninos}. */
public interface FotoEntregaNinosRepository extends JpaRepository<FotoEntregaNinos, Integer> {

    /** Lista las fotos de entrega a niños de una iglesia en una temporada, ordenadas. */
    List<FotoEntregaNinos> findByIglesiaIdAndTemporadaIdOrderByOrden(
            Integer iglesiaId, Integer temporadaId);

    /** Cuenta las fotos de una iglesia en una temporada. */
    long countByIglesiaIdAndTemporadaId(Integer iglesiaId, Integer temporadaId);

    /** Elimina todas las fotos de niños de una iglesia en una temporada. */
    @Transactional
    void deleteByIglesiaIdAndTemporadaId(Integer iglesiaId, Integer temporadaId);
}
