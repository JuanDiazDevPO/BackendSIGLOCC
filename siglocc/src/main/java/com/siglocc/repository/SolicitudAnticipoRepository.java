package com.siglocc.repository;

import com.siglocc.entity.SolicitudAnticipo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad {@link SolicitudAnticipo}.
 *
 * <p>Gestiona la persistencia de las solicitudes de anticipo.
 * Los métodos CRUD básicos son suficientes por ahora; se pueden agregar
 * consultas personalizadas aquí cuando se requiera listar solicitudes
 * por equipo, estado o temporada.</p>
 */
public interface SolicitudAnticipoRepository extends JpaRepository<SolicitudAnticipo, Integer> {}
