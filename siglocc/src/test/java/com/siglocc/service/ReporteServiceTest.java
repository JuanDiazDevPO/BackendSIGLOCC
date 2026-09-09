package com.siglocc.service;

import com.siglocc.dto.CambiarEstadoRequest;
import com.siglocc.dto.ReporteDetalleRequest;
import com.siglocc.dto.ReporteRequest;
import com.siglocc.dto.ReporteResponse;
import com.siglocc.entity.EstadoReporte;
import com.siglocc.entity.Equipo;
import com.siglocc.entity.FamiliaCategoria;
import com.siglocc.entity.ReporteCategoria;
import com.siglocc.entity.ReporteMensual;
import com.siglocc.entity.Usuario;
import com.siglocc.entity.VistaControlSaldos;
import com.siglocc.entity.VistaControlSaldosId;
import com.siglocc.repository.EquipoRepository;
import com.siglocc.repository.ReporteCategoriaRepository;
import com.siglocc.repository.ReporteDetalleRepository;
import com.siglocc.repository.ReporteMensualRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ReporteService}, enfocadas en la lógica de
 * negocio más sensible: validación de categorías por rol, techo
 * presupuestal, y las transiciones de estado del flujo de aprobación.
 *
 * <p>Cada test autentica explícitamente el equipo/rol que necesita (no hay
 * un default silencioso en {@code @BeforeEach}) para que quede claro, leyendo
 * el test, bajo qué identidad corre cada escenario.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private ReporteMensualRepository reporteRepo;
    @Mock private ReporteDetalleRepository detalleRepo;
    @Mock private ReporteCategoriaRepository categoriaRepo;
    @Mock private VistaControlSaldosRepository saldosRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private EquipoRepository equipoRepo;
    @Mock private StorageService storageService;

    private ReporteService service;

    private static final Integer EQUIPO_ID = 8;
    private static final Integer TEMPORADA_ID = 1;

    @BeforeEach
    void setUp() {
        service = new ReporteService(reporteRepo, detalleRepo, categoriaRepo,
                saldosRepo, usuarioRepo, equipoRepo, storageService);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    /** Autentica el hilo de prueba con un equipoId/equipoTipo dados. */
    private void autenticarComo(Integer equipoId, String equipoTipo) {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(new JwtAuthDetails(equipoId, equipoTipo));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ReporteCategoria categoria(String codigo, FamiliaCategoria familia) {
        ReporteCategoria c = new ReporteCategoria();
        c.setCodigo(codigo);
        c.setFamilia(familia);
        c.setNombreLargo("Categoria " + codigo);
        return c;
    }

    /**
     * Mockea una fila de {@code vista_control_saldos_enl}. Se construye ANTES
     * de usarla en un {@code when(...).thenReturn(...)} externo -- anidar un
     * segundo {@code when(mock.metodo())} dentro de los argumentos de otro
     * confunde el tracking interno de Mockito (UnfinishedStubbingException).
     */
    private VistaControlSaldos saldosCon(BigDecimal saldoEntrenamiento, BigDecimal saldoMentoreo) {
        VistaControlSaldos saldos = mock(VistaControlSaldos.class);
        when(saldos.getSaldoEntrenamiento()).thenReturn(saldoEntrenamiento);
        when(saldos.getSaldoMentoreo()).thenReturn(saldoMentoreo);
        return saldos;
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearReporte — identidad y validaciones de forma
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearReporte_sinIdentidadJwt_lanzaIdentidadJwtException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026, List.of());

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IdentidadJwtException.class);
    }

    @Test
    void crearReporte_conMesCero_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 0, 2026, List.of());

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mes");
    }

    @Test
    void crearReporte_conMesTrece_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 13, 2026, List.of());

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mes");
    }

    @Test
    void crearReporte_duplicado_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(reporteRepo.existsByEquipoIdAndTemporadaIdAndMesAndAnio(EQUIPO_ID, TEMPORADA_ID, 3, 2026))
                .thenReturn(true);
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026, List.of());

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearReporte — validación de categorías y familia por rol
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearReporte_conCategoriaInexistente_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(categoriaRepo.findById("E-99")).thenReturn(Optional.empty());
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("E-99", new BigDecimal("10000"))));

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void crearReporte_erlUsandoCategoriaFueraDeFamiliaE_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(categoriaRepo.findById("M-1")).thenReturn(Optional.of(categoria("M-1", FamiliaCategoria.M)));
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("M-1", new BigDecimal("10000"))));

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("familia 'E'");
    }

    @Test
    void crearReporte_erlUsandoCategoriaDeFamiliaE_esPermitido() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(categoriaRepo.findById("E-1")).thenReturn(Optional.of(categoria("E-1", FamiliaCategoria.E)));
        VistaControlSaldos saldos = saldosCon(new BigDecimal("500000"), null);
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.of(saldos));

        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("E-1", new BigDecimal("10000"))));

        assertThatCode(() -> service.crearReporte(request)).doesNotThrowAnyException();
    }

    @Test
    void crearReporte_conMontoNegativo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(categoriaRepo.findById("E-1")).thenReturn(Optional.of(categoria("E-1", FamiliaCategoria.E)));
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("E-1", new BigDecimal("-1"))));

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor o igual a cero");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearReporte — techo presupuestal
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearReporte_sinPresupuestoConfigurado_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(categoriaRepo.findById("E-1")).thenReturn(Optional.of(categoria("E-1", FamiliaCategoria.E)));
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.empty());
        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("E-1", new BigDecimal("10000"))));

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("presupuesto configurado");
    }

    @Test
    void crearReporte_excedeSaldoEntrenamiento_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(categoriaRepo.findById("E-1")).thenReturn(Optional.of(categoria("E-1", FamiliaCategoria.E)));
        VistaControlSaldos saldos = saldosCon(new BigDecimal("100000"), BigDecimal.ZERO);
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.of(saldos));

        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("E-1", new BigDecimal("500000"))));

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entrenamiento")
                .hasMessageContaining("supera");
    }

    @Test
    void crearReporte_excedeSaldoMentoreo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ENL");
        when(categoriaRepo.findById("M-1")).thenReturn(Optional.of(categoria("M-1", FamiliaCategoria.M)));
        VistaControlSaldos saldos = saldosCon(BigDecimal.ZERO, new BigDecimal("50000"));
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.of(saldos));

        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026,
                List.of(new ReporteDetalleRequest("M-1", new BigDecimal("200000"))));

        assertThatThrownBy(() -> service.crearReporte(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mentoría");
    }

    @Test
    void crearReporte_dentroDelSaldo_creaExitosamenteConMontoTotalCorrecto() {
        autenticarComo(EQUIPO_ID, "ENL");
        when(categoriaRepo.findById("E-1")).thenReturn(Optional.of(categoria("E-1", FamiliaCategoria.E)));
        when(categoriaRepo.findById("M-1")).thenReturn(Optional.of(categoria("M-1", FamiliaCategoria.M)));
        VistaControlSaldos saldos = saldosCon(new BigDecimal("1000000"), new BigDecimal("1000000"));
        when(saldosRepo.findById(new VistaControlSaldosId(EQUIPO_ID, TEMPORADA_ID)))
                .thenReturn(Optional.of(saldos));
        Equipo equipo = new Equipo();
        equipo.setNombre("Equipo Nacional de Liderazgo");
        when(equipoRepo.findById(EQUIPO_ID)).thenReturn(Optional.of(equipo));

        ReporteRequest request = new ReporteRequest(TEMPORADA_ID, 3, 2026, List.of(
                new ReporteDetalleRequest("E-1", new BigDecimal("300000")),
                new ReporteDetalleRequest("M-1", new BigDecimal("150000"))
        ));

        ReporteResponse response = service.crearReporte(request);

        assertThat(response.estado()).isEqualTo("BORRADOR");
        assertThat(response.equipoId()).isEqualTo(EQUIPO_ID);
        assertThat(response.nombreEquipo()).isEqualTo("Equipo Nacional de Liderazgo");
        assertThat(response.montoTotal()).isEqualByComparingTo("450000");
        assertThat(response.detalles()).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────────────────
    // cambiarEstado — transiciones del flujo de aprobación
    // ─────────────────────────────────────────────────────────────────────

    private ReporteMensual reporteEnEstado(EstadoReporte estado) {
        ReporteMensual r = new ReporteMensual();
        r.setEquipoId(EQUIPO_ID);
        r.setTemporadaId(TEMPORADA_ID);
        r.setMes(3);
        r.setAnio(2026);
        r.setEstado(estado);
        return r;
    }

    @Test
    void cambiarEstado_erleFueraDePendienteErle_lanzaIllegalStateException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporteEnEstado(EstadoReporte.BORRADOR)));
        CambiarEstadoRequest request = new CambiarEstadoRequest("PENDIENTE_ENL", null);

        assertThatThrownBy(() -> service.cambiarEstado(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDIENTE_ERLE");
    }

    @Test
    void cambiarEstado_erleIntentaSaltarAAprobado_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporteEnEstado(EstadoReporte.PENDIENTE_ERLE)));
        CambiarEstadoRequest request = new CambiarEstadoRequest("APROBADO", null);

        assertThatThrownBy(() -> service.cambiarEstado(1, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cambiarEstado_rechazoSinObservaciones_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporteEnEstado(EstadoReporte.PENDIENTE_ERLE)));
        CambiarEstadoRequest request = new CambiarEstadoRequest("RECHAZADO", "  ");

        assertThatThrownBy(() -> service.cambiarEstado(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observaciones");
    }

    @Test
    void cambiarEstado_estadoDestinoInvalido_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERLE");
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporteEnEstado(EstadoReporte.PENDIENTE_ERLE)));
        CambiarEstadoRequest request = new CambiarEstadoRequest("NO_EXISTE", null);

        assertThatThrownBy(() -> service.cambiarEstado(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado no válido");
    }

    @Test
    void cambiarEstado_enlApruebaCorrectamente_registraFechaYAprobador() {
        autenticarComo(EQUIPO_ID, "ENL");
        ReporteMensual reporte = reporteEnEstado(EstadoReporte.PENDIENTE_ENL);
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporte));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        when(auth.getName()).thenReturn("aprobador@siglocc.org");
        Usuario aprobador = new Usuario();
        aprobador.setId(77);
        when(usuarioRepo.findByEmail("aprobador@siglocc.org")).thenReturn(Optional.of(aprobador));

        CambiarEstadoRequest request = new CambiarEstadoRequest("APROBADO", null);
        ReporteResponse response = service.cambiarEstado(1, request);

        assertThat(response.estado()).isEqualTo("APROBADO");
        assertThat(response.fechaAprobacionFinal()).isNotNull();
        assertThat(response.aprobadorEnlId()).isEqualTo(77);
    }

    // ─────────────────────────────────────────────────────────────────────
    // subirSoporte — control de acceso y estado previo
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void subirSoporte_equipoDistintoAlDueno_lanzaIllegalStateException() {
        autenticarComo(99, "ERL"); // no es el dueño del reporte (EQUIPO_ID = 8)
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporteEnEstado(EstadoReporte.BORRADOR)));
        MultipartFile archivo = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.subirSoporte(1, archivo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("propietario");
    }

    @Test
    void subirSoporte_reporteNoEstaEnBorrador_lanzaIllegalStateException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporteEnEstado(EstadoReporte.PENDIENTE_ERLE)));
        MultipartFile archivo = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.subirSoporte(1, archivo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BORRADOR");
    }

    // ─────────────────────────────────────────────────────────────────────
    // subirSoporte — el siguiente estado depende del tipo de equipo dueño
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void subirSoporte_comoERL_avanzaAPendienteErle() {
        autenticarComo(EQUIPO_ID, "ERL");
        ReporteMensual reporte = reporteEnEstado(EstadoReporte.BORRADOR);
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporte));
        MultipartFile archivo = mock(MultipartFile.class);
        when(storageService.almacenarSoporte(archivo, EQUIPO_ID, 3, 2026)).thenReturn("SOPORTE_EQ8_MES3_2026.pdf");

        ReporteResponse response = service.subirSoporte(1, archivo);

        assertThat(response.estado()).isEqualTo("PENDIENTE_ERLE");
        assertThat(response.fechaAprobacionFinal()).isNull();
        assertThat(response.aprobadorEnlId()).isNull();
    }

    @Test
    void subirSoporte_comoERLE_saltaAutoaprobacionYVaDirectoAPendienteEnl() {
        autenticarComo(EQUIPO_ID, "ERLE");
        ReporteMensual reporte = reporteEnEstado(EstadoReporte.BORRADOR);
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporte));
        MultipartFile archivo = mock(MultipartFile.class);
        when(storageService.almacenarSoporte(archivo, EQUIPO_ID, 3, 2026)).thenReturn("SOPORTE_EQ8_MES3_2026.pdf");

        ReporteResponse response = service.subirSoporte(1, archivo);

        assertThat(response.estado())
                .as("un ERLE no se autoaprueba: debe ir directo a PENDIENTE_ENL, nunca pasar por PENDIENTE_ERLE")
                .isEqualTo("PENDIENTE_ENL");
        assertThat(response.aprobadorEnlId()).isNull();
    }

    @Test
    void subirSoporte_comoENL_quedaAprobadoDeInmediatoConAprobadorYFecha() {
        autenticarComo(EQUIPO_ID, "ENL");
        ReporteMensual reporte = reporteEnEstado(EstadoReporte.BORRADOR);
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporte));
        MultipartFile archivo = mock(MultipartFile.class);
        when(storageService.almacenarSoporte(archivo, EQUIPO_ID, 3, 2026)).thenReturn("SOPORTE_EQ8_MES3_2026.pdf");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        when(auth.getName()).thenReturn("enl@siglocc.org");
        Usuario usuarioEnl = new Usuario();
        usuarioEnl.setId(5);
        when(usuarioRepo.findByEmail("enl@siglocc.org")).thenReturn(Optional.of(usuarioEnl));

        ReporteResponse response = service.subirSoporte(1, archivo);

        assertThat(response.estado())
                .as("el ENL no tiene a nadie por encima que lo apruebe: queda APROBADO al subir el soporte")
                .isEqualTo("APROBADO");
        assertThat(response.fechaAprobacionFinal()).isNotNull();
        assertThat(response.aprobadorEnlId())
                .as("se autoconfirma con el propio usuario ENL que subió el soporte")
                .isEqualTo(5);
    }

    @Test
    void subirSoporte_tipoEquipoNoReconocido_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "SUPERVISOR");
        ReporteMensual reporte = reporteEnEstado(EstadoReporte.BORRADOR);
        when(reporteRepo.findById(1)).thenReturn(Optional.of(reporte));
        MultipartFile archivo = mock(MultipartFile.class);
        when(storageService.almacenarSoporte(archivo, EQUIPO_ID, 3, 2026)).thenReturn("SOPORTE_EQ8_MES3_2026.pdf");

        assertThatThrownBy(() -> service.subirSoporte(1, archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }
}
