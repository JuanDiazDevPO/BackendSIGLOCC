package com.siglocc.controller;

import com.siglocc.dto.AnticipoRequest;
import com.siglocc.dto.AnticipoResponse;
import com.siglocc.repository.SolicitudAnticipoRepository;
import com.siglocc.service.AnticipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Controlador REST para el módulo de anticipos de presupuesto.
 *
 * <p>Expone tres endpoints:</p>
 * <ul>
 *   <li>{@code POST  /api/v1/anticipos}             – Crear solicitud de anticipo.</li>
 *   <li>{@code PATCH /api/v1/anticipos/{id}/aprobar} – Aprobar solicitud (solo ENL_RECURSOS).</li>
 *   <li>{@code GET   /api/v1/anticipos/{id}/pdf}     – Descargar el PDF formal de la solicitud.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/anticipos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Anticipos", description = "Solicitudes de anticipo de presupuesto")
public class AnticipoController {

    private final AnticipoService anticipoService;
    private final SolicitudAnticipoRepository solicitudRepo;

    @Value("${app.storage.path:uploads}")
    private String storagePath;

    public AnticipoController(AnticipoService anticipoService,
                               SolicitudAnticipoRepository solicitudRepo) {
        this.anticipoService = anticipoService;
        this.solicitudRepo   = solicitudRepo;
    }

    /**
     * Crea una nueva solicitud de anticipo.
     * El sistema valida el saldo disponible automáticamente y genera el PDF formal.
     */
    @Operation(summary = "Crear solicitud de anticipo",
               description = "Valida saldo, guarda la solicitud y genera el PDF formal. " +
                             "Devuelve 201 si queda PENDIENTE o 400 si es rechazada por saldo insuficiente.")
    @PostMapping
    public ResponseEntity<AnticipoResponse> crearSolicitud(@RequestBody AnticipoRequest request) {
        AnticipoResponse response = anticipoService.crearSolicitud(request);
        HttpStatus status = "RECHAZADO".equals(response.estado())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Aprueba una solicitud de anticipo en estado PENDIENTE.
     * Solo accesible para el rol {@code ENL_RECURSOS}.
     */
    @Operation(summary = "Aprobar solicitud de anticipo",
               description = "Solo ENL_RECURSOS puede aprobar. Notifica al solicitante por correo.")
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ENL_RECURSOS')")
    public ResponseEntity<AnticipoResponse> aprobar(@PathVariable Integer id) {
        return ResponseEntity.ok(anticipoService.aprobarSolicitud(id));
    }

    /**
     * Descarga el PDF formal de la solicitud de anticipo.
     * El archivo se genera automáticamente al crear la solicitud.
     */
    @Operation(summary = "Descargar PDF de la solicitud",
               description = "Devuelve el PDF formal generado al crear la solicitud. " +
                             "Retorna 404 si el PDF aún no fue generado.")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> descargarPdf(@PathVariable Integer id) throws IOException {
        var solicitudOpt = solicitudRepo.findById(id);
        if (solicitudOpt.isEmpty() || solicitudOpt.get().getRutaPdf() == null) {
            return ResponseEntity.notFound().build();
        }
        Path archivo = Path.of(storagePath).resolve(solicitudOpt.get().getRutaPdf());
        Resource recurso = new UrlResource(archivo.toUri());
        if (!recurso.exists() || !recurso.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ANTICIPO_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(recurso);
    }
}
