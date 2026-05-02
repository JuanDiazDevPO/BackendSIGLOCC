package com.siglocc.controller;

import com.siglocc.dto.CargaMasivaIglesiasResponse;
import com.siglocc.dto.IglesiaAprobacionRequest;
import com.siglocc.dto.IglesiaRequest;
import com.siglocc.dto.IglesiaResponse;
import com.siglocc.service.IglesiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST del Momento 1 – La Visión (inscripción de iglesias).
 *
 * <p>Expone cuatro endpoints:</p>
 * <ol>
 *   <li>{@code POST  /api/v1/logistica/iglesias} – registrar una iglesia.</li>
 *   <li>{@code POST  /api/v1/logistica/iglesias/upload} – carga masiva CSV.</li>
 *   <li>{@code PATCH /api/v1/logistica/iglesias/{id}/estado} – aprobar o rechazar.</li>
 *   <li>{@code GET   /api/v1/logistica/iglesias?temporadaId=} – listar.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/logistica/iglesias")
@Tag(name = "Logística – Momento 1 Iglesias", description = "Inscripción y aprobación de iglesias participantes")
public class IglesiaController {

    private final IglesiaService iglesiaService;

    public IglesiaController(IglesiaService iglesiaService) {
        this.iglesiaService = iglesiaService;
    }

    /**
     * Registra una nueva iglesia en la operación logística.
     * El equipoId se extrae del JWT; la iglesia queda en estado PENDIENTE.
     */
    @Operation(summary = "Registrar iglesia",
               description = "Inscribe una iglesia en la operación. Estado inicial: PENDIENTE. " +
                             "El equipoId se toma del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<IglesiaResponse> registrar(@RequestBody IglesiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iglesiaService.registrar(request));
    }

    /**
     * Carga masiva de iglesias desde un archivo CSV.
     *
     * <p><strong>Formato CSV (13 columnas, primera fila = encabezado):</strong><br>
     * {@code nombre,denominacion,departamento,ciudad,direccion,pastor_nombre,pastor_celular,
     * pastor_correo,nombre_lider,celular_lider,correo_lider,temporada_id,cajas_solicitadas}</p>
     */
    @Operation(summary = "Carga masiva de iglesias (CSV)",
               description = "Procesa todas las filas sin detenerse ante errores. " +
                             "Los fallos se reportan en la respuesta con número de fila y motivo.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CargaMasivaIglesiasResponse> registrarMasivo(
            @RequestParam MultipartFile archivo) {
        return ResponseEntity.ok(iglesiaService.registrarMasivo(archivo));
    }

    /**
     * Aprueba o rechaza la participación de una iglesia.
     * Cualquier equipo autenticado puede tomar esta decisión.
     */
    @Operation(summary = "Aprobar o rechazar iglesia",
               description = "Cualquier equipo puede cambiar el estado de PENDIENTE a APROBADA o RECHAZADA. " +
                             "El motivo de rechazo es obligatorio si la decisión es RECHAZADA.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<IglesiaResponse> aprobar(
            @PathVariable Integer id,
            @RequestBody IglesiaAprobacionRequest request) {
        return ResponseEntity.ok(iglesiaService.aprobar(id, request));
    }

    /**
     * Lista las iglesias visibles para el usuario autenticado en una temporada.
     * ENL ve todas; ERLE ve su clúster; ERL ve solo las suyas.
     */
    @Operation(summary = "Listar iglesias",
               description = "Filtrado automático por jerarquía: ENL ve todas, " +
                             "ERLE ve su clúster, ERL solo las suyas.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<IglesiaResponse>> listar(@RequestParam Integer temporadaId) {
        return ResponseEntity.ok(iglesiaService.listar(temporadaId));
    }

}
