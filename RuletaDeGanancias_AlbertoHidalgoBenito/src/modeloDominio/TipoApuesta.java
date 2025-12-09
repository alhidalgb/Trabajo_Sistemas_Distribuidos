package modeloDominio;

/**
 * Enum TipoApuesta
 * ----------------
 * Catálogo de las modalidades de apuesta disponibles en el sistema.
 * Se utiliza tanto en el Cliente (para crear la apuesta) como en el Servidor (para calcular premios).
 */
public enum TipoApuesta {
    /** Apuesta directa a un número específico (0-36). Pago 35:1. */
    NUMERO,
    
    /** Apuesta al color (ROJO o NEGRO). Pago 1:1. */
    COLOR,
    
    /** Apuesta a la paridad (PAR o IMPAR). Pago 1:1. */
    PAR_IMPAR,
    
    /** Apuesta a rangos de docenas (1-12, 13-24, 25-36). Pago 2:1. */
    DOCENA
}