package com.siglocc.repository;

import com.siglocc.entity.FamiliaCategoria;
import com.siglocc.entity.ReporteCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la tabla maestra {@code reporte_categorias}.
 *
 * <p>Permite consultar las categorías disponibles por familia, lo cual es útil
 * para que el front-end muestre solo las categorías que le corresponden al equipo
 * según su tipo (ERL solo ve familia E; ERLE y ENL ven todas).</p>
 */
public interface ReporteCategoriaRepository extends JpaRepository<ReporteCategoria, String> {

    /**
     * Retorna todas las categorías que pertenecen a la familia indicada.
     *
     * @param familia la familia de categorías a filtrar (E, M u O)
     * @return lista de categorías de esa familia, o lista vacía si no hay ninguna
     */
    List<ReporteCategoria> findByFamilia(FamiliaCategoria familia);
}
