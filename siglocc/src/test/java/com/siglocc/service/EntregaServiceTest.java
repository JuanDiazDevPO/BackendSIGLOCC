package com.siglocc.service;

import com.siglocc.dto.EntregaDetalleRequest;
import com.siglocc.dto.EntregaRequest;
import com.siglocc.dto.EntregaResponse;
import com.siglocc.entity.CategoriaCaja;
import com.siglocc.entity.EntregaIglesia;
import com.siglocc.entity.EstadoEntrega;
import com.siglocc.entity.EstadoIglesia;
import com.siglocc.entity.Iglesia;
import com.siglocc.entity.TipoItem;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.DetalleEntregaIglesiaRepository;
import com.siglocc.repository.EntregaIglesiaRepository;
import com.siglocc.repository.FotoEntregaIglesiaRepository;
import com.siglocc.repository.FotoEntregaNinosRepository;
import com.siglocc.repository.IglesiaRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link EntregaService}.
 *
 * <p>Cubre la validación de la iglesia (debe existir, estar APROBADA, no
 * tener ya un acta en la temporada), la misma regla "exactamente uno de
 * categoriaCajaId/tipoItemId" que comparten {@code RecepcionContenedorService}
 * y {@code AsignacionService}, la validación del catálogo real al crear cada
 * detalle, y las transiciones PENDIENTE → COMPLETADA/PARCIAL.</p>
 */
