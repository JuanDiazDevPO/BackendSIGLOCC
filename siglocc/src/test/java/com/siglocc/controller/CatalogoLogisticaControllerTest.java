package com.siglocc.controller;

import com.siglocc.dto.CategoriaCajaResponse;
import com.siglocc.dto.TipoItemResponse;
import com.siglocc.entity.CategoriaCaja;
import com.siglocc.entity.GeneroCategoria;
import com.siglocc.entity.TipoItem;
import com.siglocc.repository.CategoriaCajaRepository;
import com.siglocc.repository.TipoItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CatalogoLogisticaController}.
 *
 * <p>Sin lógica de negocio propia -- solo mapea entidades a DTOs -- así que el
 * foco es confirmar que usa los métodos de repositorio ya ordenados
 * ({@code findAllByOrderById}, {@code findAllByOrderByMomentoAscIdAsc}, no
 * {@code findAll()}) y que ningún campo se pierde en el mapeo.</p>
 */
@ExtendWith(MockitoExtension.class)
class CatalogoLogisticaControllerTest {

    @Mock private CategoriaCajaRepository categoriaCajaRepo;
    @Mock private TipoItemRepository tipoItemRepo;

    private CatalogoLogisticaController controller;

    private CategoriaCaja categoria(Integer id, String codigo, GeneroCategoria genero,
                                    Integer edadMin, Integer edadMax, String descripcion) {
        CategoriaCaja c = new CategoriaCaja();
        c.setId(id);
        c.setCodigo(codigo);
        c.setGenero(genero);
        c.setEdadMin(edadMin);
        c.setEdadMax(edadMax);
        c.setDescripcion(descripcion);
        return c;
    }

    private TipoItem tipoItem(Integer id, String codigo, String nombreCompleto,
                              Boolean aplicaNinos, Integer momento) {
        TipoItem t = new TipoItem();
        t.setId(id);
        t.setCodigo(codigo);
        t.setNombreCompleto(nombreCompleto);
        t.setAplicaNinos(aplicaNinos);
        t.setMomento(momento);
        return t;
    }

    // ─────────────────────────────────────────────────────────────────────
    // listarCategoriasCaja
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void listarCategoriasCaja_mapeaTodosLosCampos() {
        controller = new CatalogoLogisticaController(categoriaCajaRepo, tipoItemRepo);
        when(categoriaCajaRepo.findAllByOrderById()).thenReturn(List.of(
                categoria(1, "NINO_2_4", GeneroCategoria.NINO, 2, 4, "Niño 2-4 años")));

        ResponseEntity<List<CategoriaCajaResponse>> response = controller.listarCategoriasCaja();

        assertThat(response.getBody()).hasSize(1);
        CategoriaCajaResponse dto = response.getBody().get(0);
        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.codigo()).isEqualTo("NINO_2_4");
        assertThat(dto.genero()).isEqualTo("NINO");
        assertThat(dto.edadMin()).isEqualTo(2);
        assertThat(dto.edadMax()).isEqualTo(4);
        assertThat(dto.descripcion()).isEqualTo("Niño 2-4 años");
    }

    @Test
    void listarCategoriasCaja_usaElMetodoOrdenadoPorId() {
        controller = new CatalogoLogisticaController(categoriaCajaRepo, tipoItemRepo);
        when(categoriaCajaRepo.findAllByOrderById()).thenReturn(List.of());

        controller.listarCategoriasCaja();

        verify(categoriaCajaRepo).findAllByOrderById();
    }

    @Test
    void listarCategoriasCaja_conListaVacia_noFalla() {
        controller = new CatalogoLogisticaController(categoriaCajaRepo, tipoItemRepo);
        when(categoriaCajaRepo.findAllByOrderById()).thenReturn(List.of());

        ResponseEntity<List<CategoriaCajaResponse>> response = controller.listarCategoriasCaja();

        assertThat(response.getBody()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────
    // listarTiposItem
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void listarTiposItem_mapeaTodosLosCampos() {
        controller = new CatalogoLogisticaController(categoriaCajaRepo, tipoItemRepo);
        when(tipoItemRepo.findAllByOrderByMomentoAscIdAsc()).thenReturn(List.of(
                tipoItem(5, "GM", "Guía Ministerial para maestros", false, 2)));

        ResponseEntity<List<TipoItemResponse>> response = controller.listarTiposItem();

        assertThat(response.getBody()).hasSize(1);
        TipoItemResponse dto = response.getBody().get(0);
        assertThat(dto.id()).isEqualTo(5);
        assertThat(dto.codigo()).isEqualTo("GM");
        assertThat(dto.nombreCompleto()).isEqualTo("Guía Ministerial para maestros");
        assertThat(dto.aplicaNinos()).isFalse();
        assertThat(dto.momento()).isEqualTo(2);
    }

    @Test
    void listarTiposItem_usaElMetodoOrdenadoPorMomento() {
        controller = new CatalogoLogisticaController(categoriaCajaRepo, tipoItemRepo);
        when(tipoItemRepo.findAllByOrderByMomentoAscIdAsc()).thenReturn(List.of());

        controller.listarTiposItem();

        verify(tipoItemRepo).findAllByOrderByMomentoAscIdAsc();
    }

    @Test
    void listarTiposItem_conListaVacia_noFalla() {
        controller = new CatalogoLogisticaController(categoriaCajaRepo, tipoItemRepo);
        when(tipoItemRepo.findAllByOrderByMomentoAscIdAsc()).thenReturn(List.of());

        ResponseEntity<List<TipoItemResponse>> response = controller.listarTiposItem();

        assertThat(response.getBody()).isEmpty();
    }
}
