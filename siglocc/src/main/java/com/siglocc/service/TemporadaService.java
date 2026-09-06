package com.siglocc.service;

import com.siglocc.dto.TemporadaRequest;
import com.siglocc.dto.TemporadaResponse;
import com.siglocc.entity.Temporada;
import com.siglocc.repository.TemporadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Servicio de gestión de temporadas: creación, edición de fechas y activación.
 *
 * <p>La invariante central de este servicio es que <strong>nunca puede haber
 * más de una temporada con {@code esActual = true}</strong> al mismo tiempo —
 * el resto del sistema (anticipos, dashboard, reportes) asume que
 * {@code findByEsActualTrue()} devuelve como máximo una fila. Por eso activar
 * una temporada es una operación transaccional separada de crear/editar,
 * nunca implícita.</p>
 */
@Service
public class TemporadaService {

    private final TemporadaRepository temporadaRepo;

    public TemporadaService(TemporadaRepository temporadaRepo) {
        this.temporadaRepo = temporadaRepo;
    }

    /**
     * Crea una nueva temporada. Siempre nace con {@code esActual = false} —
     * activarla es un paso explícito aparte con {@link #activar}.
     *
     * @param request nombre y fechas de la nueva temporada
     * @return DTO de la temporada creada
     * @throws IllegalArgumentException si el nombre está vacío, faltan fechas,
     *                                  o {@code fechaFin} no es posterior a {@code fechaInicio}
     */
    @Transactional
    public TemporadaResponse crear(TemporadaRequest request) {
        validarDatos(request);

        Temporada temporada = new Temporada();
        temporada.setNombre(request.nombre());
        temporada.setFechaInicio(request.fechaInicio());
        temporada.setFechaFin(request.fechaFin());
        temporada.setEsActual(false);

        temporadaRepo.save(temporada);
        return toResponse(temporada);
    }

    /**
     * Edita el nombre y las fechas de una temporada existente.
     *
     * <p>No toca {@code esActual} — para eso está {@link #activar}.</p>
     *
     * @param id      ID de la temporada a editar
     * @param request nuevo nombre y fechas
     * @return DTO actualizado
     * @throws NoSuchElementException   si la temporada no existe (→ 404)
     * @throws IllegalArgumentException si los datos no son válidos
     */
    @Transactional
    public TemporadaResponse editar(Integer id, TemporadaRequest request) {
        validarDatos(request);

        Temporada temporada = temporadaRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Temporada no encontrada con id: " + id));

        temporada.setNombre(request.nombre());
        temporada.setFechaInicio(request.fechaInicio());
        temporada.setFechaFin(request.fechaFin());

        temporadaRepo.save(temporada);
        return toResponse(temporada);
    }

    /**
     * Marca una temporada como la actual, desactivando automáticamente
     * cualquier otra que lo estuviera.
     *
     * <p><strong>Atomicidad:</strong> ambos cambios (desactivar la vieja,
     * activar la nueva) ocurren en la misma transacción — nunca hay una
     * ventana en la que existan cero o dos temporadas activas.</p>
     *
     * @param id ID de la temporada a activar
     * @return DTO de la temporada ya activa
     * @throws NoSuchElementException si la temporada no existe (→ 404)
     */
    @Transactional
    public TemporadaResponse activar(Integer id) {
        Temporada nueva = temporadaRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Temporada no encontrada con id: " + id));

        temporadaRepo.findByEsActualTrue().ifPresent(actual -> {
            if (!actual.getId().equals(id)) {
                actual.setEsActual(false);
                temporadaRepo.save(actual);
            }
        });

        nueva.setEsActual(true);
        temporadaRepo.save(nueva);
        return toResponse(nueva);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void validarDatos(TemporadaRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la temporada es obligatorio.");
        }
        if (request.fechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        }
        if (request.fechaFin() == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria.");
        }
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    private TemporadaResponse toResponse(Temporada t) {
        return new TemporadaResponse(t.getId(), t.getNombre(),
                t.getFechaInicio(), t.getFechaFin(), t.isEsActual());
    }
}
