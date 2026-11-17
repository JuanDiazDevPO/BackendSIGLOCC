package com.siglocc.service;

import com.siglocc.dto.AnticipoRequest;
import com.siglocc.dto.AnticipoResponse;
import com.siglocc.dto.CambiarEstadoRequest;
import com.siglocc.dto.SaldosEquipoResponse;
import com.siglocc.entity.Equipo;
import com.siglocc.entity.EstadoSolicitud;
import com.siglocc.entity.Rol;
import com.siglocc.entity.SolicitudAnticipo;
import com.siglocc.entity.Temporada;
import com.siglocc.entity.TipoEquipo;
import com.siglocc.entity.TipoPresupuesto;
import com.siglocc.entity.Usuario;
import com.siglocc.entity.VistaControlSaldos;
import com.siglocc.entity.VistaControlSaldosId;
import com.siglocc.repository.EquipoRepository;
import com.siglocc.repository.SolicitudAnticipoRepository;
import com.siglocc.repository.TemporadaRepository;
import com.siglocc.repository.UsuarioRepository;
import com.siglocc.repository.VistaControlSaldosRepository;
import com.siglocc.security.IdentidadJwtException;
import com.siglocc.security.JwtAuthDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AnticipoService}.
 *
 * <p><strong>Nota técnica:</strong> {@code crearSolicitud} y
 * {@code cambiarEstado} registran callbacks de correo con
 * {@code TransactionSynchronizationManager.registerSynchronization(...)},
 * que lanza {@code IllegalStateException} si no hay sincronización de
 * transacción activa. En un test unitario puro (sin Spring real, sin
 * {@code @Transactional} de verdad) eso nunca está activo por defecto, así
 * que se activa manualmente en {@code @BeforeEach} — sin que esto ejecute
 * los correos de verdad, porque nunca se simula un commit real.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnticipoServiceTest {

    @Mock private SolicitudAnticipoRepository solicitudRepo;
    @Mock private VistaControlSaldosRepository saldosRepo;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TemporadaRepository temporadaRepository;
    @Mock private EquipoRepository equipoRepository;
    @Mock private EmailService emailService;
    @Mock private AnticipoDocumentoService documentoService;
    @Mock private StorageService storageService;

    private AnticipoService service;

    private static final Integer EQUIPO_ID = 8;
    private static final Integer TEMPORADA_ID = 1;

    @BeforeEach
    void setUp() {
        service = new AnticipoService(solicitudRepo, saldosRepo, usuarioRepository,
                temporadaRepository, equipoRepository, emailService, documentoService, storageService);
        // Necesario para que registerSynchronization(...) no explote fuera de una
        // transaccion real -- no ejecuta los callbacks, solo permite registrarlos.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoEmail(String email) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(email);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void autenticarComoEquipo(Integer equipoId, String equipoTipo) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getDetails()).thenReturn(new JwtAuthDetails(equipoId, equipoTipo));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** Autentica simultáneamente por email (para resolverUsuarioId) y por equipo/tipo (para JwtAuthDetails). */
    private void autenticarComoEmailYEquipo(String email, Integer equipoId, String equipoTipo) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(email);
        lenient().when(auth.getDetails()).thenReturn(new JwtAuthDetails(equipoId, equipoTipo));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Usuario usuarioCompleto(String email, TipoEquipo tipoEquipo) {
        Rol rol = new Rol();
        rol.setName("ERL_LIDER");
        Equipo equipo = new Equipo();
        equipo.setNombre("ERL Bogotá Centro");
        equipo.setTipo(tipoEquipo);

        Usuario u = new Usuario();
        u.setId(5);
        u.setName("Laura");
        u.setLastname("Gómez");
        u.setEmail(email);
        u.setRol(rol);
        u.setEquipo(equipo);
        // el id del equipo es de solo lectura via reflexion de JPA; para el test
        // se lee siempre por referencia al objeto `equipo`, no por su id real.
        return u;
    }

    private VistaControlSaldos saldosCon(BigDecimal saldoEntrenamiento, BigDecimal saldoMentoreo) {
        VistaControlSaldos saldos = mock(VistaControlSaldos.class);
        lenient().when(saldos.getSaldoEntrenamiento()).thenReturn(saldoEntrenamiento);
        lenient().when(saldos.getSaldoMentoreo()).thenReturn(saldoMentoreo);
        return saldos;
    }

    private Temporada temporadaActiva() {
        Temporada t = mock(Temporada.class);
        when(t.getId()).thenReturn(TEMPORADA_ID);
        return t;
    }

    private AnticipoRequest requestPara(TipoPresupuesto tipo, BigDecimal monto) {
        return new AnticipoRequest("Anticipo de prueba", "Descripción", monto, tipo,
                "Bogotá", "123", "Bancolombia", com.siglocc.entity.TipoCuenta.AHORROS,
                "0011", "Laura Gómez", "123");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearSolicitud — identidad y prerequisitos
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearSolicitud_usuarioAutenticadoNoEncontrado_lanzaIllegalStateException() {
        autenticarComoEmail("fantasma@siglocc.org");
        when(usuarioRepository.findByEmail("fantasma@siglocc.org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSolicitud(requestPara(TipoPresupuesto.ENTRENAMIENTO, BigDecimal.TEN)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Usuario autenticado no encontrado");
    }

    @Test
    void crearSolicitud_sinTemporadaActiva_lanzaIllegalStateException() {
        autenticarComoEmail("laura@siglocc.org");
        when(usuarioRepository.findByEmail("laura@siglocc.org"))
                .thenReturn(Optional.of(usuarioCompleto("laura@siglocc.org", TipoEquipo.ERL)));
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSolicitud(requestPara(TipoPresupuesto.ENTRENAMIENTO, BigDecimal.TEN)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("temporada activa");
    }

    @Test
    void crearSolicitud_sinPresupuestoConfigurado_lanzaIllegalArgumentException() {
        Usuario usuario = usuarioCompleto("laura@siglocc.org", TipoEquipo.ERL);
        autenticarComoEmail("laura@siglocc.org");
        when(usuarioRepository.findByEmail("laura@siglocc.org")).thenReturn(Optional.of(usuario));
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        when(saldosRepo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSolicitud(requestPara(TipoPresupuesto.ENTRENAMIENTO, BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró presupuesto");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearSolicitud — rechazo automático vs. siguiente estado, por bucket correcto
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearSolicitud_excedeSaldoEntrenamiento_seRechazaAutomaticamente() {
        Usuario usuario = usuarioCompleto("laura@siglocc.org", TipoEquipo.ERL);
        autenticarComoEmail("laura@siglocc.org");
        when(usuarioRepository.findByEmail("laura@siglocc.org")).thenReturn(Optional.of(usuario));
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        VistaControlSaldos saldos = saldosCon(new BigDecimal("100000"), new BigDecimal("999999"));
        when(saldosRepo.findById(any())).thenReturn(Optional.of(saldos));

        AnticipoResponse response = service.crearSolicitud(
                requestPara(TipoPresupuesto.ENTRENAMIENTO, new BigDecimal("500000")));

        assertThat(response.estado()).isEqualTo("RECHAZADO");
    }

    @Test
    void crearSolicitud_excedeSaldoMentoreo_seRechazaAutomaticamente() {
        Usuario usuario = usuarioCompleto("laura@siglocc.org", TipoEquipo.ERL);
        autenticarComoEmail("laura@siglocc.org");
        when(usuarioRepository.findByEmail("laura@siglocc.org")).thenReturn(Optional.of(usuario));
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        // saldo de entrenamiento sobra a proposito, para probar que SI selecciona
        // el bucket de mentoreo y no el de entrenamiento por error
        VistaControlSaldos saldos = saldosCon(new BigDecimal("999999"), new BigDecimal("50000"));
        when(saldosRepo.findById(any())).thenReturn(Optional.of(saldos));

        AnticipoResponse response = service.crearSolicitud(
                requestPara(TipoPresupuesto.MENTOREO, new BigDecimal("200000")));

        assertThat(response.estado()).isEqualTo("RECHAZADO");
    }

    @Test
    void crearSolicitud_equipoErl_quedaPendienteErle() {
        Usuario usuario = usuarioCompleto("laura@siglocc.org", TipoEquipo.ERL);
        autenticarComoEmail("laura@siglocc.org");
        when(usuarioRepository.findByEmail("laura@siglocc.org")).thenReturn(Optional.of(usuario));
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        VistaControlSaldos saldos = saldosCon(new BigDecimal("500000"), BigDecimal.ZERO);
        when(saldosRepo.findById(any())).thenReturn(Optional.of(saldos));
        lenient().when(usuarioRepository.findFirstByEquipo_IdAndRol_Name(any(), any())).thenReturn(Optional.empty());

        AnticipoResponse response = service.crearSolicitud(
                requestPara(TipoPresupuesto.ENTRENAMIENTO, new BigDecimal("200000")));

        assertThat(response.estado()).isEqualTo("PENDIENTE_ERLE");
    }

    @Test
    void crearSolicitud_equipoErle_saltaDirectoAPendienteEnl() {
        Usuario usuario = usuarioCompleto("laura@siglocc.org", TipoEquipo.ERLE);
        autenticarComoEmail("laura@siglocc.org");
        when(usuarioRepository.findByEmail("laura@siglocc.org")).thenReturn(Optional.of(usuario));
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        VistaControlSaldos saldos = saldosCon(BigDecimal.ZERO, new BigDecimal("300000"));
        when(saldosRepo.findById(any())).thenReturn(Optional.of(saldos));
        lenient().when(usuarioRepository.findFirstByRol_Name("ENL_RECURSOS")).thenReturn(Optional.empty());

        AnticipoResponse response = service.crearSolicitud(
                requestPara(TipoPresupuesto.MENTOREO, new BigDecimal("150000")));

        assertThat(response.estado()).isEqualTo("PENDIENTE_ENL");
    }

    @Test
    void crearSolicitud_equipoEnl_quedaAutoaprobada() {
        Usuario usuario = usuarioCompleto("lider@siglocc.org", TipoEquipo.ENL);
        autenticarComoEmail("lider@siglocc.org");
        when(usuarioRepository.findByEmail("lider@siglocc.org")).thenReturn(Optional.of(usuario));
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        VistaControlSaldos saldos = saldosCon(new BigDecimal("500000"), BigDecimal.ZERO);
        when(saldosRepo.findById(any())).thenReturn(Optional.of(saldos));

        AnticipoResponse response = service.crearSolicitud(
                requestPara(TipoPresupuesto.ENTRENAMIENTO, new BigDecimal("200000")));

        assertThat(response.estado()).isEqualTo("APROBADO");
    }

    // ─────────────────────────────────────────────────────────────────────
    // cambiarEstado — identidad y validación de transición
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void cambiarEstado_sinIdentidadJwt_lanzaIdentidadJwtException() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getDetails()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> service.cambiarEstado(1, new CambiarEstadoRequest("APROBADO", null)))
                .isInstanceOf(IdentidadJwtException.class);
    }

    @Test
    void cambiarEstado_solicitudNoEncontrada_lanzaIllegalArgumentException() {
        autenticarComoEmailYEquipo("enl@siglocc.org", 1, "ENL");
        when(solicitudRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(99, new CambiarEstadoRequest("APROBADO", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cambiarEstado_estadoDestinoInvalido_lanzaIllegalArgumentException() {
        autenticarComoEmailYEquipo("enl@siglocc.org", 1, "ENL");
        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ENL);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.cambiarEstado(1, new CambiarEstadoRequest("NO_EXISTE", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado no válido");
    }

    @Test
    void cambiarEstado_erlNoPuedeCambiarEstado_lanzaIllegalStateException() {
        autenticarComoEmailYEquipo("erl@siglocc.org", 8, "ERL");
        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ERLE);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.cambiarEstado(1, new CambiarEstadoRequest("PENDIENTE_ENL", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene permisos");
    }

    @Test
    void cambiarEstado_erleFueraDeSuCluster_lanzaIllegalStateException() {
        autenticarComoEmailYEquipo("erle@siglocc.org", 2, "ERLE");
        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ERLE);
        solicitud.setEquipoId(8);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));

        Equipo equipoSolicitante = new Equipo();
        equipoSolicitante.setTipo(TipoEquipo.ERL);
        // erleId real del equipo es 999, distinto del ERLE autenticado (2)
        setErleIdViaReflexion(equipoSolicitante, 999);
        when(equipoRepository.findById(8)).thenReturn(Optional.of(equipoSolicitante));

        assertThatThrownBy(() -> service.cambiarEstado(1, new CambiarEstadoRequest("PENDIENTE_ENL", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no pertenece a su clúster");
    }

    @Test
    void cambiarEstado_erleApruebaSuPropioCluster_pasaAPendienteEnl() {
        autenticarComoEmailYEquipo("erle@siglocc.org", 2, "ERLE");
        when(usuarioRepository.findByEmail("erle@siglocc.org")).thenReturn(Optional.empty());

        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ERLE);
        solicitud.setEquipoId(8);
        solicitud.setUsuarioId(5);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));

        Equipo equipoSolicitante = new Equipo();
        equipoSolicitante.setTipo(TipoEquipo.ERL);
        setErleIdViaReflexion(equipoSolicitante, 2);
        when(equipoRepository.findById(8)).thenReturn(Optional.of(equipoSolicitante));
        lenient().when(usuarioRepository.findFirstByRol_Name("ENL_RECURSOS")).thenReturn(Optional.empty());
        lenient().when(usuarioRepository.findById(5)).thenReturn(Optional.empty());

        AnticipoResponse response = service.cambiarEstado(1, new CambiarEstadoRequest("PENDIENTE_ENL", null));

        assertThat(response.estado()).isEqualTo("PENDIENTE_ENL");
        assertThat(solicitud.getAprobadorErleId()).isNull(); // usuario no encontrado por email en este test
        assertThat(solicitud.getFechaAprobacionErle()).isNotNull();
    }

    @Test
    void cambiarEstado_rechazoSinObservaciones_lanzaIllegalArgumentException() {
        autenticarComoEmailYEquipo("enl@siglocc.org", 1, "ENL");
        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ENL);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.cambiarEstado(1, new CambiarEstadoRequest("RECHAZADO", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observaciones");
    }

    @Test
    void cambiarEstado_enlRechazaConObservaciones_quedaRechazada() {
        autenticarComoEmailYEquipo("enl@siglocc.org", 1, "ENL");
        when(usuarioRepository.findByEmail("enl@siglocc.org")).thenReturn(Optional.empty());

        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ENL);
        solicitud.setUsuarioId(5);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));
        lenient().when(usuarioRepository.findById(5)).thenReturn(Optional.empty());

        AnticipoResponse response = service.cambiarEstado(
                1, new CambiarEstadoRequest("RECHAZADO", "Soporte bancario ilegible"));

        assertThat(response.estado()).isEqualTo("RECHAZADO");
        assertThat(solicitud.getMotivoRechazo()).isEqualTo("Soporte bancario ilegible");
        assertThat(solicitud.getFechaRechazo()).isNotNull();
    }

    @Test
    void cambiarEstado_enlAprueba_registraFechaYAprobador() {
        autenticarComoEmailYEquipo("enl@siglocc.org", 1, "ENL");
        when(usuarioRepository.findByEmail("enl@siglocc.org")).thenReturn(Optional.empty());

        SolicitudAnticipo solicitud = new SolicitudAnticipo();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_ENL);
        solicitud.setUsuarioId(5);
        when(solicitudRepo.findById(1)).thenReturn(Optional.of(solicitud));
        lenient().when(usuarioRepository.findById(5)).thenReturn(Optional.empty());

        AnticipoResponse response = service.cambiarEstado(1, new CambiarEstadoRequest("APROBADO", null));

        assertThat(response.estado()).isEqualTo("APROBADO");
        assertThat(solicitud.getFechaAprobacionFinal()).isNotNull();
    }

    /** Asigna erleId a un Equipo saltando el insertable=false/updatable=false de JPA, solo para tests. */
    private void setErleIdViaReflexion(Equipo equipo, Integer erleId) {
        try {
            var campo = Equipo.class.getDeclaredField("erleId");
            campo.setAccessible(true);
            campo.set(equipo, erleId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // consultarMisSaldos
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void consultarMisSaldos_sinIdentidadJwt_lanzaIdentidadJwtException() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getDetails()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> service.consultarMisSaldos())
                .isInstanceOf(IdentidadJwtException.class);
    }

    @Test
    void consultarMisSaldos_sinTemporadaActiva_lanzaIllegalStateException() {
        autenticarComoEquipo(EQUIPO_ID, "ERL");
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarMisSaldos())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void consultarMisSaldos_sinPresupuestoConfigurado_lanzaNoSuchElementException() {
        autenticarComoEquipo(EQUIPO_ID, "ERL");
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarMisSaldos())
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void consultarMisSaldos_conDatos_devuelveRubrosCorrectos() {
        autenticarComoEquipo(EQUIPO_ID, "ERL");
        Temporada temporada = temporadaActiva();
        when(temporadaRepository.findByEsActualTrue()).thenReturn(Optional.of(temporada));

        VistaControlSaldos saldos = mock(VistaControlSaldos.class);
        when(saldos.getPresupuestoEntrenamiento()).thenReturn(new BigDecimal("1000000"));
        when(saldos.getEjecutadoEntrenamiento()).thenReturn(new BigDecimal("300000"));
        when(saldos.getSaldoEntrenamiento()).thenReturn(new BigDecimal("700000"));
        when(saldos.getPresupuestoMentoreo()).thenReturn(new BigDecimal("400000"));
        when(saldos.getEjecutadoMentoreo()).thenReturn(BigDecimal.ZERO);
        when(saldos.getSaldoMentoreo()).thenReturn(new BigDecimal("400000"));
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.of(saldos));

        Equipo equipo = new Equipo();
        equipo.setNombre("ERL Bogotá Centro");
        when(equipoRepository.findById(EQUIPO_ID)).thenReturn(Optional.of(equipo));

        SaldosEquipoResponse response = service.consultarMisSaldos();

        assertThat(response.equipoNombre()).isEqualTo("ERL Bogotá Centro");
        assertThat(response.entrenamiento().disponible()).isEqualByComparingTo("700000");
        assertThat(response.mentoreo().disponible()).isEqualByComparingTo("400000");
    }
}
