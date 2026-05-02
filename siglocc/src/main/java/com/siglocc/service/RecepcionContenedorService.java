package com.siglocc.service;

import com.siglocc.dto.DetalleRecepcionRequest;
import com.siglocc.dto.DetalleRecepcionResponse;
import com.siglocc.dto.RecepcionContenedorRequest;
import com.siglocc.dto.RecepcionContenedorResponse;
import com.siglocc.entity.CategoriaCaja;
import com.siglocc.entity.DetalleRecepcionContenedor;
import com.siglocc.entity.FotoContenedor;
import com.siglocc.entity.RecepcionContenedor;
import com.siglocc.entity.TipoItem;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.DetalleRecepcionContenedorRepository;
import com.siglocc.repository.FotoContenedorRepository;
import com.siglocc.repository.RecepcionContenedorRepository;
import com.siglocc.repository.TipoItemRepository;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.security.JwtAuthDetails;
import com.siglocc.entity.Usuario;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar la recepción de contenedores (Momento A de fotos).
 *
 * <p>Cubre el registro del contenedor con su desglose por categoría de caja
 * y tipo de literatura, la carga de documentos adjuntos (lista transportadora
 * y documento ABC) y la carga de hasta 4 fotos de evidencia.</p>
 */
@Service
public class RecepcionContenedorService {

    private static final int MAX_FOTOS_CONTENEDOR = 4;

    private final RecepcionContenedorRepository recepcionRepo;
    private final DetalleRecepcionContenedorRepository detalleRepo;
    private final FotoContenedorRepository fotoRepo;
    private final CategoriaCajaRepository categoriaCajaRepo;
    private final TipoItemRepository tipoItemRepo;
    private final UsuarioRepository usuarioRepo;
    private final StorageService storageService;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Beans singletons gestionados por Spring.")
    public RecepcionContenedorService(RecepcionContenedorRepository recepcionRepo,
                                      DetalleRecepcionContenedorRepository detalleRepo,
                                      FotoContenedorRepository fotoRepo,
                                      CategoriaCajaRepository categoriaCajaRepo,
                                      TipoItemRepository tipoItemRepo,
                                      UsuarioRepository usuarioRepo,
                                      StorageService storageService) {
        this.recepcionRepo      = recepcionRepo;
        this.detalleRepo        = detalleRepo;
        this.fotoRepo           = fotoRepo;
        this.categoriaCajaRepo  = categoriaCajaRepo;
        this.tipoItemRepo       = tipoItemRepo;
        this.usuarioRepo        = usuarioRepo;
        this.storageService     = storageService;
    }

    /**
     * Registra la llegada de un contenedor a un punto de entrega con su desglose
     * de inventario por categoría de caja y tipo de literatura.
     *
     * <p>El campo {@code totalCajasRecibidas} se calcula automáticamente
     * sumando las cantidades de todos los detalles cuyo {@code categoriaCajaId}
     * sea no nulo.</p>
     *
     * @param request datos del contenedor con lista de detalles
     * @return respuesta con el registro creado y sus detalles enriquecidos
     * @throws IllegalArgumentException si faltan campos o hay detalles inválidos
     */
    @Transactional
    public RecepcionContenedorResponse registrar(RecepcionContenedorRequest request) {
        JwtAuthDetails details = obtenerDetails();

        if (request.numeroContenedor() == null || request.numeroContenedor().isBlank()) {
            throw new IllegalArgumentException("El número de contenedor es obligatorio.");
        }
        if (request.puntoEntregaId() == null) {
            throw new IllegalArgumentException("El puntoEntregaId es obligatorio.");
        }
        if (request.temporadaId() == null) {
            throw new IllegalArgumentException("El temporadaId es obligatorio.");
        }
        if (request.fechaLlegada() == null) {
            throw new IllegalArgumentException("La fecha de llegada es obligatoria.");
        }
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new IllegalArgumentException("Se debe incluir al menos un detalle de inventario recibido.");
        }

