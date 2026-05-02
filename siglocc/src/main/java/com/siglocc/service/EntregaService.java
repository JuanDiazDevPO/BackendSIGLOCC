package com.siglocc.service;

import com.siglocc.dto.*;
import com.siglocc.entity.*;
import com.siglocc.repository.*;
import com.siglocc.security.JwtAuthDetails;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar las entregas de cajas y literatura a iglesias (Momento 3).
 *
 * <p>También gestiona los dos momentos de fotos de evidencia:</p>
 * <ul>
 *   <li><strong>Momento B:</strong> fotos de la entrega en el punto de distribución.</li>
 *   <li><strong>Momento C:</strong> fotos de la entrega a los niños en la iglesia.</li>
 * </ul>
 */
@Service
public class EntregaService {

    private final EntregaIglesiaRepository entregaRepo;
    private final DetalleEntregaIglesiaRepository detalleRepo;
    private final TipoItemRepository tipoItemRepo;
    private final IglesiaRepository iglesiaRepo;
    private final FotoEntregaIglesiaRepository fotoEntregaRepo;
    private final FotoEntregaNinosRepository fotoNinosRepo;
    private final UsuarioRepository usuarioRepo;
    private final StorageService storageService;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Beans singletons gestionados por Spring.")
    public EntregaService(EntregaIglesiaRepository entregaRepo,
                          DetalleEntregaIglesiaRepository detalleRepo,
                          TipoItemRepository tipoItemRepo,
                          IglesiaRepository iglesiaRepo,
                          FotoEntregaIglesiaRepository fotoEntregaRepo,
                          FotoEntregaNinosRepository fotoNinosRepo,
                          UsuarioRepository usuarioRepo,
                          StorageService storageService) {
        this.entregaRepo    = entregaRepo;
        this.detalleRepo    = detalleRepo;
        this.tipoItemRepo   = tipoItemRepo;
        this.iglesiaRepo    = iglesiaRepo;
        this.fotoEntregaRepo = fotoEntregaRepo;
        this.fotoNinosRepo  = fotoNinosRepo;
        this.usuarioRepo    = usuarioRepo;
        this.storageService = storageService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREAR ENTREGA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea el acta de entrega de cajas y literatura a una iglesia aprobada.
     *
     * @param request datos del acta con los ítems a entregar
     * @return respuesta con el acta creada en estado PENDIENTE
     * @throws IllegalArgumentException si la iglesia no existe, no está aprobada, ya tiene acta,
     *                                  algún tipo de ítem no existe, o las cantidades son inválidas
     */
    @Transactional
    public EntregaResponse crearEntrega(EntregaRequest request) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId = details.equipoId();

        if (request.iglesiaId() == null) {
            throw new IllegalArgumentException("El iglesiaId es obligatorio.");
        }
        if (request.puntoEntregaId() == null) {
            throw new IllegalArgumentException("El puntoEntregaId es obligatorio.");
        }
        if (request.temporadaId() == null) {
            throw new IllegalArgumentException("El temporadaId es obligatorio.");
        }

        Iglesia iglesia = iglesiaRepo.findById(request.iglesiaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Iglesia no encontrada con id: " + request.iglesiaId()));

        if (iglesia.getEstado() != EstadoIglesia.APROBADA) {
            throw new IllegalArgumentException(
                    "La iglesia '" + iglesia.getNombre()
                    + "' no está aprobada. Solo se pueden crear actas para iglesias aprobadas.");
        }

        if (entregaRepo.existsByIglesiaIdAndTemporadaId(
                request.iglesiaId(), request.temporadaId())) {
            throw new IllegalArgumentException(
                    "Ya existe un acta de entrega para la iglesia "
                    + request.iglesiaId() + " en la temporada " + request.temporadaId() + ".");
        }

        // Validar tipo de firma
        FirmaTipo firmaTipo = null;
        if (request.firmaTipo() != null && !request.firmaTipo().isBlank()) {
            try {
                firmaTipo = FirmaTipo.valueOf(request.firmaTipo().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "firmaTipo inválido: '" + request.firmaTipo()
                        + "'. Valores permitidos: DIGITAL, ESCANEADA.");
            }
        }

        // Validar y preparar detalles
        List<DetalleEntregaIglesia> detalles = new ArrayList<>();
        for (EntregaDetalleRequest dr : request.detalles()) {
            if (dr.tipoItemId() == null) {
                throw new IllegalArgumentException("Cada detalle debe tener un tipoItemId.");
            }
            if (dr.cantidadEntregada() == null || dr.cantidadEntregada() <= 0) {
                throw new IllegalArgumentException(
                        "La cantidad entregada para el ítem " + dr.tipoItemId()
                        + " debe ser mayor que cero.");
            }
            tipoItemRepo.findById(dr.tipoItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tipo de ítem no encontrado con id: " + dr.tipoItemId()));

            DetalleEntregaIglesia detalle = new DetalleEntregaIglesia();
            detalle.setTipoItemId(dr.tipoItemId());
            detalle.setCantidadEntregada(dr.cantidadEntregada());
            detalles.add(detalle);
        }

        // Persistir el acta
        EntregaIglesia entrega = new EntregaIglesia();
        entrega.setIglesiaId(request.iglesiaId());
        entrega.setPuntoEntregaId(request.puntoEntregaId());
        entrega.setTemporadaId(request.temporadaId());
        entrega.setEquipoId(equipoId);
        entrega.setFechaEntrega(request.fechaEntrega());
        entrega.setFirmaTipo(firmaTipo);
        entrega.setEstado(EstadoEntrega.PENDIENTE);
        entrega.setObservaciones(request.observaciones());
        entrega.setConfirmado(false);
        entrega.setFechaRegistro(LocalDateTime.now());
        entregaRepo.save(entrega);

        for (DetalleEntregaIglesia detalle : detalles) {
            detalle.setEntregaId(entrega.getId());
            detalleRepo.save(detalle);
        }

        return construirResponse(entrega, detalles);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPLETAR ENTREGA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Marca un acta de entrega como COMPLETADA y la confirma.
     *
     * @param id     ID del acta
     * @param parcial si es {@code true}, el estado pasa a PARCIAL; si es {@code false}, a COMPLETADA
     * @return respuesta actualizada del acta
     */
    @Transactional
    public EntregaResponse completarEntrega(Integer id, boolean parcial) {
        EntregaIglesia entrega = entregaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta de entrega no encontrada con id: " + id));

        if (entrega.getEstado() != EstadoEntrega.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se pueden completar actas en estado PENDIENTE. "
                    + "Estado actual: " + entrega.getEstado().name());
        }

        entrega.setEstado(parcial ? EstadoEntrega.PARCIAL : EstadoEntrega.COMPLETADA);
        entrega.setConfirmado(true);
        entregaRepo.save(entrega);

        List<DetalleEntregaIglesia> detalles = detalleRepo.findByEntregaId(id);
        return construirResponse(entrega, detalles);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBIR FIRMA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adjunta la firma del receptor al acta de entrega.
     *
     * @param id    ID del acta
     * @param firma archivo PDF o imagen de la firma
     * @return respuesta actualizada del acta con la URL de la firma
     */
    @Transactional
    public EntregaResponse subirFirma(Integer id, MultipartFile firma) {
        EntregaIglesia entrega = entregaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta de entrega no encontrada con id: " + id));

        String nombreArchivo = storageService.almacenarFotoLogistica(
                firma, "firma", id, 1);

        entrega.setFirmaUrl(nombreArchivo);
        entregaRepo.save(entrega);

        List<DetalleEntregaIglesia> detalles = detalleRepo.findByEntregaId(id);
        return construirResponse(entrega, detalles);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOTOS MOMENTO B — ENTREGA A IGLESIA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sube una foto de evidencia de la entrega en el punto de distribución (Momento B).
     *
     * @param entregaId ID del acta de entrega
     * @param foto      imagen JPG/PNG
     * @return ruta del archivo almacenado
     * @throws IllegalArgumentException si el acta no existe
     */
    @Transactional
    public String subirFotoEntrega(Integer entregaId, MultipartFile foto) {
        if (!entregaRepo.existsById(entregaId)) {
            throw new IllegalArgumentException(
                    "Acta de entrega no encontrada con id: " + entregaId);
        }

        int orden = (int) fotoEntregaRepo.countByEntregaId(entregaId) + 1;
        String rutaArchivo = storageService.almacenarFotoLogistica(
                foto, "entrega", entregaId, orden);

        FotoEntregaIglesia fotoEntity = new FotoEntregaIglesia();
        fotoEntity.setEntregaId(entregaId);
        fotoEntity.setUrl(rutaArchivo);
        fotoEntity.setOrden(orden);
        fotoEntity.setSubidoPor(resolverUsuarioId());
        fotoEntity.setFechaCarga(LocalDateTime.now());
        fotoEntregaRepo.save(fotoEntity);

        return rutaArchivo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOTOS MOMENTO C — ENTREGA A NIÑOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sube una foto de evidencia de la entrega a los niños en la iglesia (Momento C).
     *
     * @param iglesiaId   ID de la iglesia
     * @param temporadaId ID de la temporada
     * @param foto        imagen JPG/PNG
     * @return ruta del archivo almacenado
     * @throws IllegalArgumentException si la iglesia no existe
     */
    @Transactional
    public String subirFotoNinos(Integer iglesiaId, Integer temporadaId, MultipartFile foto) {
        if (!iglesiaRepo.existsById(iglesiaId)) {
            throw new IllegalArgumentException(
                    "Iglesia no encontrada con id: " + iglesiaId);
        }

        int orden = (int) fotoNinosRepo.countByIglesiaIdAndTemporadaId(iglesiaId, temporadaId) + 1;
        String rutaArchivo = storageService.almacenarFotoLogistica(
                foto, "ninos", iglesiaId, orden);

        FotoEntregaNinos fotoEntity = new FotoEntregaNinos();
        fotoEntity.setIglesiaId(iglesiaId);
        fotoEntity.setTemporadaId(temporadaId);
        fotoEntity.setUrl(rutaArchivo);
        fotoEntity.setOrden(orden);
        fotoEntity.setSubidoPor(resolverUsuarioId());
        fotoEntity.setFechaCarga(LocalDateTime.now());
        fotoNinosRepo.save(fotoEntity);

        return rutaArchivo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTAR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lista las entregas visibles para el usuario autenticado en una temporada.
     *
     * @param temporadaId ID de la temporada
     * @return lista de actas de entrega según jerarquía
     */
    public List<EntregaResponse> listar(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<EntregaIglesia> entregas = switch (equipoTipo) {
            case "ENL"  -> entregaRepo.findByTemporadaId(temporadaId);
            case "ERLE" -> entregaRepo.findByErleClusterAndTemporada(equipoId, temporadaId);
            case "ERL"  -> entregaRepo.findByEquipoIdAndTemporadaId(equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido: " + equipoTipo);
        };

        return entregas.stream()
                .map(e -> construirResponse(e, detalleRepo.findByEntregaId(e.getId())))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private EntregaResponse construirResponse(EntregaIglesia e, List<DetalleEntregaIglesia> detalles) {
        List<EntregaDetalleResponse> detalleResponses = detalles.stream()
                .map(d -> {
                    TipoItem item = tipoItemRepo.findById(d.getTipoItemId()).orElse(null);
                    String codigo = item != null ? item.getCodigo() : String.valueOf(d.getTipoItemId());
                    String nombre = item != null ? item.getNombreCompleto() : "";
                    return new EntregaDetalleResponse(
                            d.getTipoItemId(), codigo, nombre, d.getCantidadEntregada());
                })
                .toList();

        return new EntregaResponse(
                e.getId(), e.getIglesiaId(), e.getPuntoEntregaId(), e.getTemporadaId(),
                e.getEquipoId(), e.getFechaEntrega(),
                e.getFirmaTipo() != null ? e.getFirmaTipo().name() : null,
                e.getFirmaUrl(), e.getEstado().name(), e.getObservaciones(),
                e.getConfirmado(), e.getFechaRegistro(), detalleResponses
        );
    }

    private JwtAuthDetails obtenerDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getDetails() instanceof JwtAuthDetails details)) {
            throw new IllegalStateException(
                    "El token no contiene identidad jerárquica válida.");
        }
        return details;
    }

    private Integer resolverUsuarioId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepo.findByEmail(email).map(Usuario::getId).orElse(null);
    }
}
