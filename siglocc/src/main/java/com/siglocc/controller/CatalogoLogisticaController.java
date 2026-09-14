package com.siglocc.controller;

import com.siglocc.dto.CategoriaCajaResponse;
import com.siglocc.dto.TipoItemResponse;
import com.siglocc.entity.CategoriaCaja;
import com.siglocc.entity.TipoItem;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.TipoItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogos de referencia del módulo de Logística.
 *
 * <p>Expone las categorías de caja OCC y los tipos de ítem/literatura, usados
 * como {@code categoriaCajaId}/{@code tipoItemId} en los detalles de
 * recepción de contenedores, asignación y entrega a iglesias (ver
 * {@link com.siglocc.dto.DetalleRecepcionRequest}, {@code AsignacionDetalleResponse},
 * {@code EntregaDetalleRequest}). El front debe consultar estos catálogos para
 * poblar los selects antes de enviar cualquiera de esos formularios con los
 * IDs correctos — no existían hasta ahora, aunque los repositorios ya estaban
 * listos para esto.</p>
 */
@RestController
@RequestMapping("/api/v1/logistica")
@Tag(name = "Logística – Catálogos", description = "Categorías de caja y tipos de ítem para poblar selects del front")
public class CatalogoLogisticaController {

    private final CategoriaCajaRepository categoriaCajaRepo;
    private final TipoItemRepository tipoItemRepo;

    public CatalogoLogisticaController(CategoriaCajaRepository categoriaCajaRepo,
                                       TipoItemRepository tipoItemRepo) {
        this.categoriaCajaRepo = categoriaCajaRepo;
        this.tipoItemRepo = tipoItemRepo;
    }

    /**
     * Lista las 6 categorías de caja OCC (género × rango de edad), ordenadas por id.
     */
    @Operation(summary = "Listar categorías de caja",
               description = "Las 6 categorías estándar SP (género × rango de edad). Usa el id devuelto " +
                             "como categoriaCajaId al registrar recepciones, asignaciones o entregas.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/categorias-caja")
    public ResponseEntity<List<CategoriaCajaResponse>> listarCategoriasCaja() {
        List<CategoriaCajaResponse> resultado = categoriaCajaRepo.findAllByOrderById().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(resultado);
    }

    /**
     * Lista el catálogo completo de tipos de ítem/literatura, ordenado por momento operativo.
     */
    @Operation(summary = "Listar tipos de ítem",
               description = "Folletos, guías y literatura (OE, FOLLETO, GM, MPG, EMR, LGA, NT), ordenados " +
                             "FOLLETO→GM→MPG→EMR→LGA→NT. Usa el id devuelto como tipoItemId.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/tipos-item")
    public ResponseEntity<List<TipoItemResponse>> listarTiposItem() {
        List<TipoItemResponse> resultado = tipoItemRepo.findAllByOrderByMomentoAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(resultado);
    }

    private CategoriaCajaResponse toResponse(CategoriaCaja c) {
        return new CategoriaCajaResponse(
                c.getId(), c.getCodigo(), c.getGenero().name(),
                c.getEdadMin(), c.getEdadMax(), c.getDescripcion());
    }

    private TipoItemResponse toResponse(TipoItem t) {
        return new TipoItemResponse(
                t.getId(), t.getCodigo(), t.getNombreCompleto(),
                t.getAplicaNinos(), t.getMomento());
    }
}
