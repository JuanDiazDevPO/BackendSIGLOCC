package com.siglocc.service;

import com.siglocc.dto.CapacitacionRequest;
import com.siglocc.dto.CapacitacionResponse;
import com.siglocc.entity.CapacitacionIglesia;
import com.siglocc.entity.EstadoIglesia;
import com.siglocc.entity.Iglesia;
import com.siglocc.repository.CapacitacionIglesiaRepository;
import com.siglocc.repository.IglesiaRepository;
import com.siglocc.security.JwtAuthDetails;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestionar las capacitaciones de maestros (Momento 2 – La Capacitación).
 *
 * <p><strong>Regla de cálculo:</strong> {@code cajasCalculadas = maestrosEnviados × 25}.
 * Solo se puede capacitar una iglesia que esté en estado {@link EstadoIglesia#APROBADA}.</p>
 */
@Service
public class CapacitacionService {

    private final CapacitacionIglesiaRepository capacitacionRepo;
    private final IglesiaRepository iglesiaRepo;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Beans singletons gestionados por Spring.")
    public CapacitacionService(CapacitacionIglesiaRepository capacitacionRepo,
                               IglesiaRepository iglesiaRepo) {
        this.capacitacionRepo = capacitacionRepo;
        this.iglesiaRepo      = iglesiaRepo;
    }

    /**
     * Registra la capacitación de los maestros de una iglesia aprobada.
     *
     * @param request datos de la capacitación
     * @return respuesta con los datos registrados y las cajas calculadas
     * @throws IllegalArgumentException si la iglesia no existe, no está aprobada, ya tiene
     *                                  capacitación registrada, o los datos son inválidos
     */
    @Transactional
    public CapacitacionResponse registrar(CapacitacionRequest request) {
        if (request.iglesiaId() == null) {
            throw new IllegalArgumentException("El iglesiaId es obligatorio.");
        }
        if (request.temporadaId() == null) {
            throw new IllegalArgumentException("El temporadaId es obligatorio.");
        }
        if (request.maestrosEnviados() == null || request.maestrosEnviados() <= 0) {
            throw new IllegalArgumentException(
                    "El número de maestros enviados debe ser mayor que cero.");
        }

        Iglesia iglesia = iglesiaRepo.findById(request.iglesiaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Iglesia no encontrada con id: " + request.iglesiaId()));

        if (iglesia.getEstado() != EstadoIglesia.APROBADA) {
            throw new IllegalStateException(
                    "La iglesia '" + iglesia.getNombre()
                    + "' no está aprobada. Estado actual: " + iglesia.getEstado().name());
        }

        if (capacitacionRepo.existsByIglesiaIdAndTemporadaId(
                request.iglesiaId(), request.temporadaId())) {
            throw new IllegalArgumentException(
                    "Ya existe un registro de capacitación para la iglesia "
                    + request.iglesiaId() + " en la temporada " + request.temporadaId() + ".");
        }

        int cajasCalculadas = request.maestrosEnviados() * 25;

        CapacitacionIglesia capacitacion = new CapacitacionIglesia();
        capacitacion.setIglesiaId(request.iglesiaId());
        capacitacion.setTemporadaId(request.temporadaId());
        capacitacion.setFechaCapacitacion(request.fechaCapacitacion());
        capacitacion.setMaestrosEnviados(request.maestrosEnviados());
        capacitacion.setCajasCalculadas(cajasCalculadas);
        capacitacion.setGmEntregados(request.gmEntregados() != null
                ? request.gmEntregados() : request.maestrosEnviados());
        capacitacion.setMpgEntregados(request.mpgEntregados() != null
                ? request.mpgEntregados() : request.maestrosEnviados());
        capacitacion.setObservaciones(request.observaciones());

        capacitacionRepo.save(capacitacion);
        return construirResponse(capacitacion);
    }

    /**
     * Lista las capacitaciones visibles para el usuario autenticado en una temporada.
     *
     * @param temporadaId ID de la temporada
     * @return lista de capacitaciones según jerarquía
     */
    public List<CapacitacionResponse> listar(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<CapacitacionIglesia> capacitaciones = switch (equipoTipo) {
            case "ENL"  -> capacitacionRepo.findByTemporadaId(temporadaId);
            case "ERLE" -> capacitacionRepo.findByErleClusterAndTemporada(equipoId, temporadaId);
            case "ERL"  -> capacitacionRepo.findByEquipoAndTemporada(equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido: " + equipoTipo);
        };

        return capacitaciones.stream().map(this::construirResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private CapacitacionResponse construirResponse(CapacitacionIglesia c) {
        return new CapacitacionResponse(
                c.getId(), c.getIglesiaId(), c.getTemporadaId(), c.getFechaCapacitacion(),
                c.getMaestrosEnviados(), c.getCajasCalculadas(), c.getGmEntregados(),
                c.getMpgEntregados(), c.getObservaciones()
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
}
