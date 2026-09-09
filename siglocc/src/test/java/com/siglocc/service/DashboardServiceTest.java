package com.siglocc.service;

import com.siglocc.dto.DashboardItemResponse;
import com.siglocc.entity.VistaDashboardFinanciero;
import com.siglocc.entity.VistaDashboardId;
import com.siglocc.repository.VistaDashboardFinancieroRepository;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link DashboardService}.
 *
 * <p>El foco es el ordenamiento jerárquico ({@code ORDEN_JERARQUICO}) que ya
 * tuvo un bug real (ver historial: el ENL quedaba al final por usar
 * {@code nullsLast} en vez de {@code nullsFirst} en {@code enl_id}) — estas
 * pruebas son la regresión para que ese bug no vuelva a colarse.</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VistaDashboardFinancieroRepository dashboardRepo;

    private DashboardService service;

    private static final Integer TEMPORADA_ID = 1;

    @BeforeEach
    void setUp() {
        service = new DashboardService(dashboardRepo);
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

    /** Mockea una fila de {@code vista_dashboard_financiero}, solo con los campos de jerarquía. */
    private VistaDashboardFinanciero fila(Integer equipoId, String nombre, String tipo,
                                          Integer erleId, Integer enlId) {
        VistaDashboardFinanciero v = mock(VistaDashboardFinanciero.class);
        when(v.getId()).thenReturn(new VistaDashboardId(equipoId, TEMPORADA_ID));
        when(v.getEquipoNombre()).thenReturn(nombre);
        when(v.getEquipoTipo()).thenReturn(tipo);
        // lenient: el comparador solo lee erleId cuando enlId empata (thenComparing);
        // para filas cuyo enlId no empata con ninguna otra (ej. el ENL raiz), este
        // getter nunca llega a invocarse, y eso es correcto, no un test mal escrito.
        lenient().when(v.getErleId()).thenReturn(erleId);
        when(v.getEnlId()).thenReturn(enlId);
        return v;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Identidad y despacho por equipoTipo
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void getConsolidado_sinIdentidadJwt_lanzaIdentidadJwtException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> service.getConsolidado(TEMPORADA_ID))
                .isInstanceOf(IdentidadJwtException.class);
    }

    @Test
    void getConsolidado_conTipoEquipoNoReconocido_lanzaIllegalArgumentException() {
        autenticarComo(1, "SUPERVISOR");

        assertThatThrownBy(() -> service.getConsolidado(TEMPORADA_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }

    @Test
    void getConsolidado_comoENL_consultaFindAllByTemporada() {
        autenticarComo(1, "ENL");
        when(dashboardRepo.findAllByTemporada(TEMPORADA_ID)).thenReturn(List.of());

        service.getConsolidado(TEMPORADA_ID);

        verify(dashboardRepo).findAllByTemporada(TEMPORADA_ID);
        verify(dashboardRepo, never()).findByErleAndTemporada(anyInt(), anyInt());
        verify(dashboardRepo, never()).findByEquipoAndTemporada(anyInt(), anyInt());
    }

    @Test
    void getConsolidado_comoERLE_consultaFindByErleAndTemporada() {
        autenticarComo(2, "ERLE");
        when(dashboardRepo.findByErleAndTemporada(2, TEMPORADA_ID)).thenReturn(List.of());

        service.getConsolidado(TEMPORADA_ID);

        verify(dashboardRepo).findByErleAndTemporada(2, TEMPORADA_ID);
        verify(dashboardRepo, never()).findAllByTemporada(anyInt());
    }

    @Test
    void getConsolidado_comoERL_consultaFindByEquipoAndTemporada() {
        autenticarComo(36, "ERL");
        when(dashboardRepo.findByEquipoAndTemporada(36, TEMPORADA_ID)).thenReturn(List.of());

        service.getConsolidado(TEMPORADA_ID);

        verify(dashboardRepo).findByEquipoAndTemporada(36, TEMPORADA_ID);
        verify(dashboardRepo, never()).findAllByTemporada(anyInt());
    }

    // ─────────────────────────────────────────────────────────────────────
    // ORDEN_JERARQUICO — regresión del bug nullsLast -> nullsFirst
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void getConsolidado_elEnlSiempreSaleDePrimero() {
        // El ENL es la raiz: enl_id = null. Si el orden estuviera mal
        // (nullsLast) quedaria de ULTIMO en vez de PRIMERO.
        VistaDashboardFinanciero enl = fila(1, "Equipo Nacional de Liderazgo", "ENL", null, null);
        VistaDashboardFinanciero erle = fila(2, "Atlantico", "ERLE", null, 1);
        VistaDashboardFinanciero erl = fila(24, "Cesar", "ERL", 2, 1);

        // Se entregan deliberadamente desordenados
        autenticarComo(1, "ENL");
        when(dashboardRepo.findAllByTemporada(TEMPORADA_ID))
                .thenReturn(new ArrayList<>(List.of(erl, erle, enl)));

        List<DashboardItemResponse> resultado = service.getConsolidado(TEMPORADA_ID);

        assertThat(resultado).extracting(DashboardItemResponse::equipoId)
                .as("el ENL (enl_id null) debe quedar primero, no ultimo")
                .containsExactly(1, 2, 24);
    }

    @Test
    void getConsolidado_agrupaCadaErlBajoSuErleCorrecto() {
        // Dos clusters ERLE distintos, entregados intercalados para probar
        // que el agrupamiento por coalesce(erle_id, equipo_id) funciona.
        VistaDashboardFinanciero enl = fila(1, "ENL Nacional", "ENL", null, null);
        VistaDashboardFinanciero erleAtlantico = fila(2, "Atlantico", "ERLE", null, 1);
        VistaDashboardFinanciero erleBolivar = fila(7, "Bolivar", "ERLE", null, 1);
        VistaDashboardFinanciero cesar = fila(24, "Cesar", "ERL", 2, 1);       // hijo de Atlantico
        VistaDashboardFinanciero guajira = fila(25, "Guajira", "ERL", 2, 1);   // hijo de Atlantico
        VistaDashboardFinanciero sucre = fila(30, "Sucre", "ERL", 7, 1);       // hijo de Bolivar

        autenticarComo(1, "ENL");
        when(dashboardRepo.findAllByTemporada(TEMPORADA_ID)).thenReturn(
                new ArrayList<>(List.of(sucre, cesar, erleBolivar, guajira, enl, erleAtlantico)));

        List<DashboardItemResponse> resultado = service.getConsolidado(TEMPORADA_ID);

        assertThat(resultado).extracting(DashboardItemResponse::equipoId)
                .as("ENL, luego Atlantico con sus 2 hijos juntos, luego Bolivar con su hijo")
                .containsExactly(1, 2, 24, 25, 7, 30);
    }

    @Test
    void getConsolidado_conListaVacia_noFalla() {
        autenticarComo(1, "ENL");
        when(dashboardRepo.findAllByTemporada(TEMPORADA_ID)).thenReturn(List.of());

        List<DashboardItemResponse> resultado = service.getConsolidado(TEMPORADA_ID);

        assertThat(resultado).isEmpty();
    }
}
