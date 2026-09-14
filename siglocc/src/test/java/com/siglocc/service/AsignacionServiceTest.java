package com.siglocc.service;

import com.siglocc.dto.AjusteAsignacionRequest;
import com.siglocc.dto.AsignacionDetalleResponse;
import com.siglocc.dto.AsignacionResponse;
import com.siglocc.entity.AsignacionCabecera;
import com.siglocc.entity.AsignacionDetalle;
import com.siglocc.entity.CategoriaCaja;
import com.siglocc.entity.EstadoAsignacion;
import com.siglocc.entity.EstadoIglesia;
import com.siglocc.entity.Iglesia;
import com.siglocc.entity.TipoItem;
import com.siglocc.repository.AsignacionCabeceraRepository;
import com.siglocc.repository.AsignacionDetalleRepository;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.DetalleRecepcionContenedorRepository;
import com.siglocc.repository.IglesiaRepository;
import com.siglocc.repository.TipoItemRepository;
import com.siglocc.security.JwtAuthDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AsignacionService}.
 *
 * <p>El foco central es el Método de Hamilton dentro de
 * {@link AsignacionService#generarAsignacionAutomatica}: no es un método
 * público (no se puede testear directo), así que se valida a través de sus
 * invariantes observables — la suma de lo asignado siempre da exactamente el
 * disponible, y las unidades sobrantes van a quien tiene mayor residuo
 * fraccionario. Solo se mockea 1 categoría de caja a la vez (y tipos de ítem
 * vacíos salvo que el test los necesite) para que cada escenario quede claro
 * sin tener que stubear las 6 categorías reales.</p>
 */
@ExtendWith(MockitoExtension.class)
class AsignacionServiceTest {

    @Mock private AsignacionCabeceraRepository cabeceraRepo;
    @Mock private AsignacionDetalleRepository detalleRepo;
    @Mock private IglesiaRepository iglesiaRepo;
    @Mock private CategoriaCajaRepository categoriaCajaRepo;
    @Mock private TipoItemRepository tipoItemRepo;
    @Mock private DetalleRecepcionContenedorRepository detalleRecepcionRepo;

    private AsignacionService service;

    private static final Integer EQUIPO_ID = 24;
    private static final Integer TEMPORADA_ID = 1;
    private static final Integer CATEGORIA_ID = 1;

    @BeforeEach
    void setUp() {
        service = new AsignacionService(cabeceraRepo, detalleRepo, iglesiaRepo,
                categoriaCajaRepo, tipoItemRepo, detalleRecepcionRepo);
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

    private Iglesia iglesia(Integer id, Integer cajasSolicitadas) {
        Iglesia i = new Iglesia();
        i.setId(id);
        i.setNombre("Iglesia " + id);
        i.setCajasSolicitadas(cajasSolicitadas);
        i.setEstado(EstadoIglesia.APROBADA);
        return i;
    }

    private CategoriaCaja categoria(Integer id) {
        CategoriaCaja c = new CategoriaCaja();
        c.setId(id);
        c.setCodigo("NINO_2_4");
        c.setDescripcion("Niño 2-4 años");
        return c;
    }

    /** Configura 1 categoría de caja (id=1) y ninguna literatura, salvo que el test agregue más. */
    private void mockearUnaCategoriaSinLiteratura() {
        when(categoriaCajaRepo.findAll()).thenReturn(List.of(categoria(CATEGORIA_ID)));
        when(tipoItemRepo.findAll()).thenReturn(List.of());
    }

    private AsignacionCabecera cabeceraEnEstado(EstadoAsignacion estado) {
        AsignacionCabecera c = new AsignacionCabecera();
        c.setId(1);
        c.setEquipoId(EQUIPO_ID);
        c.setTemporadaId(TEMPORADA_ID);
        c.setEstado(estado);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────
    // generarAsignacionAutomatica — validaciones de forma
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void generarAsignacionAutomatica_sinIglesiasAprobadas_lanzaIllegalStateException() {
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay iglesias APROBADAS");
    }

    @Test
    void generarAsignacionAutomatica_iglesiasAprobadasSinCajasSolicitadas_lanzaIllegalStateException() {
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA))
                .thenReturn(List.of(iglesia(1, 0), iglesia(2, null)));

        assertThatThrownBy(() -> service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cajas solicitadas");
    }

    // ─────────────────────────────────────────────────────────────────────
    // generarAsignacionAutomatica — factorReduccion
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void generarAsignacionAutomatica_stockSuficiente_factorReduccionEsUno() {
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA))
                .thenReturn(List.of(iglesia(1, 3), iglesia(2, 7)));
        mockearUnaCategoriaSinLiteratura();
        when(detalleRecepcionRepo.sumCajasByEquipoAndTemporadaAndCategoria(
                EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(10);
        when(detalleRepo.sumAsignadoCajas(EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(0);

        AsignacionResponse response = service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID);

        assertThat(response.factorReduccion()).isEqualByComparingTo("1.0000");
        assertThat(response.totalCajasDisponibles()).isEqualTo(10);
        assertThat(response.totalCajasSolicitadas()).isEqualTo(10);
    }

    @Test
    void generarAsignacionAutomatica_stockInsuficiente_calculaFactorReduccionProporcional() {
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA))
                .thenReturn(List.of(iglesia(1, 2), iglesia(2, 2)));
        mockearUnaCategoriaSinLiteratura();
        // 4 solicitadas, solo 2 disponibles -> factor = 2/4 = 0.5000
        when(detalleRecepcionRepo.sumCajasByEquipoAndTemporadaAndCategoria(
                EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(2);
        when(detalleRepo.sumAsignadoCajas(EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(0);

        AsignacionResponse response = service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID);

        assertThat(response.factorReduccion()).isEqualByComparingTo("0.5000");
    }

    // ─────────────────────────────────────────────────────────────────────
    // generarAsignacionAutomatica — Método Hamilton (invariantes)
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void hamilton_laSumaDeLoAsignadoSiempreDaExactamenteElDisponible_yElMayorResiduoGanaElSobrante() {
        // 3 iglesias piden 1 caja cada una (total 3), solo hay 2 disponibles.
        // Cuota exacta de cada una: (1/3)*2 = 0.6667 -> floor 0 para las 3.
        // Sobrante = 2 - 0 = 2 -> se reparte 1 a cada una de las 2 primeras
        // (mismo residuo fraccionario -> orden estable de la lista original).
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA))
                .thenReturn(List.of(iglesia(1, 1), iglesia(2, 1), iglesia(3, 1)));
        mockearUnaCategoriaSinLiteratura();
        when(detalleRecepcionRepo.sumCajasByEquipoAndTemporadaAndCategoria(
                EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(2);
        when(detalleRepo.sumAsignadoCajas(EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(0);

        AsignacionResponse response = service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID);

        int sumaAsignada = response.detalles().stream()
                .mapToInt(AsignacionDetalleResponse::cantidadAsignada)
                .sum();
        assertThat(sumaAsignada)
                .as("el metodo Hamilton debe agotar el inventario exactamente, sin sobrar ni faltar")
                .isEqualTo(2);
        assertThat(response.detalles()).hasSize(3);
        assertThat(response.detalles()).filteredOn(d -> d.cantidadAsignada() == 1).hasSize(2);
        assertThat(response.detalles()).filteredOn(d -> d.cantidadAsignada() == 0).hasSize(1);
    }

    @Test
    void hamilton_sinStockDisponible_asignaCeroATodasLasIglesias() {
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA))
                .thenReturn(List.of(iglesia(1, 5), iglesia(2, 5)));
        mockearUnaCategoriaSinLiteratura();
        // No se stubean sumCajas/sumAsignado -> Mockito devuelve 0 por defecto -> disponible = 0
        when(detalleRecepcionRepo.sumCajasByEquipoAndTemporadaAndCategoria(
                EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(0);
        when(detalleRepo.sumAsignadoCajas(EQUIPO_ID, TEMPORADA_ID, CATEGORIA_ID)).thenReturn(0);

        AsignacionResponse response = service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID);

        assertThat(response.detalles()).allSatisfy(d ->
                assertThat(d.cantidadAsignada()).isZero());
    }

    @Test
    void hamilton_tambienDistribuyeLiteratura() {
        Integer tipoItemId = 5;
        when(iglesiaRepo.findByEquipoIdAndTemporadaIdAndEstado(
                EQUIPO_ID, TEMPORADA_ID, EstadoIglesia.APROBADA))
                .thenReturn(List.of(iglesia(1, 4)));
        when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        TipoItem item = new TipoItem();
        item.setId(tipoItemId);
        item.setCodigo("GM");
        when(tipoItemRepo.findAll()).thenReturn(List.of(item));
        when(detalleRecepcionRepo.sumLiteraturaByEquipoAndTemporadaAndTipo(
                EQUIPO_ID, TEMPORADA_ID, tipoItemId)).thenReturn(4);
        when(detalleRepo.sumAsignadoLiteratura(EQUIPO_ID, TEMPORADA_ID, tipoItemId)).thenReturn(0);

        AsignacionResponse response = service.generarAsignacionAutomatica(EQUIPO_ID, TEMPORADA_ID);

        assertThat(response.detalles()).hasSize(1);
        assertThat(response.detalles().get(0).tipoItemId()).isEqualTo(tipoItemId);
        assertThat(response.detalles().get(0).cantidadAsignada()).isEqualTo(4);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ajustarLinea
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void ajustarLinea_cabeceraNoExiste_lanzaIllegalArgumentException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.empty());
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, CATEGORIA_ID, null, 5);

        assertThatThrownBy(() -> service.ajustarLinea(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void ajustarLinea_cabeceraNoEstaEnBorrador_lanzaIllegalStateException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.CONFIRMADA)));
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, CATEGORIA_ID, null, 5);

        assertThatThrownBy(() -> service.ajustarLinea(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BORRADOR");
    }

    @Test
    void ajustarLinea_cantidadNegativa_lanzaIllegalArgumentException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.BORRADOR)));
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, CATEGORIA_ID, null, -1);

        assertThatThrownBy(() -> service.ajustarLinea(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 o mayor");
    }

    @Test
    void ajustarLinea_conAmbosIds_lanzaIllegalArgumentException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.BORRADOR)));
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, CATEGORIA_ID, 5, 3);

        assertThatThrownBy(() -> service.ajustarLinea(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void ajustarLinea_sinNingunId_lanzaIllegalArgumentException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.BORRADOR)));
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, null, null, 3);

        assertThatThrownBy(() -> service.ajustarLinea(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactamente uno");
    }

    @Test
    void ajustarLinea_lineaDeCajaNoEncontrada_lanzaIllegalArgumentException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.BORRADOR)));
        when(detalleRepo.findByCabeceraIdAndIglesiaIdAndCategoriaCajaId(1, 1, CATEGORIA_ID))
                .thenReturn(Optional.empty());
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, CATEGORIA_ID, null, 5);

        assertThatThrownBy(() -> service.ajustarLinea(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Línea de asignación no encontrada");
    }

    @Test
    void ajustarLinea_exitoso_actualizaCantidadYMarcaAjustadaManualmente() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.BORRADOR)));
        AsignacionDetalle detalle = new AsignacionDetalle();
        detalle.setId(10);
        detalle.setCabeceraId(1);
        detalle.setIglesiaId(1);
        detalle.setCategoriaCajaId(CATEGORIA_ID);
        detalle.setCantidadAsignada(3);
        detalle.setAjustadaManualmente(false);
        when(detalleRepo.findByCabeceraIdAndIglesiaIdAndCategoriaCajaId(1, 1, CATEGORIA_ID))
                .thenReturn(Optional.of(detalle));
        when(detalleRepo.findByCabeceraId(1)).thenReturn(List.of(detalle));
        when(iglesiaRepo.findByEquipoIdAndTemporadaId(EQUIPO_ID, TEMPORADA_ID)).thenReturn(List.of());
        when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        when(tipoItemRepo.findAll()).thenReturn(List.of());
        AjusteAsignacionRequest request = new AjusteAsignacionRequest(1, CATEGORIA_ID, null, 8);

        service.ajustarLinea(1, request);

        assertThat(detalle.getCantidadAsignada()).isEqualTo(8);
        assertThat(detalle.getAjustadaManualmente()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────
    // confirmar
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void confirmar_cabeceraNoExiste_lanzaIllegalArgumentException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmar(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void confirmar_yaConfirmada_lanzaIllegalStateException() {
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabeceraEnEstado(EstadoAsignacion.CONFIRMADA)));

        assertThatThrownBy(() -> service.confirmar(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue confirmada");
    }

    @Test
    void confirmar_exitoso_cambiaEstadoACONFIRMADA() {
        AsignacionCabecera cabecera = cabeceraEnEstado(EstadoAsignacion.BORRADOR);
        when(cabeceraRepo.findById(1)).thenReturn(Optional.of(cabecera));
        when(detalleRepo.findByCabeceraId(1)).thenReturn(List.of());
        when(iglesiaRepo.findByEquipoIdAndTemporadaId(EQUIPO_ID, TEMPORADA_ID)).thenReturn(List.of());
        when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        when(tipoItemRepo.findAll()).thenReturn(List.of());

        AsignacionResponse response = service.confirmar(1);

        assertThat(response.estado()).isEqualTo("CONFIRMADA");
        assertThat(cabecera.getEstado()).isEqualTo(EstadoAsignacion.CONFIRMADA);
    }

    // ─────────────────────────────────────────────────────────────────────
    // listar — despacho por jerarquía
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void listar_comoENL_consultaFindByTemporadaId() {
        autenticarComo(1, "ENL");
        when(cabeceraRepo.findByTemporadaIdOrderByFechaGeneracionDesc(TEMPORADA_ID)).thenReturn(List.of());
        when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        when(tipoItemRepo.findAll()).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(cabeceraRepo).findByTemporadaIdOrderByFechaGeneracionDesc(TEMPORADA_ID);
        verify(cabeceraRepo, never()).findByErleClusterAndTemporadaOrderByFechaGeneracionDesc(anyInt(), anyInt());
    }

    @Test
    void listar_comoERLE_consultaFindByErleClusterAndTemporada() {
        autenticarComo(2, "ERLE");
        when(cabeceraRepo.findByErleClusterAndTemporadaOrderByFechaGeneracionDesc(2, TEMPORADA_ID))
                .thenReturn(List.of());
        when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        when(tipoItemRepo.findAll()).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(cabeceraRepo).findByErleClusterAndTemporadaOrderByFechaGeneracionDesc(2, TEMPORADA_ID);
    }

    @Test
    void listar_comoERL_consultaFindByEquipoIdAndTemporadaId() {
        autenticarComo(EQUIPO_ID, "ERL");
        when(cabeceraRepo.findByEquipoIdAndTemporadaIdOrderByFechaGeneracionDesc(EQUIPO_ID, TEMPORADA_ID))
                .thenReturn(List.of());
        when(categoriaCajaRepo.findAll()).thenReturn(List.of());
        when(tipoItemRepo.findAll()).thenReturn(List.of());

        service.listar(TEMPORADA_ID);

        verify(cabeceraRepo).findByEquipoIdAndTemporadaIdOrderByFechaGeneracionDesc(EQUIPO_ID, TEMPORADA_ID);
    }

    @Test
    void listar_tipoEquipoNoReconocido_lanzaIllegalArgumentException() {
        autenticarComo(EQUIPO_ID, "SUPERVISOR");

        assertThatThrownBy(() -> service.listar(TEMPORADA_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }
}