        // Validate each detail and compute totalCajasRecibidas
        int totalCajasRecibidas = 0;
        for (DetalleRecepcionRequest det : request.detalles()) {
            boolean tieneCaja = det.categoriaCajaId() != null;
            boolean tieneItem = det.tipoItemId() != null;
            if (tieneCaja == tieneItem) {
                throw new IllegalArgumentException(
                        "Cada detalle debe tener exactamente uno de categoriaCajaId o tipoItemId, no ambos ni ninguno.");
            }
            if (det.cantidad() == null || det.cantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada detalle debe ser mayor que cero.");
            }
            if (tieneCaja) {
                totalCajasRecibidas += det.cantidad();
            }
        }

        RecepcionContenedor recepcion = new RecepcionContenedor();
        recepcion.setNumeroContenedor(request.numeroContenedor());
        recepcion.setPuntoEntregaId(request.puntoEntregaId());
        recepcion.setTemporadaId(request.temporadaId());
        recepcion.setFechaLlegada(request.fechaLlegada());
        recepcion.setTotalCajasRecibidas(totalCajasRecibidas);
        recepcion.setEquipoId(details.equipoId());
        recepcion.setObservaciones(request.observaciones());
        recepcion.setFechaRegistro(LocalDateTime.now());
        recepcionRepo.save(recepcion);

        List<DetalleRecepcionContenedor> detalles = new ArrayList<>();
        for (DetalleRecepcionRequest det : request.detalles()) {
            DetalleRecepcionContenedor entidad = new DetalleRecepcionContenedor();
            entidad.setRecepcionId(recepcion.getId());
            entidad.setCategoriaCajaId(det.categoriaCajaId());
            entidad.setTipoItemId(det.tipoItemId());
            entidad.setCantidad(det.cantidad());
            detalles.add(entidad);
        }
        detalleRepo.saveAll(detalles);

