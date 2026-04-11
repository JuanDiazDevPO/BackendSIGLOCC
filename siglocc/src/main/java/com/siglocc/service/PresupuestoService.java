package com.siglocc.service;

import com.siglocc.dto.CargaMasivaResponse;
import com.siglocc.dto.PresupuestoRequest;
import com.siglocc.dto.PresupuestoResponse;
import com.siglocc.entity.PresupuestoDatos;
import com.siglocc.repository.EquipoRepository;
import com.siglocc.repository.MetaEquipoRepository;
import com.siglocc.repository.ParametrosNconnectRepository;
import com.siglocc.repository.PresupuestoDatosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio que gestiona la ingesta de datos operativos de presupuesto por equipo.
 *
 * <p>Implementa dos modalidades de carga:</p>
 * <ul>
 *   <li>{@link #guardar} – Carga manual individual de un solo equipo.</li>
 *   <li>{@link #procesarCsv} – ETL masivo desde archivo CSV (sin detener el proceso ante errores).</li>
 * </ul>
 *
 * <p><strong>Prerequisitos que se validan en ambas modalidades:</strong></p>
 * <ol>
 *   <li>Que el ENL haya configurado los parámetros para la temporada indicada
 *       ({@code parametros_nconnect}). Sin esto, el módulo está bloqueado.</li>
 *   <li>Que el equipo tenga una meta de contenedores asignada en {@code metas_equipo}.</li>
 *   <li>Que el equipo exista en la BD.</li>
 * </ol>
 *
 * <p><strong>Formato CSV esperado (11 columnas, con encabezado):</strong></p>
 * <pre>
 * equipo_id,temporada_id,promedio_cm,num_pv,entrenadores_pv,personas_pv,maestros_lga,num_cap,entrenadores_cap,mentoreo_equipos,oracion_cop
 * 2,1,49.5,100,2,3,384,3,12,3,50000.00
 * </pre>
 */
@Service
public class PresupuestoService {

    private final PresupuestoDatosRepository presupuestoRepo;
    private final ParametrosNconnectRepository parametrosRepo;
    private final MetaEquipoRepository metaEquipoRepo;
    private final EquipoRepository equipoRepo;

    public PresupuestoService(PresupuestoDatosRepository presupuestoRepo,
                              ParametrosNconnectRepository parametrosRepo,
                              MetaEquipoRepository metaEquipoRepo,
                              EquipoRepository equipoRepo) {
        this.presupuestoRepo = presupuestoRepo;
        this.parametrosRepo = parametrosRepo;
        this.metaEquipoRepo = metaEquipoRepo;
        this.equipoRepo = equipoRepo;
    }

    /**
     * Registra los datos de presupuesto de un equipo de forma individual.
     *
     * <p>Valida los prerequisitos, construye la entidad y la persiste.
     * Si ya existe un registro para la misma combinación equipo+temporada,
     * lo sobreescribe con los nuevos valores.</p>
     *
     * @param request datos operativos del equipo
     * @return DTO con el ID del registro y mensaje de confirmación con el nombre del equipo
     * @throws IllegalStateException    si no existen parámetros ENL para la temporada
     * @throws IllegalArgumentException si el equipo no existe o no tiene meta asignada
     */
    @Transactional
    public PresupuestoResponse guardar(PresupuestoRequest request) {
        validarPrerequisitos(request.equipoId(), request.temporadaId());

        String nombreEquipo = equipoRepo.findById(request.equipoId())
                .map(e -> e.getNombre())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipo no encontrado con id: " + request.equipoId()));

        PresupuestoDatos datos = presupuestoRepo
                .findByEquipoIdAndTemporadaId(request.equipoId(), request.temporadaId())
                .orElse(new PresupuestoDatos());

        datos.setEquipoId(request.equipoId());
        datos.setTemporadaId(request.temporadaId());
        datos.setPromedioCmCont(request.promedioCmCont());
        datos.setNumPv(request.numPv());
        datos.setEntrenadoresPv(request.entrenadoresPv());
        datos.setPersonasPv(request.personasPv());
        datos.setMaestrosLga(request.maestrosLga());
        datos.setNumCapOcc(request.numCapOcc());
        datos.setNumEntrenadoresCap(request.numEntrenadoresCap());
        datos.setEquiposBajoMentoreo(request.equiposBajoMentoreo());
        datos.setMontoOracionCop(request.montoOracionCop());

        presupuestoRepo.save(datos);

        return new PresupuestoResponse(
                "Presupuesto creado exitosamente para " + nombreEquipo,
                datos.getId()
        );
    }

    /**
     * Procesa un archivo CSV con datos de presupuesto de múltiples equipos (ETL masivo).
     *
     * <p><strong>Comportamiento ante errores:</strong> Si una fila falla por cualquier motivo
     * (equipo inexistente, dato no numérico, prerequisito no cumplido), el error se registra
     * en la lista de errores y el proceso continúa con la siguiente fila sin interrumpirse.</p>
     *
     * <p><strong>Formato esperado del CSV:</strong> 11 columnas separadas por comas,
     * primera fila es el encabezado (se omite). Orden:
     * {@code equipo_id, temporada_id, promedio_cm, num_pv, entrenadores_pv, personas_pv,
     * maestros_lga, num_cap, entrenadores_cap, mentoreo_equipos, oracion_cop}.</p>
     *
     * @param archivo archivo CSV enviado como {@code multipart/form-data}
     * @return resumen del proceso: procesados, exitosos, fallidos y lista de errores
     */
    @Transactional
    public CargaMasivaResponse procesarCsv(MultipartFile archivo) {
        List<String> errores = new ArrayList<>();
        int exitosos = 0;
        int procesados = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            int numeroFila = 0;

            while ((linea = reader.readLine()) != null) {
                numeroFila++;

                // Saltar la fila de encabezado
                if (numeroFila == 1) continue;

                // Saltar líneas vacías
                if (linea.isBlank()) continue;

                procesados++;

                try {
                    procesarFilaCsv(linea, numeroFila);
                    exitosos++;
                } catch (Exception e) {
                    errores.add("Fila " + numeroFila + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errores.add("Error al leer el archivo: " + e.getMessage());
        }

        return new CargaMasivaResponse(procesados, exitosos, procesados - exitosos, errores);
    }

    /**
     * Parsea una línea del CSV y persiste el registro de presupuesto correspondiente.
     *
     * <p>Lanza una excepción con mensaje descriptivo si cualquier campo es inválido,
     * permitiendo que {@link #procesarCsv} registre el error y continúe.</p>
     *
     * @param linea      línea del CSV a procesar
     * @param numeroFila número de fila para mensajes de error
     * @throws IllegalArgumentException si los datos son inválidos o los prerequisitos no se cumplen
     * @throws NumberFormatException    si algún campo no tiene formato numérico correcto
     */
    private void procesarFilaCsv(String linea) {
        String[] campos = linea.split(",", -1);

        if (campos.length != 11) {
            throw new IllegalArgumentException(
                    "Se esperan 11 columnas pero se encontraron " + campos.length);
        }

        Integer equipoId;
        Integer temporadaId;

        try {
            equipoId = Integer.parseInt(campos[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo equipo_id no es numérico: '" + campos[0].trim() + "'");
        }

        try {
            temporadaId = Integer.parseInt(campos[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo temporada_id no es numérico: '" + campos[1].trim() + "'");
        }

        // Verificar prerequisitos antes de parsear el resto
        validarPrerequisitos(equipoId, temporadaId);

        if (!equipoRepo.existsById(equipoId)) {
            throw new IllegalArgumentException("El equipo ID " + equipoId + " no existe.");
        }

        try {
            BigDecimal promedioCm  = new BigDecimal(campos[2].trim());
            Integer    numPv       = Integer.parseInt(campos[3].trim());
            Integer    entPv       = Integer.parseInt(campos[4].trim());
            Integer    persPv      = Integer.parseInt(campos[5].trim());
            Integer    maestros    = Integer.parseInt(campos[6].trim());
            Integer    numCap      = Integer.parseInt(campos[7].trim());
            Integer    entCap      = Integer.parseInt(campos[8].trim());
            Integer    mentoreo    = Integer.parseInt(campos[9].trim());
            BigDecimal oracion     = new BigDecimal(campos[10].trim());

            PresupuestoDatos datos = presupuestoRepo
                    .findByEquipoIdAndTemporadaId(equipoId, temporadaId)
                    .orElse(new PresupuestoDatos());

            datos.setEquipoId(equipoId);
            datos.setTemporadaId(temporadaId);
            datos.setPromedioCmCont(promedioCm);
            datos.setNumPv(numPv);
            datos.setEntrenadoresPv(entPv);
            datos.setPersonasPv(persPv);
            datos.setMaestrosLga(maestros);
            datos.setNumCapOcc(numCap);
            datos.setNumEntrenadoresCap(entCap);
            datos.setEquiposBajoMentoreo(mentoreo);
            datos.setMontoOracionCop(oracion);

            presupuestoRepo.save(datos);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato numérico inválido en columna de datos: " + e.getMessage());
        }
    }

    /**
     * Valida que existan los prerequisitos necesarios para ingresar datos de presupuesto.
     *
     * @param equipoId    ID del equipo a validar
     * @param temporadaId ID de la temporada a validar
     * @throws IllegalStateException    si el ENL no configuró los parámetros para la temporada
     * @throws IllegalArgumentException si el equipo no tiene meta asignada para la temporada
     */
    private void validarPrerequisitos(Integer equipoId, Integer temporadaId) {
        if (parametrosRepo.findByTemporadaId(temporadaId).isEmpty()) {
            throw new IllegalStateException(
                    "El módulo de presupuestos está bloqueado: el ENL no ha configurado " +
                    "los parámetros para la temporada " + temporadaId + ".");
        }

        if (!metaEquipoRepo.existsByEquipoIdAndTemporadaId(equipoId, temporadaId)) {
            throw new IllegalArgumentException(
                    "El equipo ID " + equipoId + " no tiene meta de contenedores asignada " +
                    "para la temporada " + temporadaId + ".");
        }
    }
}
