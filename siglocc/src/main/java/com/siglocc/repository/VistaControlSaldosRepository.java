package com.siglocc.repository;

import com.siglocc.entity.VistaControlSaldos;
import com.siglocc.entity.VistaControlSaldosId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad de solo lectura {@link VistaControlSaldos}.
 *
 * <p>Permite consultar la vista {@code vista_control_saldos_enl} usando la
 * clave compuesta {@link VistaControlSaldosId}. El método principal que se
 * usa es {@code findById(equipoId, temporadaId)} para obtener los saldos
 * disponibles antes de aceptar una solicitud de anticipo.</p>
 *
 * <p>Al estar marcada la entidad como {@code @Immutable}, Hibernate nunca
 * intentará hacer escrituras sobre esta vista.</p>
 */
public interface VistaControlSaldosRepository extends JpaRepository<VistaControlSaldos, VistaControlSaldosId> {}
