package com.siglocc.service;

import com.siglocc.dto.PuntoEntregaRequest;
import com.siglocc.dto.PuntoEntregaResponse;
import com.siglocc.entity.PuntoEntrega;
import com.siglocc.repository.PuntoEntregaRepository;
import com.siglocc.security.JwtAuthDetails;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para gestionar los puntos de entrega logística.
 *
 * <p>Solo los equipos ERLE pueden registrar puntos de entrega. La URL de Google Maps
 * se genera dinámicamente en la respuesta a partir de las coordenadas GPS.</p>
 */
@Service
public class PuntoEntregaService {

    private final PuntoEntregaRepository puntoRepo;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Bean singleton gestionado por Spring.")
    public PuntoEntregaService(PuntoEntregaRepository puntoRepo) {
        this.puntoRepo = puntoRepo;
    }

    /**
     * Registra un nuevo punto de entrega para el equipo ERLE autenticado.
     *
     * @param request datos del punto de entrega
     * @return respuesta con el punto creado y su URL de Google Maps
     * @throws IllegalArgumentException si faltan campos obligatorios
     * @throws IllegalStateException    si el equipo no es de tipo ERLE
     */
    @Transactional
    public PuntoEntregaResponse registrar(PuntoEntregaRequest request) {
        JwtAuthDetails details = obtenerDetails();

        if (!"ERLE".equals(details.equipoTipo())) {
            throw new IllegalStateException(
                    "Solo los equipos ERLE pueden registrar puntos de entrega.");
        }

        validarCamposObligatorios(request);

        PuntoEntrega punto = new PuntoEntrega();
        punto.setNombre(request.nombre());
        punto.setDepartamento(request.departamento());
        punto.setCiudad(request.ciudad());
        punto.setDireccion(request.direccion());
        punto.setCoordenadasLat(request.coordenadasLat());
        punto.setCoordenadasLng(request.coordenadasLng());
        punto.setRestriccionMovilidad(Boolean.TRUE.equals(request.restriccionMovilidad()));
        punto.setAlturaCuerdas(Boolean.TRUE.equals(request.alturaCuerdas()));
        punto.setNoTejasRotas(Boolean.TRUE.equals(request.noTejasRotas()));
        punto.setLugarSeguro(Boolean.TRUE.equals(request.lugarSeguro()));
        punto.setFacilAcceso(Boolean.TRUE.equals(request.facilAcceso()));
        punto.setEquipoId(details.equipoId());
        punto.setTemporadaId(request.temporadaId());

        puntoRepo.save(punto);
        return construirResponse(punto);
    }

    /**
     * Lista los puntos de entrega visibles para el usuario autenticado en una temporada.
     *
     * <ul>
     *   <li>ENL: todos los puntos de la temporada.</li>
     *   <li>ERLE: los propios más los de sus ERL subordinados.</li>
     *   <li>ERL: solo los de su propio equipo.</li>
     * </ul>
     *
     * @param temporadaId ID de la temporada
     * @return lista de puntos de entrega visibles
     */
    public List<PuntoEntregaResponse> listar(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<PuntoEntrega> puntos = switch (equipoTipo) {
            case "ENL"  -> puntoRepo.findByTemporadaId(temporadaId);
            case "ERLE" -> puntoRepo.findByErleClusterAndTemporada(equipoId, temporadaId);
            case "ERL"  -> puntoRepo.findByEquipoIdAndTemporadaId(equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido: " + equipoTipo);
        };

        return puntos.stream().map(this::construirResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private void validarCamposObligatorios(PuntoEntregaRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del punto de entrega es obligatorio.");
        }
        if (request.departamento() == null || request.departamento().isBlank()) {
            throw new IllegalArgumentException("El departamento es obligatorio.");
        }
        if (request.ciudad() == null || request.ciudad().isBlank()) {
            throw new IllegalArgumentException("La ciudad es obligatoria.");
        }
        if (request.temporadaId() == null) {
            throw new IllegalArgumentException("El temporadaId es obligatorio.");
        }
    }

    private PuntoEntregaResponse construirResponse(PuntoEntrega p) {
        String urlMaps = null;
        if (p.getCoordenadasLat() != null && p.getCoordenadasLng() != null
                && p.getCoordenadasLat().compareTo(BigDecimal.ZERO) != 0
                && p.getCoordenadasLng().compareTo(BigDecimal.ZERO) != 0) {
            urlMaps = "https://maps.google.com/?q=" + p.getCoordenadasLat()
                    + "," + p.getCoordenadasLng();
        }
        return new PuntoEntregaResponse(
                p.getId(), p.getNombre(), p.getDepartamento(), p.getCiudad(),
                p.getDireccion(), p.getCoordenadasLat(), p.getCoordenadasLng(), urlMaps,
                p.getRestriccionMovilidad(), p.getAlturaCuerdas(), p.getNoTejasRotas(),
                p.getLugarSeguro(), p.getFacilAcceso(), p.getEquipoId(), p.getTemporadaId()
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
