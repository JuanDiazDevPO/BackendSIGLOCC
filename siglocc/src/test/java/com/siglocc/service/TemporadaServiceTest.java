package com.siglocc.service;

import com.siglocc.dto.TemporadaRequest;
import com.siglocc.dto.TemporadaResponse;
import com.siglocc.entity.Temporada;
import com.siglocc.repository.TemporadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link TemporadaService}.
 *
 * <p>El foco es la invariante central del servicio: nunca puede haber más de
 * una temporada con {@code esActual = true} al mismo tiempo, y la validación
 * de fechas al crear/editar.</p>
 */
@ExtendWith(MockitoExtension.class)
class TemporadaServiceTest {

    @Mock
    private TemporadaRepository temporadaRepo;

    private TemporadaService service;

    @BeforeEach
    void setUp() {
        service = new TemporadaService(temporadaRepo);
    }

    // ─────────────────────────────────────────────────────────────────────
    // crear()
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void crear_conNombreVacio_lanzaIllegalArgumentException() {
        TemporadaRequest request = new TemporadaRequest("  ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        verify(temporadaRepo, never()).save(any());
    }

    @Test
    void crear_sinFechaInicio_lanzaIllegalArgumentException() {
        TemporadaRequest request = new TemporadaRequest("Temporada 2026", null, LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inicio");
    }

    @Test
    void crear_sinFechaFin_lanzaIllegalArgumentException() {
        TemporadaRequest request = new TemporadaRequest("Temporada 2026", LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fin");
    }

    @Test
    void crear_conFechaFinAntesDeFechaInicio_lanzaIllegalArgumentException() {
        TemporadaRequest request = new TemporadaRequest(
                "Temporada invertida", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void crear_conFechaFinIgualAFechaInicio_lanzaIllegalArgumentException() {
        // fechaFin debe ser estrictamente posterior, no solo >=
        LocalDate misma = LocalDate.of(2026, 6, 1);
        TemporadaRequest request = new TemporadaRequest("Temporada de un dia", misma, misma);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crear_conDatosValidos_creaConEsActualEnFalse() {
        TemporadaRequest request = new TemporadaRequest(
                "Temporada 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        TemporadaResponse response = service.crear(request);

        ArgumentCaptor<Temporada> captor = ArgumentCaptor.forClass(Temporada.class);
        verify(temporadaRepo).save(captor.capture());

        Temporada guardada = captor.getValue();
        assertThat(guardada.getNombre()).isEqualTo("Temporada 2026");
        assertThat(guardada.isEsActual())
                .as("una temporada recien creada nunca debe nacer activa")
                .isFalse();
        assertThat(response.nombre()).isEqualTo("Temporada 2026");
        assertThat(response.esActual()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────
    // editar()
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void editar_conIdInexistente_lanzaNoSuchElementException() {
        when(temporadaRepo.findById(99)).thenReturn(Optional.empty());
        TemporadaRequest request = new TemporadaRequest(
                "X", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.editar(99, request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void editar_noModificaEsActual() {
        Temporada existente = new Temporada();
        existente.setNombre("Nombre viejo");
        existente.setEsActual(true);
        when(temporadaRepo.findById(5)).thenReturn(Optional.of(existente));

        TemporadaRequest request = new TemporadaRequest(
                "Nombre nuevo", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));

        TemporadaResponse response = service.editar(5, request);

        assertThat(response.nombre()).isEqualTo("Nombre nuevo");
        assertThat(response.esActual())
                .as("editar no debe tocar esActual, solo activar() puede cambiarlo")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────
    // activar() — la invariante central
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void activar_conIdInexistente_lanzaNoSuchElementException() {
        when(temporadaRepo.findById(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(42))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void activar_desactivaLaTemporadaQueEstabaActivaAntes() {
        // Temporada.id es autogenerado por JPA y no tiene setter publico --
        // se mockea la entidad para poder controlar el id en la prueba.
        Temporada viejaActiva = mock(Temporada.class);
        when(viejaActiva.getId()).thenReturn(1);

        Temporada nueva = mock(Temporada.class);
        when(nueva.getId()).thenReturn(2);
        when(nueva.getNombre()).thenReturn("Temporada 2026");
        when(nueva.isEsActual()).thenReturn(true);

        when(temporadaRepo.findById(2)).thenReturn(Optional.of(nueva));
        when(temporadaRepo.findByEsActualTrue()).thenReturn(Optional.of(viejaActiva));

        TemporadaResponse response = service.activar(2);

        assertThat(response.esActual()).isTrue();
        // la prueba real de comportamiento: se llamaron los setters correctos
        verify(nueva).setEsActual(true);
        verify(viejaActiva)
                .setEsActual(false);
        verify(temporadaRepo).save(viejaActiva);
        verify(temporadaRepo).save(nueva);
    }

    @Test
    void activar_laMismaQueYaEstaActiva_esIdempotente() {
        Temporada actual = mock(Temporada.class);
        when(actual.getId()).thenReturn(2);
        when(actual.getNombre()).thenReturn("Temporada 2026");
        when(actual.isEsActual()).thenReturn(true);

        when(temporadaRepo.findById(2)).thenReturn(Optional.of(actual));
        when(temporadaRepo.findByEsActualTrue()).thenReturn(Optional.of(actual));

        TemporadaResponse response = service.activar(2);

        assertThat(response.esActual()).isTrue();
        // mismo id -> nunca debe intentar desactivarla a si misma
        verify(actual, never()).setEsActual(false);
        verify(temporadaRepo, times(1)).save(actual);
    }

    @Test
    void activar_cuandoNoHabiaNingunaActiva_soloActivaLaNueva() {
        Temporada nueva = mock(Temporada.class);
        when(nueva.getId()).thenReturn(1);
        when(nueva.getNombre()).thenReturn("Primera temporada");
        when(nueva.isEsActual()).thenReturn(true);

        when(temporadaRepo.findById(1)).thenReturn(Optional.of(nueva));
        when(temporadaRepo.findByEsActualTrue()).thenReturn(Optional.empty());

        TemporadaResponse response = service.activar(1);

        assertThat(response.esActual()).isTrue();
        verify(nueva).setEsActual(true);
        verify(temporadaRepo, times(1)).save(nueva);
    }
}
