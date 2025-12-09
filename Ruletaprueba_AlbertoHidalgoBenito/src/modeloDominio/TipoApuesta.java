package modeloDominio;

/**
 * Enum TipoApuesta
 * ----------------
 * Representa los distintos tipos de apuesta que un jugador puede realizar en la ruleta.
 *
 * Valores:
 *  - NUMERO   → Apuesta directa a un número específico (0–36).
 *  - COLOR    → Apuesta al color de la casilla (ROJO, NEGRO).
 *  - PAR_IMPAR→ Apuesta a la paridad del número (PAR o IMPAR).
 *  - DOCENA   → Apuesta a la docena (1ª: 1–12, 2ª: 13–24, 3ª: 25–36).
 *
 * PRECONDICIONES:
 *  - El tipo de apuesta debe ser uno de los valores definidos en este enum.
 *
 * POSTCONDICIONES:
 *  - Se obtiene un valor de tipo de apuesta válido.
 *  - El método toString() devuelve el nombre del tipo en mayúsculas.
 */
public enum TipoApuesta {
    NUMERO,
    COLOR,
    PAR_IMPAR,
    DOCENA
}