        return construirResponse(recepcion, detalles);
    }

    /**
     * Sube un documento adjunto a una recepción de contenedor.
     *
     * @param id       ID de la recepción
     * @param tipo     «TRANSPORTADORA» para la lista de la transportadora, «ABC» para el formato interno
     * @param documento archivo PDF
     * @return respuesta actualizada de la recepción
     * @throws IllegalArgumentException si la recepción no existe o el tipo es inválido
     */
    @Transactional
    public RecepcionContenedorResponse subirDocumento(Integer id, String tipo, MultipartFile documento) {
        RecepcionContenedor recepcion = recepcionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recepción no encontrada con id: " + id));

        if (!"TRANSPORTADORA".equalsIgnoreCase(tipo) && !"ABC".equalsIgnoreCase(tipo)) {
            throw new IllegalArgumentException(
                    "Tipo de documento inválido: '" + tipo + "'. Use TRANSPORTADORA o ABC.");
        }

        String rutaArchivo = storageService.almacenarDocumentoLogistica(documento, tipo, id);

        if ("TRANSPORTADORA".equalsIgnoreCase(tipo)) {
            recepcion.setListaTransportadoraUrl(rutaArchivo);
        } else {
            recepcion.setDocumentoAbcUrl(rutaArchivo);
        }

        recepcionRepo.save(recepcion);
        List<DetalleRecepcionContenedor> detalles = detalleRepo.findByRecepcionId(id);
        return construirResponse(recepcion, detalles);
    }

    /**
     * Sube una foto de evidencia de la llegada del contenedor.
     *
     * <p>Se permiten hasta {@value #MAX_FOTOS_CONTENEDOR} fotos por recepción.
     * La primera (orden = 1) debe mostrar el número en la puerta del contenedor.</p>
     *
     * @param id          ID de la recepción
     * @param foto        imagen JPG/PNG
     * @param descripcion descripción opcional de la foto
     * @return la ruta del archivo almacenado
     * @throws IllegalArgumentException si la recepción no existe
     * @throws IllegalStateException    si ya se alcanzó el límite de 4 fotos
     */
    @Transactional
    public String subirFoto(Integer id, MultipartFile foto, String descripcion) {
        if (!recepcionRepo.existsById(id)) {
            throw new IllegalArgumentException(
                    "Recepción no encontrada con id: " + id);
        }

        long totalFotos = fotoRepo.countByRecepcionId(id);
        if (totalFotos >= MAX_FOTOS_CONTENEDOR) {
            throw new IllegalStateException(
                    "Ya se alcanzó el máximo de " + MAX_FOTOS_CONTENEDOR
                    + " fotos para esta recepción.");
        }

        int orden = (int) totalFotos + 1;
        String rutaArchivo = storageService.almacenarFotoLogistica(foto, "contenedor", id, orden);

        FotoContenedor fotoEntity = new FotoContenedor();
        fotoEntity.setRecepcionId(id);
        fotoEntity.setUrl(rutaArchivo);
        fotoEntity.setOrden(orden);
        fotoEntity.setDescripcion(descripcion);
        fotoEntity.setSubidoPor(resolverUsuarioId());
        fotoEntity.setFechaCarga(LocalDateTime.now());

        fotoRepo.save(fotoEntity);
        return rutaArchivo;
    }

    /**
     * Lista las recepciones visibles para el usuario autenticado en una temporada.
     *
     * @param temporadaId ID de la temporada
     * @return lista de recepciones según jerarquía ENL / ERLE / ERL
     */
    public List<RecepcionContenedorResponse> listar(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<RecepcionContenedor> recepciones = switch (equipoTipo) {
            case "ENL"  -> recepcionRepo.findByTemporadaId(temporadaId);
            case "ERLE" -> recepcionRepo.findByErleClusterAndTemporada(equipoId, temporadaId);
            case "ERL"  -> recepcionRepo.findByEquipoIdAndTemporadaId(equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido: " + equipoTipo);
        };

        return recepciones.stream()
                .map(r -> construirResponse(r, detalleRepo.findByRecepcionId(r.getId())))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private RecepcionContenedorResponse construirResponse(RecepcionContenedor r,
                                                          List<DetalleRecepcionContenedor> detalles) {
        // Bulk-load lookup data to avoid N+1
        Map<Integer, CategoriaCaja> categoriaMap = categoriaCajaRepo.findAll().stream()
                .collect(Collectors.toMap(CategoriaCaja::getId, c -> c));
        Map<Integer, TipoItem> itemMap = tipoItemRepo.findAll().stream()
                .collect(Collectors.toMap(TipoItem::getId, t -> t));

        List<DetalleRecepcionResponse> detalleResponses = detalles.stream()
                .map(d -> {
                    String codigoCategoria    = null;
                    String descripcionCategoria = null;
                    String codigoItem         = null;

                    if (d.getCategoriaCajaId() != null) {
                        CategoriaCaja cat = categoriaMap.get(d.getCategoriaCajaId());
                        if (cat != null) {
                            codigoCategoria     = cat.getCodigo();
                            descripcionCategoria = cat.getDescripcion();
                        }
                    }
                    if (d.getTipoItemId() != null) {
                        TipoItem item = itemMap.get(d.getTipoItemId());
                        if (item != null) {
                            codigoItem = item.getCodigo();
                        }
                    }
                    return new DetalleRecepcionResponse(
                            d.getCategoriaCajaId(), codigoCategoria, descripcionCategoria,
                            d.getTipoItemId(), codigoItem, d.getCantidad());
                })
                .toList();

        return new RecepcionContenedorResponse(
                r.getId(), r.getNumeroContenedor(), r.getPuntoEntregaId(), r.getTemporadaId(),
                r.getFechaLlegada(), r.getTotalCajasRecibidas(), r.getEquipoId(),
                r.getObservaciones(), r.getListaTransportadoraUrl(), r.getDocumentoAbcUrl(),
                r.getFechaRegistro(), detalleResponses);
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
