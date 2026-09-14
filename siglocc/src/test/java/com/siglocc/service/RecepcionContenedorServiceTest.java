package com.siglocc.service;

import com.siglocc.dto.DetalleRecepcionRequest;
import com.siglocc.dto.RecepcionContenedorRequest;
import com.siglocc.dto.RecepcionContenedorResponse;
import com.siglocc.entity.FotoContenedor;
import com.siglocc.entity.RecepcionContenedor;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.DetalleRecepcionContenedorRepository;
import com.siglocc.repository.FotoContenedorRepository;
import com.siglocc.repository.RecepcionContenedorRepository;
import com.siglocc.repository.TipoItemRepository;
import com.siglocc.repository.UsuarioRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link RecepcionContenedorService}.
 *
 * <p>Cubre las validaciones de {@code registrar} (incluida la regla
 * "exactamente uno de categoriaCajaId/tipoItemId" que comparte con
 * {@code AsignacionService} y {@code EntregaService}), el cálculo automático
 * de {@code totalCajasRecibidas}, el límite de 4 fotos, y el despacho por
 * jerarquía de {@code listar}.</p>
 */
@ExtendWith(MockitoExtension.class)
class RecepcionContenedorServiceTest {

    @Mock private RecepcionContenedorRepository recepcionRepo;
    @Mock private DetalleRecepcionContenedorRepository detalleRepo;
    @Mock private FotoContenedorRepository fotoRepo;
    @Mock private CategoriaCajaRepository categoriaCajaRepo;
    @Mock private TipoItemRepository tipoItemRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private StorageService storageService;

    private RecepcionContenedorService service;

    private static final Integer EQUIPO_ID = 24;
    private static final Integer TEMPORADA_ID = 1;

    @BeforeEach
    void setUp() {
        service = new RecepcionContenedorService(recepcionRepo, detalleRepo, fotoRepo,
                categoriaCajaRepo, tipoItemRepo, usuarioRepo, storageService);
        // Estas dos se consultan en construirResponse() para enriquecer los detalles;
        // no todos los tests devuelven un response con detalles, así que van lenient.
        lenient().when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        lenient().when(tipoItemRepo.findAll()).thenReturn(List.of());
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Integer equipoId, String equipoTipo) {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(new JwtAuthDetails(equipoId, equipoTipo));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private RecepcionContenedorRequest requestValido(List<DetalleRecepcionRequest> detalles) {
        return new RecepcionContenedorRequest(
                "CONT-001", 7, TEMPORADA_ID, LocalDate.of(2026, 3, 1), "Sin novedad", detalles);
    }

    // ─────────────────────────────────────────────────────────────────────
    // registrar — validaciones de forma
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void registrar_numeroContenedorNulo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = new RecepcionContenedorRequest(
                null, 7, TEMPORADA_ID, LocalDate.now(), null, List.of());

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("número de contenedor");
    }

