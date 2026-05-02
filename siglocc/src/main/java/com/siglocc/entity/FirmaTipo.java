package com.siglocc.entity;

/**
 * Tipo de firma utilizada para certificar la entrega de material a una iglesia.
 */
public enum FirmaTipo {

    /** Firma capturada digitalmente a través de la aplicación. */
    DIGITAL,

    /** Acta firmada físicamente, escaneada y subida como imagen/PDF. */
    ESCANEADA
}