@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    @Mock private EntregaIglesiaRepository entregaRepo;
    @Mock private DetalleEntregaIglesiaRepository detalleRepo;
    @Mock private TipoItemRepository tipoItemRepo;
    @Mock private CategoriaCajaRepository categoriaCajaRepo;
    @Mock private IglesiaRepository iglesiaRepo;
    @Mock private FotoEntregaIglesiaRepository fotoEntregaRepo;
    @Mock private FotoEntregaNinosRepository fotoNinosRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private StorageService storageService;

    private EntregaService service;

    private static final Integer EQUIPO_ID = 24;
    private static final Integer TEMPORADA_ID = 1;
    private static final Integer IGLESIA_ID = 8;
    private static final Integer CATEGORIA_ID = 1;

    @BeforeEach
    void setUp() {
        service = new EntregaService(entregaRepo, detalleRepo, tipoItemRepo, categoriaCajaRepo,
                iglesiaRepo, fotoEntregaRepo, fotoNinosRepo, usuarioRepo, storageService);
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

    private Iglesia iglesiaAprobada() {
        Iglesia i = new Iglesia();
        i.setId(IGLESIA_ID);
        i.setNombre("Iglesia Central");
        i.setEstado(EstadoIglesia.APROBADA);
        return i;
    }

    private EntregaRequest requestValido(List<EntregaDetalleRequest> detalles) {
        return new EntregaRequest(IGLESIA_ID, 7, TEMPORADA_ID, null, null, "obs", detalles);
    }

    private EntregaIglesia entregaEnEstado(EstadoEntrega estado) {
        EntregaIglesia e = new EntregaIglesia();
        e.setId(1);
        e.setIglesiaId(IGLESIA_ID);
        e.setEquipoId(EQUIPO_ID);
        e.setTemporadaId(TEMPORADA_ID);
        e.setEstado(estado);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearEntrega — validaciones de forma
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearEntrega_iglesiaIdNulo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        EntregaRequest request = new EntregaRequest(null, 7, TEMPORADA_ID, null, null, null, List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iglesiaId");
    }

    @Test
    void crearEntrega_puntoEntregaIdNulo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        EntregaRequest request = new EntregaRequest(IGLESIA_ID, null, TEMPORADA_ID, null, null, null, List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("puntoEntregaId");
    }

    @Test
    void crearEntrega_temporadaIdNula_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        EntregaRequest request = new EntregaRequest(IGLESIA_ID, 7, null, null, null, null, List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temporadaId");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearEntrega — validación de la iglesia
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearEntrega_iglesiaNoExiste_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.empty());
        EntregaRequest request = requestValido(List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Iglesia no encontrada");
    }

    @Test
    void crearEntrega_iglesiaNoAprobada_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        Iglesia pendiente = iglesiaAprobada();
        pendiente.setEstado(EstadoIglesia.PENDIENTE);
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(pendiente));
        EntregaRequest request = requestValido(List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está aprobada");
    }

    @Test
    void crearEntrega_yaExisteActaParaLaTemporada_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(true);
        EntregaRequest request = requestValido(List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un acta");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearEntrega — firmaTipo
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crearEntrega_firmaTipoInvalido_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        EntregaRequest request = new EntregaRequest(IGLESIA_ID, 7, TEMPORADA_ID, null,
                "MANUSCRITA", null, List.of());

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firmaTipo inválido");
    }

    @Test
    void crearEntrega_firmaTipoDigital_seAsignaCorrectamente() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        when(categoriaCajaRepo.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoria()));
        EntregaRequest request = new EntregaRequest(IGLESIA_ID, 7, TEMPORADA_ID, null, "digital", null,
                List.of(new EntregaDetalleRequest(CATEGORIA_ID, null, 10)));

        EntregaResponse response = service.crearEntrega(request);

        assertThat(response.firmaTipo()).isEqualTo("DIGITAL");
    }

    // ─────────────────────────────────────────────────────────────────────
    // crearEntrega — regla "exactamente uno" y validación de catálogo
    // ─────────────────────────────────────────────────────────────────────

    private CategoriaCaja categoria() {
        CategoriaCaja c = new CategoriaCaja();
        c.setId(CATEGORIA_ID);
        c.setCodigo("NINO_2_4");
        c.setDescripcion("Niño 2-4 años");
        return c;
    }

    private TipoItem tipoItem() {
        TipoItem t = new TipoItem();
        t.setId(5);
        t.setCodigo("GM");
        t.setNombreCompleto("Guía Ministerial");
        return t;
    }

    @Test
    void crearEntrega_detalleConAmbosIds_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        EntregaRequest request = requestValido(
                List.of(new EntregaDetalleRequest(CATEGORIA_ID, 5, 10)));

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void crearEntrega_detalleSinNingunId_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        EntregaRequest request = requestValido(
                List.of(new EntregaDetalleRequest(null, null, 10)));

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void crearEntrega_cantidadEntregadaCero_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        EntregaRequest request = requestValido(
                List.of(new EntregaDetalleRequest(CATEGORIA_ID, null, 0)));

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    void crearEntrega_categoriaCajaNoExisteEnCatalogo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        when(categoriaCajaRepo.findById(99)).thenReturn(Optional.empty());
        EntregaRequest request = requestValido(
                List.of(new EntregaDetalleRequest(99, null, 10)));

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoría de caja no encontrada");
    }

    @Test
    void crearEntrega_tipoItemNoExisteEnCatalogo_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        when(tipoItemRepo.findById(99)).thenReturn(Optional.empty());
        EntregaRequest request = requestValido(
                List.of(new EntregaDetalleRequest(null, 99, 10)));

        assertThatThrownBy(() -> service.crearEntrega(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de ítem no encontrado");
    }

    @Test
    void crearEntrega_exitoso_quedaEnEstadoPendienteConLosDetallesCorrectos() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(iglesiaRepo.findById(IGLESIA_ID)).thenReturn(Optional.of(iglesiaAprobada()));
        when(entregaRepo.existsByIglesiaIdAndTemporadaId(IGLESIA_ID, TEMPORADA_ID)).thenReturn(false);
        // El mismo stub cubre la validación en crearEntrega() y el enriquecimiento en construirResponse()
        when(categoriaCajaRepo.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoria()));
        when(tipoItemRepo.findById(5)).thenReturn(Optional.of(tipoItem()));
        EntregaRequest request = requestValido(List.of(
                new EntregaDetalleRequest(CATEGORIA_ID, null, 10),
                new EntregaDetalleRequest(null, 5, 20)
        ));

        EntregaResponse response = service.crearEntrega(request);

        assertThat(response.estado()).isEqualTo("PENDIENTE");
        assertThat(response.equipoId()).isEqualTo(EQUIPO_ID);
        assertThat(response.confirmado()).isFalse();
        assertThat(response.detalles()).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────────────────
    // completarEntrega
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void completarEntrega_actaNoExiste_lanzaIllegalArgumentException() {
        when(entregaRepo.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completarEntrega(1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void completarEntrega_noEstaPendiente_lanzaIllegalStateException() {
        when(entregaRepo.findById(1)).thenReturn(Optional.of(entregaEnEstado(EstadoEntrega.COMPLETADA)));

        assertThatThrownBy(() -> service.completarEntrega(1, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    void completarEntrega_parcialFalse_quedaEnEstadoCompletadaYConfirmada() {
        EntregaIglesia entrega = entregaEnEstado(EstadoEntrega.PENDIENTE);
        when(entregaRepo.findById(1)).thenReturn(Optional.of(entrega));
        when(detalleRepo.findByEntregaId(1)).thenReturn(List.of());

        EntregaResponse response = service.completarEntrega(1, false);

        assertThat(response.estado()).isEqualTo("COMPLETADA");
        assertThat(response.confirmado()).isTrue();
    }

    @Test
    void completarEntrega_parcialTrue_quedaEnEstadoParcial() {
        EntregaIglesia entrega = entregaEnEstado(EstadoEntrega.PENDIENTE);
        when(entregaRepo.findById(1)).thenReturn(Optional.of(entrega));
        when(detalleRepo.findByEntregaId(1)).thenReturn(List.of());

        EntregaResponse response = service.completarEntrega(1, true);

        assertThat(response.estado()).isEqualTo("PARCIAL");
        assertThat(response.confirmado()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────
    // listar — despacho por jerarquía
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void listar_comoENL_consultaFindByTemporadaId() {
        autenticarComo(1, "ENL");
        when(entregaRepo.findByTemporadaId(TEMPORADA_ID)).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(entregaRepo).findByTemporadaId(TEMPORADA_ID);
        verify(entregaRepo, never()).findByErleClusterAndTemporada(anyInt(), anyInt());
        verify(entregaRepo, never()).findByEquipoIdAndTemporadaId(anyInt(), anyInt());
    }

    @Test
    void listar_comoERLE_consultaFindByErleClusterAndTemporada() {
        autenticarComo(2, "ERLE");
        when(entregaRepo.findByErleClusterAndTemporada(2, TEMPORADA_ID)).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(entregaRepo).findByErleClusterAndTemporada(2, TEMPORADA_ID);
    }

    @Test
    void listar_comoERL_consultaFindByEquipoIdAndTemporadaId() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(entregaRepo.findByEquipoIdAndTemporadaId(EQUIPO_ID, TEMPORADA_ID)).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(entregaRepo).findByEquipoIdAndTemporadaId(EQUIPO_ID, TEMPORADA_ID);
    }

    @Test
    void listar_tipoEquipoNoReconocido_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "SUPERVISOR");

        assertThatThrownBy(() -> service.listar(TEMPORADA_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }
}
