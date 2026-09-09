package com.siglocc.controller;

import com.siglocc.dto.CambiarEstadoRequest;
import com.siglocc.dto.EditarReporteRequest;
import com.siglocc.dto.ReporteCategoriaResponse;
import com.siglocc.dto.ReporteRequest;
import com.siglocc.dto.ReporteResponse;
import com.siglocc.entity.FamiliaCategoria;
import com.siglocc.repository.ReporteCategoriaRepository;
import com.siglocc.service.ReporteService;
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
 * Controlador REST del módulo de Reportes Mensuales.
 *
 * <p>Expone cinco endpoints que cubren el ciclo de vida completo de un reporte:</p>
 * <ol>
 *   <li>{@code POST  /api/v1/reportes} – crear reporte con rubros.</li>
 *   <li>{@code PUT   /api/v1/reportes/{id}} – corregir reporte rechazado (reset a BORRADOR).</li>
 *   <li>{@code PUT   /api/v1/reportes/{id}/soporte} – adjuntar archivo de evidencia.</li>
 *   <li>{@code GET   /api/v1/reportes?temporadaId=} – listar reportes según jerarquía.</li>
 *   <li>{@code PATCH /api/v1/reportes/{id}/estado} – aprobar o rechazar.</li>
 * </ol>
 *
 * <p>Todos los endpoints requieren un token JWT válido en el header
 * {@code Authorization: Bearer <token>}. El control de acceso granular
 * (qué reportes puede ver cada usuario, qué puede aprobar) se delega al
 * {@link ReporteService}, que extrae el {@code equipoId} y {@code equipoTipo}
 * directamente del token.</p>
 */
