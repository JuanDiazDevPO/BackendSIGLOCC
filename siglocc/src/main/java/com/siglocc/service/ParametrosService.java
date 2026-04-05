package com.siglocc.service;

import com.siglocc.dto.ActualizarTasaCambioRequest;
import com.siglocc.dto.ClonarParametrosRequest;
import com.siglocc.dto.ParametrosRequest;
import com.siglocc.dto.ParametrosResponse;
import com.siglocc.entity.ParametrosNconnect;
import com.siglocc.repository.ParametrosNconnectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que gestiona los parámetros financieros globales del módulo ENL.
 *
 * <p>Implementa tres operaciones administrativas:</p>
 * <ul>
 *   <li>{@link #guardar} – Crea o actualiza los parámetros de una temporada.</li>
 *   <li>{@link #clonar} – Copia todos los parámetros de una temporada a otra.</li>
 *   <li>{@link #actualizarTasaCambio} – Actualiza solo la TRM en tiempo real.</li>
 * </ul>
 *
 * <p><strong>Impacto de la tasa de cambio:</strong> Al actualizarla, las vistas
 * de presupuesto de toda la red recalculan automáticamente sus valores en COP
 * sin necesidad de tocar datos adicionales.</p>
 */
@Service
public class ParametrosService {

    private final ParametrosNconnectRepository parametrosRepo;

    public ParametrosService(ParametrosNconnectRepository parametrosRepo) {
        this.parametrosRepo = parametrosRepo;
    }

    /**
     * Crea o actualiza los parámetros financieros para una temporada.
     *
     * <p>Si ya existe un registro para el {@code temporadaId} indicado, lo actualiza
     * con los nuevos valores. Si no existe, crea uno nuevo.</p>
     *
     * @param request datos financieros a guardar
     * @return DTO con el ID del registro y confirmación del resultado
     */
    @Transactional
    public ParametrosResponse guardar(ParametrosRequest request) {
        ParametrosNconnect parametros = parametrosRepo
                .findByTemporadaId(request.temporadaId())
                .orElse(new ParametrosNconnect());

        boolean esNuevo = parametros.getId() == null;

        parametros.setTemporadaId(request.temporadaId());
        parametros.setTasaCambio(request.tasaCambio());
        parametros.setCajasPorContenedor(request.cajasPorContenedor());
        parametros.setPorcentajeLga(request.porcentajeLga());
        parametros.setUsdAdminCm(request.usdAdminCm());
        parametros.setUsdRefrigeroPv(request.usdRefrigeroPv());
        parametros.setUsdTransportePv(request.usdTransportePv());
        parametros.setUsdTransporteCap(request.usdTransporteCap());
        parametros.setUsdRefrierioCap(request.usdRefrierioCap());
        parametros.setVisitasMentoreo(request.visitasMentoreo());
        parametros.setPersonasPorVisita(request.personasPorVisita());
        parametros.setUsdTransporteMentoreo(request.usdTransporteMentoreo());
        parametros.setUsdAlimentoMentoreo(request.usdAlimentoMentoreo());
        parametros.setUsdHospedajeMentoreo(request.usdHospedajeMentoreo());
        parametros.setUsdAdminMentoreo(request.usdAdminMentoreo());

        parametrosRepo.save(parametros);

        String mensaje = esNuevo
                ? "Parámetros creados correctamente para la temporada " + request.temporadaId()
                : "Parámetros actualizados correctamente para la temporada " + request.temporadaId();

        return new ParametrosResponse(parametros.getId(), parametros.getTemporadaId(), mensaje);
    }

    /**
     * Clona los parámetros de una temporada origen a una temporada destino.
     *
     * <p>Copia todos los valores financieros (incluyendo la tasa de cambio)
     * de la temporada origen. El coordinador ENL normalmente ajusta la
     * {@code tasaCambio} posteriormente con {@link #actualizarTasaCambio}.</p>
     *
     * @param request IDs de la temporada origen y destino
     * @return DTO con el ID del nuevo registro y confirmación
     * @throws IllegalArgumentException si no existen parámetros para la temporada origen
     * @throws IllegalStateException    si la temporada destino ya tiene parámetros configurados
     */
    @Transactional
    public ParametrosResponse clonar(ClonarParametrosRequest request) {
        ParametrosNconnect origen = parametrosRepo
                .findByTemporadaId(request.temporadaOrigenId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existen parámetros para la temporada origen: " + request.temporadaOrigenId()));

        if (parametrosRepo.findByTemporadaId(request.temporadaDestinoId()).isPresent()) {
            throw new IllegalStateException(
                    "La temporada destino " + request.temporadaDestinoId() + " ya tiene parámetros configurados.");
        }

        ParametrosNconnect destino = new ParametrosNconnect();
        destino.setTemporadaId(request.temporadaDestinoId());
        destino.setTasaCambio(origen.getTasaCambio());
        destino.setCajasPorContenedor(origen.getCajasPorContenedor());
        destino.setPorcentajeLga(origen.getPorcentajeLga());
        destino.setUsdAdminCm(origen.getUsdAdminCm());
        destino.setUsdRefrigeroPv(origen.getUsdRefrigeroPv());
        destino.setUsdTransportePv(origen.getUsdTransportePv());
        destino.setUsdTransporteCap(origen.getUsdTransporteCap());
        destino.setUsdRefrierioCap(origen.getUsdRefrierioCap());
        destino.setVisitasMentoreo(origen.getVisitasMentoreo());
        destino.setPersonasPorVisita(origen.getPersonasPorVisita());
        destino.setUsdTransporteMentoreo(origen.getUsdTransporteMentoreo());
        destino.setUsdAlimentoMentoreo(origen.getUsdAlimentoMentoreo());
        destino.setUsdHospedajeMentoreo(origen.getUsdHospedajeMentoreo());
        destino.setUsdAdminMentoreo(origen.getUsdAdminMentoreo());

        parametrosRepo.save(destino);

        return new ParametrosResponse(
                destino.getId(),
                destino.getTemporadaId(),
                "Parámetros clonados exitosamente de la temporada "
                        + request.temporadaOrigenId() + " a la temporada " + request.temporadaDestinoId()
        );
    }

    /**
     * Actualiza únicamente la tasa de cambio de una temporada.
     *
     * <p>Este es el endpoint de mayor impacto en tiempo real: al ejecutarse,
     * todos los cálculos de presupuesto en COP de todos los equipos de la
     * temporada se actualizan automáticamente en la próxima consulta a la vista.</p>
     *
     * @param temporadaId ID de la temporada cuya TRM se va a actualizar
     * @param request     nuevo valor de la tasa de cambio
     * @return DTO con confirmación y el nuevo valor aplicado
     * @throws IllegalArgumentException si no existen parámetros para esa temporada
     */
    @Transactional
    public ParametrosResponse actualizarTasaCambio(Integer temporadaId, ActualizarTasaCambioRequest request) {
        ParametrosNconnect parametros = parametrosRepo
                .findByTemporadaId(temporadaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existen parámetros para la temporada: " + temporadaId));

        parametros.setTasaCambio(request.tasaCambio());
        parametrosRepo.save(parametros);

        return new ParametrosResponse(
                parametros.getId(),
                parametros.getTemporadaId(),
                "Tasa de cambio actualizada a " + request.tasaCambio()
                        + " COP/USD para la temporada " + temporadaId
                        + ". Todos los presupuestos de la red se actualizan automáticamente."
        );
    }
}