    @Test
    void registrar_puntoEntregaIdNulo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = new RecepcionContenedorRequest(
                "CONT-001", null, TEMPORADA_ID, LocalDate.now(), null, List.of());

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("puntoEntregaId");
    }

    @Test
    void registrar_temporadaIdNula_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = new RecepcionContenedorRequest(
                "CONT-001", 7, null, LocalDate.now(), null, List.of());

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temporadaId");
    }

    @Test
    void registrar_fechaLlegadaNula_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = new RecepcionContenedorRequest(
                "CONT-001", 7, TEMPORADA_ID, null, null, List.of());

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha de llegada");
    }

    @Test
    void registrar_sinDetalles_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(List.of());

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un detalle");
    }

    // ─────────────────────────────────────────────────────────────────────
    // registrar — regla "exactamente uno" por detalle
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void registrar_detalleConCategoriaYTipoItem_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(
                List.of(new DetalleRecepcionRequest(1, 3, 10)));

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void registrar_detalleSinCategoriaNiTipoItem_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(
                List.of(new DetalleRecepcionRequest(null, null, 10)));

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void registrar_cantidadNula_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(
                List.of(new DetalleRecepcionRequest(1, null, null)));

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    void registrar_cantidadCero_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(
                List.of(new DetalleRecepcionRequest(1, null, 0)));

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor que cero");
    }

    // ─────────────────────────────────────────────────────────────────────
    // registrar — cálculo de totalCajasRecibidas (regla de negocio central)
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void registrar_sumaSoloLasCantidadesDeCategoriaCaja_ignorandoLiteratura() {
        autenticarComo(EQUIPO_ID, "ERL");
        // 2 filas de cajas (10 + 15 = 25) + 1 fila de literatura (100, no debe contar)
        RecepcionContenedorRequest request = requestValido(List.of(
                new DetalleRecepcionRequest(1, null, 10),
                new DetalleRecepcionRequest(2, null, 15),
                new DetalleRecepcionRequest(null, 3, 100)
        ));

        RecepcionContenedorResponse response = service.registrar(request);

        assertThat(response.totalCajasRecibidas())
                .as("solo debe sumar las filas de categoriaCajaId, la literatura no cuenta como caja")
                .isEqualTo(25);
    }

    @Test
    void registrar_soloLiteratura_totalCajasRecibidasEsCero() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(
                List.of(new DetalleRecepcionRequest(null, 3, 50)));

        RecepcionContenedorResponse response = service.registrar(request);

        assertThat(response.totalCajasRecibidas()).isZero();
    }

    @Test
    void registrar_exitoso_asignaEquipoIdDesdeJwt() {
        autenticarComo(EQUIPO_ID, "ERL");
        RecepcionContenedorRequest request = requestValido(
                List.of(new DetalleRecepcionRequest(1, null, 10)));

        RecepcionContenedorResponse response = service.registrar(request);

        assertThat(response.equipoId()).isEqualTo(EQUIPO_ID);
        assertThat(response.numeroContenedor()).isEqualTo("CONT-001");
    }

    // ─────────────────────────────────────────────────────────────────────
    // subirDocumento
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void subirDocumento_recepcionNoExiste_lanzaIllegalArgumentException() {
        when(recepcionRepo.findById(99)).thenReturn(Optional.empty());
        MultipartFile archivo = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.subirDocumento(99, "ABC", archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void subirDocumento_tipoInvalido_lanzaIllegalArgumentException() {
        RecepcionContenedor recepcion = new RecepcionContenedor();
        recepcion.setId(1);
        when(recepcionRepo.findById(1)).thenReturn(Optional.of(recepcion));
        MultipartFile archivo = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.subirDocumento(1, "FACTURA", archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de documento inválido");
    }

    @Test
    void subirDocumento_tipoTransportadora_asignaListaTransportadoraUrl() {
        RecepcionContenedor recepcion = new RecepcionContenedor();
        recepcion.setId(1);
        when(recepcionRepo.findById(1)).thenReturn(Optional.of(recepcion));
        when(detalleRepo.findByRecepcionId(1)).thenReturn(List.of());
        MultipartFile archivo = mock(MultipartFile.class);
        when(storageService.almacenarDocumentoLogistica(archivo, "TRANSPORTADORA", 1))
                .thenReturn("lista.pdf");

        RecepcionContenedorResponse response = service.subirDocumento(1, "TRANSPORTADORA", archivo);

        assertThat(response.listaTransportadoraUrl()).isEqualTo("lista.pdf");
        assertThat(response.documentoAbcUrl()).isNull();
    }

    @Test
    void subirDocumento_tipoAbcMinusculas_asignaDocumentoAbcUrl() {
        RecepcionContenedor recepcion = new RecepcionContenedor();
        recepcion.setId(1);
        when(recepcionRepo.findById(1)).thenReturn(Optional.of(recepcion));
        when(detalleRepo.findByRecepcionId(1)).thenReturn(List.of());
        MultipartFile archivo = mock(MultipartFile.class);
        when(storageService.almacenarDocumentoLogistica(archivo, "abc", 1))
                .thenReturn("formato_abc.pdf");

        RecepcionContenedorResponse response = service.subirDocumento(1, "abc", archivo);

        assertThat(response.documentoAbcUrl())
                .as("el tipo se valida sin distinguir mayusculas/minusculas")
                .isEqualTo("formato_abc.pdf");
        assertThat(response.listaTransportadoraUrl()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────
    // subirFoto — límite de 4 fotos
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void subirFoto_recepcionNoExiste_lanzaIllegalArgumentException() {
        when(recepcionRepo.existsById(99)).thenReturn(false);
        MultipartFile foto = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.subirFoto(99, foto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void subirFoto_yaTiene4Fotos_lanzaIllegalStateException() {
        when(recepcionRepo.existsById(1)).thenReturn(true);
        when(fotoRepo.countByRecepcionId(1)).thenReturn(4L);
        MultipartFile foto = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.subirFoto(1, foto, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("máximo de 4");
    }

    @Test
    void subirFoto_esLaTercera_calculaOrdenComoCuatro() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("logistica@siglocc.org");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(recepcionRepo.existsById(1)).thenReturn(true);
        when(fotoRepo.countByRecepcionId(1)).thenReturn(3L);
        MultipartFile foto = mock(MultipartFile.class);
        when(storageService.almacenarFotoLogistica(eq(foto), eq("contenedor"), eq(1), eq(4)))
                .thenReturn("foto4.jpg");
        when(usuarioRepo.findByEmail("logistica@siglocc.org")).thenReturn(Optional.empty());

        String url = service.subirFoto(1, foto, "Puerta trasera");

        assertThat(url).isEqualTo("foto4.jpg");
        verify(storageService).almacenarFotoLogistica(foto, "contenedor", 1, 4);
    }

    // ─────────────────────────────────────────────────────────────────────
    // listar — despacho por jerarquía
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void listar_comoENL_consultaFindByTemporadaId() {
        autenticarComo(1, "ENL");
        when(recepcionRepo.findByTemporadaId(TEMPORADA_ID)).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(recepcionRepo).findByTemporadaId(TEMPORADA_ID);
        verify(recepcionRepo, never()).findByErleClusterAndTemporada(anyInt(), anyInt());
        verify(recepcionRepo, never()).findByEquipoIdAndTemporadaId(anyInt(), anyInt());
    }

    @Test
    void listar_comoERLE_consultaFindByErleClusterAndTemporada() {
        autenticarComo(2, "ERLE");
        when(recepcionRepo.findByErleClusterAndTemporada(2, TEMPORADA_ID)).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(recepcionRepo).findByErleClusterAndTemporada(2, TEMPORADA_ID);
    }

    @Test
    void listar_comoERL_consultaFindByEquipoIdAndTemporadaId() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(recepcionRepo.findByEquipoIdAndTemporadaId(EQUIPO_ID, TEMPORADA_ID)).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(recepcionRepo).findByEquipoIdAndTemporadaId(EQUIPO_ID, TEMPORADA_ID);
    }

    @Test
    void listar_tipoEquipoNoReconocido_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "SUPERVISOR");

        assertThatThrownBy(() -> service.listar(TEMPORADA_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }
}