@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes Mensuales", description = "Legalización de gastos mensuales por equipo")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteCategoriaRepository categoriaRepo;

    public ReporteController(ReporteService reporteService,
                             ReporteCategoriaRepository categoriaRepo) {
        this.reporteService  = reporteService;
        this.categoriaRepo   = categoriaRepo;
    }

    /**
     * Crea un nuevo reporte mensual con todos sus rubros de gasto en una sola operación.
     *
     * <p>El {@code equipoId} se toma del token JWT; el cliente no debe enviarlo.
     * El reporte se crea en estado {@code BORRADOR} hasta que se adjunte el soporte.</p>
     *
     * @param request temporada, mes, año y lista de rubros con sus montos
     * @return respuesta 201 con el reporte creado
     */
    @Operation(summary = "Crear reporte mensual",
               description = "Crea el cabezote y todos los rubros en una sola transacción. " +
                             "El equipoId se extrae del JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ReporteResponse> crearReporte(@RequestBody ReporteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reporteService.crearReporte(request));
    }

    /**
     * Corrige los rubros de un reporte rechazado y lo devuelve a estado BORRADOR.
     *
     * <p>Solo opera sobre reportes en estado {@code RECHAZADO}. El período
     * (mes, año, temporada) y el equipo no cambian. El soporte anterior se
     * borra porque los montos cambiaron; el ERL debe subir uno nuevo con
     * {@code PUT /{id}/soporte} para reiniciar el flujo de aprobación.</p>
     *
     * <p>Las observaciones del rechazo se conservan visibles durante la corrección.</p>
     *
     * @param id      ID del reporte rechazado a corregir
     * @param request nueva lista de rubros con montos corregidos
     * @return respuesta 200 con el reporte en estado BORRADOR
     */
    @Operation(summary = "Corregir reporte rechazado",
               description = "Reemplaza los rubros de un reporte RECHAZADO y lo resetea a BORRADOR. " +
                             "El ERL debe subir un nuevo soporte para reiniciar el flujo.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ReporteResponse> editarReporte(
            @PathVariable Integer id,
            @RequestBody EditarReporteRequest request) {
        return ResponseEntity.ok(reporteService.editarReporte(id, request));
    }

    /**
     * Adjunta el archivo de soporte (PDF/ZIP) a un reporte en estado BORRADOR
     * y avanza el estado según el tipo de equipo dueño del reporte: un ERL pasa
     * a {@code PENDIENTE_ERLE}, un ERLE pasa directo a {@code PENDIENTE_ENL}
     * (sin autoaprobación), y un ENL queda {@code APROBADO} de inmediato.
     *
     * <p>El archivo se recibe como {@code multipart/form-data} con el campo
     * {@code archivo}. Se almacena con el nombre
     * {@code SOPORTE_EQ{equipoId}_MES{mes}_{anio}.ext}.</p>
     *
     * @param id      ID del reporte al que se adjunta el soporte
     * @param archivo archivo PDF o ZIP de evidencia de gastos
     * @return respuesta 200 con el reporte actualizado en su nuevo estado
     */
    @Operation(summary = "Subir soporte del reporte",
               description = "Adjunta el archivo de evidencia. ERL -> PENDIENTE_ERLE, " +
                             "ERLE -> PENDIENTE_ENL directo, ENL -> APROBADO de inmediato.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{id}/soporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteResponse> subirSoporte(
            @PathVariable Integer id,
            @RequestParam MultipartFile archivo) {
        return ResponseEntity.ok(reporteService.subirSoporte(id, archivo));
    }

    /**
     * Lista los reportes visibles para el usuario autenticado en una temporada dada.
     *
     * <p>El nivel de visibilidad se determina automáticamente por el {@code equipoTipo}
     * del token:</p>
     * <ul>
     *   <li><strong>ENL:</strong> todos los reportes del país.</li>
     *   <li><strong>ERLE:</strong> su propio reporte más los de sus ERL subordinados.</li>
     *   <li><strong>ERL:</strong> solo sus propios reportes.</li>
     * </ul>
     *
     * @param temporadaId ID de la temporada para filtrar
     * @return lista de reportes con sus detalles y montos totales
     */
    @Operation(summary = "Listar reportes",
               description = "Filtra automáticamente por jerarquía: ENL ve todo, " +
                             "ERLE ve su clúster, ERL solo el suyo.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<ReporteResponse>> listarReportes(
            @RequestParam Integer temporadaId) {
        return ResponseEntity.ok(reporteService.listarReportes(temporadaId));
    }

    /**
     * Cambia el estado de un reporte en el flujo de aprobación.
     *
     * <p>Transiciones permitidas:</p>
     * <ul>
     *   <li><strong>ERLE:</strong> {@code PENDIENTE_ERLE → PENDIENTE_ENL} o {@code RECHAZADO}.</li>
     *   <li><strong>ENL:</strong> {@code PENDIENTE_ENL → APROBADO} o {@code RECHAZADO}.</li>
     * </ul>
     *
     * <p>Al aprobar como ENL (estado {@code APROBADO}), los montos del reporte
     * empiezan a contar como ejecutado en las vistas financieras.</p>
     *
     * @param id      ID del reporte a modificar
     * @param request nuevo estado y observaciones (obligatorias si se rechaza)
     * @return respuesta 200 con el reporte en su nuevo estado
     */
    @Operation(summary = "Cambiar estado del reporte",
               description = "Aprobar o rechazar. Solo APROBADO actualiza el ejecutado financiero.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReporteResponse> cambiarEstado(
            @PathVariable Integer id,
            @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(reporteService.cambiarEstado(id, request));
    }

    /**
     * Devuelve las categorías de gasto disponibles para los reportes mensuales.
     *
     * <p>Si se indica el parámetro {@code familia}, filtra por esa familia:
     * {@code E} (Entrenamiento), {@code M} (Mentoría) u {@code O} (Otros).
     * Si se omite, retorna todas las categorías.</p>
     *
     * @param familia filtro opcional: E, M u O
     * @return lista de categorías con código, familia y nombre
     */
    @Operation(summary = "Listar categorías de reporte",
               description = "Sin parámetro devuelve todas. Con familia=E/M/O filtra por familia.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/categorias")
    public ResponseEntity<List<ReporteCategoriaResponse>> listarCategorias(
            @RequestParam(required = false) String familia) {

        List<ReporteCategoriaResponse> resultado;

        if (familia != null && !familia.isBlank()) {
            FamiliaCategoria f;
            try {
                f = FamiliaCategoria.valueOf(familia.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Familia inválida: '" + familia + "'. Valores permitidos: E, M, O.");
            }
            resultado = categoriaRepo.findByFamilia(f).stream()
                    .map(c -> new ReporteCategoriaResponse(c.getCodigo(),
                            c.getFamilia().name(), c.getNombreLargo()))
                    .toList();
        } else {
            resultado = categoriaRepo.findAll().stream()
                    .map(c -> new ReporteCategoriaResponse(c.getCodigo(),
                            c.getFamilia().name(), c.getNombreLargo()))
                    .toList();
        }

        return ResponseEntity.ok(resultado);
    }

}
