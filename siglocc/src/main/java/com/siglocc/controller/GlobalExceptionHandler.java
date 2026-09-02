package com.siglocc.controller;

import com.siglocc.security.IdentidadJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manejador global de excepciones para todos los controllers REST.
 *
 * <p>Centraliza las respuestas de error para que ningún controller
 * necesite declarar sus propios {@code @ExceptionHandler}:</p>
 * <ul>
 *   <li>{@link IdentidadJwtException}   → 400 Bad Request (token sin identidad jerárquica; el cliente debe reautenticar)</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request</li>
 *   <li>{@link NoSuchElementException}   → 404 Not Found</li>
 *   <li>{@link IllegalStateException}    → 409 Conflict (conflicto de estado o permisos de negocio)</li>
 * </ul>
 *
 * <p>{@link IdentidadJwtException} extiende {@link IllegalStateException} pero se
 * resuelve con este handler más específico antes que el genérico de 409, de modo
 * que el único caso "vuelve a iniciar sesión" responde 400 en todos los endpoints
 * sin reclasificar los demás conflictos de estado/permisos que sí deben seguir
 * siendo 409.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdentidadJwtException.class)
    public ResponseEntity<Map<String, String>> handleIdentidadJwt(IdentidadJwtException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
