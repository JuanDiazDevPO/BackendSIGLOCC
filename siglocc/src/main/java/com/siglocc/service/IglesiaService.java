package com.siglocc.service;

import com.siglocc.dto.*;
import com.siglocc.entity.EstadoIglesia;
import com.siglocc.entity.Iglesia;
import com.siglocc.repository.IglesiaRepository;
import com.siglocc.security.JwtAuthDetails;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar las iglesias inscritas en la operación logística (Momento 1).
 *
 * <p>Soporta dos modos de registro:</p>
 * <ol>
 *   <li><strong>Individual:</strong> un formulario por iglesia vía
 *       {@link #registrar(IglesiaRequest)}.</li>
 *   <li><strong>Masivo:</strong> carga de CSV con múltiples iglesias vía
 *       {@link #registrarMasivo(MultipartFile)}. El proceso nunca se interrumpe:
 *       los errores se acumulan y se devuelven en el resumen.</li>
 * </ol>
 *
 * <p><strong>Flujo de aprobación:</strong> cualquier equipo autenticado puede aprobar
 * o rechazar iglesias con {@link #aprobar(Integer, IglesiaAprobacionRequest)}.</p>
 */
@Service
public class IglesiaService {

    private final IglesiaRepository iglesiaRepo;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Bean singleton gestionado por Spring.")
    public IglesiaService(IglesiaRepository iglesiaRepo) {
        this.iglesiaRepo = iglesiaRepo;
    }

    /**
     * Registra una nueva iglesia para el equipo autenticado.
     *
     * @param request datos de la iglesia
     * @return respuesta con la iglesia en estado PENDIENTE
     * @throws IllegalArgumentException si faltan campos obligatorios o la iglesia ya existe
     */
    @Transactional
    public IglesiaResponse registrar(IglesiaRequest request) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId = details.equipoId();

        validarCamposObligatorios(request);

        if (iglesiaRepo.existsByNombreAndEquipoIdAndTemporadaId(
                request.nombre(), equipoId, request.temporadaId())) {
            throw new IllegalArgumentException(
                    "Ya existe una iglesia con el nombre '" + request.nombre()
                    + "' registrada por este equipo en la temporada " + request.temporadaId() + ".");
        }

        Iglesia iglesia = mapearDesdeRequest(request, equipoId);
        iglesiaRepo.save(iglesia);
        return construirResponse(iglesia);
    }

    /**
     * Registra múltiples iglesias desde un archivo CSV.
     *
     * <p><strong>Formato CSV (13 columnas, primera fila = encabezado):</strong></p>
     * <pre>
     * nombre,denominacion,departamento,ciudad,direccion,pastor_nombre,pastor_celular,
     * pastor_correo,nombre_lider,celular_lider,correo_lider,temporada_id,cajas_solicitadas
     * </pre>
     *
     * @param archivo archivo CSV con los datos de las iglesias
     * @return resumen del proceso con conteos y descripción de errores por fila
     */
    @Transactional
    public CargaMasivaIglesiasResponse registrarMasivo(MultipartFile archivo) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId = details.equipoId();

        int procesadas = 0;
        int exitosas   = 0;
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String encabezado = reader.readLine(); // saltar encabezado
            if (encabezado == null) {
                throw new IllegalArgumentException("El archivo CSV está vacío.");
            }

            String linea;
            int numeroFila = 1;
            while ((linea = reader.readLine()) != null) {
                numeroFila++;
                procesadas++;
                try {
                    String[] cols = linea.split(",", -1);
                    if (cols.length < 13) {
                        errores.add("Fila " + numeroFila + ": se esperaban 13 columnas, se encontraron " + cols.length + ".");
                        continue;
                    }

                    String nombre      = cols[0].trim();
                    String denominacion = cols[1].trim();
                    String departamento = cols[2].trim();
                    String ciudad      = cols[3].trim();
                    String direccion   = cols[4].trim();
                    String pastorNombre   = cols[5].trim();
                    String pastorCelular  = cols[6].trim();
                    String pastorCorreo   = cols[7].trim();
                    String nombreLider    = cols[8].trim();
                    String celularLider   = cols[9].trim();
                    String correoLider    = cols[10].trim();
                    Integer temporadaId   = Integer.parseInt(cols[11].trim());
                    Integer cajasSolicitadas = cols[12].trim().isEmpty() ? null
                            : Integer.parseInt(cols[12].trim());

                    if (nombre.isEmpty() || departamento.isEmpty() || ciudad.isEmpty()) {
                        errores.add("Fila " + numeroFila + ": nombre, departamento y ciudad son obligatorios.");
                        continue;
                    }

                    if (iglesiaRepo.existsByNombreAndEquipoIdAndTemporadaId(nombre, equipoId, temporadaId)) {
                        errores.add("Fila " + numeroFila + ": ya existe la iglesia '" + nombre + "' en la temporada " + temporadaId + ".");
                        continue;
                    }

                    IglesiaRequest req = new IglesiaRequest(nombre, denominacion, departamento,
                            ciudad, direccion, pastorNombre, pastorCelular, pastorCorreo,
                            nombreLider, celularLider, correoLider, temporadaId, cajasSolicitadas);

                    Iglesia iglesia = mapearDesdeRequest(req, equipoId);
                    iglesiaRepo.save(iglesia);
                    exitosas++;

                } catch (NumberFormatException e) {
                    errores.add("Fila " + numeroFila + ": temporada_id o cajas_solicitadas no son números válidos.");
                } catch (Exception e) {
                    errores.add("Fila " + numeroFila + ": " + e.getMessage());
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Error al leer el archivo CSV: " + e.getMessage());
        }

        return new CargaMasivaIglesiasResponse(procesadas, exitosas, procesadas - exitosas, errores);
    }

    /**
     * Aprueba o rechaza la participación de una iglesia.
     *
     * <p>Cualquier equipo autenticado puede tomar esta decisión. El rechazo requiere
     * {@code motivoRechazo} obligatorio.</p>
     *
     * @param id      ID de la iglesia a gestionar
     * @param request decisión (APROBADA o RECHAZADA) y motivo opcional
     * @return respuesta actualizada de la iglesia
     * @throws IllegalArgumentException si la iglesia no existe, la decisión es inválida
     *                                  o falta el motivo al rechazar
     * @throws IllegalStateException    si la iglesia ya fue procesada anteriormente
     */
    @Transactional
    public IglesiaResponse aprobar(Integer id, IglesiaAprobacionRequest request) {
        obtenerDetails();

        Iglesia iglesia = iglesiaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Iglesia no encontrada con id: " + id));

        if (iglesia.getEstado() != EstadoIglesia.PENDIENTE) {
            throw new IllegalStateException(
                    "La iglesia ya fue procesada. Estado actual: " + iglesia.getEstado().name());
        }

        EstadoIglesia nuevoEstado;
        try {
            nuevoEstado = EstadoIglesia.valueOf(request.decision());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Decisión inválida: '" + request.decision()
                    + "'. Valores permitidos: APROBADA, RECHAZADA.");
        }

        if (nuevoEstado == EstadoIglesia.RECHAZADA) {
            if (request.motivoRechazo() == null || request.motivoRechazo().isBlank()) {
                throw new IllegalArgumentException(
                        "El campo 'motivoRechazo' es obligatorio al rechazar una iglesia.");
            }
            iglesia.setMotivoRechazo(request.motivoRechazo());
        }

        iglesia.setEstado(nuevoEstado);
        iglesiaRepo.save(iglesia);
        return construirResponse(iglesia);
    }

    /**
     * Lista las iglesias visibles para el usuario autenticado en una temporada.
     *
     * @param temporadaId ID de la temporada
     * @return lista de iglesias según jerarquía
     */
    public List<IglesiaResponse> listar(Integer temporadaId) {
        JwtAuthDetails details = obtenerDetails();
        Integer equipoId  = details.equipoId();
        String equipoTipo = details.equipoTipo();

        List<Iglesia> iglesias = switch (equipoTipo) {
            case "ENL"  -> iglesiaRepo.findByTemporadaId(temporadaId);
            case "ERLE" -> iglesiaRepo.findByErleClusterAndTemporada(equipoId, temporadaId);
            case "ERL"  -> iglesiaRepo.findByEquipoIdAndTemporadaId(equipoId, temporadaId);
            default -> throw new IllegalArgumentException(
                    "Tipo de equipo no reconocido: " + equipoTipo);
        };

        return iglesias.stream().map(this::construirResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private void validarCamposObligatorios(IglesiaRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la iglesia es obligatorio.");
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

    private Iglesia mapearDesdeRequest(IglesiaRequest request, Integer equipoId) {
        Iglesia iglesia = new Iglesia();
        iglesia.setNombre(request.nombre());
        iglesia.setDenominacion(request.denominacion());
        iglesia.setDepartamento(request.departamento());
        iglesia.setCiudad(request.ciudad());
        iglesia.setDireccion(request.direccion());
        iglesia.setPastorNombre(request.pastorNombre());
        iglesia.setPastorCelular(request.pastorCelular());
        iglesia.setPastorCorreo(request.pastorCorreo());
        iglesia.setNombreLider(request.nombreLider());
        iglesia.setCelularLider(request.celularLider());
        iglesia.setCorreoLider(request.correoLider());
        iglesia.setEquipoId(equipoId);
        iglesia.setTemporadaId(request.temporadaId());
        iglesia.setCajasSolicitadas(request.cajasSolicitadas());
        iglesia.setEstado(EstadoIglesia.PENDIENTE);
        iglesia.setFechaRegistro(LocalDateTime.now());
        return iglesia;
    }

    private IglesiaResponse construirResponse(Iglesia i) {
        return new IglesiaResponse(
                i.getId(), i.getNombre(), i.getDenominacion(), i.getDepartamento(),
                i.getCiudad(), i.getDireccion(), i.getPastorNombre(), i.getPastorCelular(),
                i.getPastorCorreo(), i.getNombreLider(), i.getCelularLider(), i.getCorreoLider(),
                i.getEquipoId(), i.getTemporadaId(), i.getCajasSolicitadas(),
                i.getEstado().name(), i.getMotivoRechazo(), i.getFechaRegistro()
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
