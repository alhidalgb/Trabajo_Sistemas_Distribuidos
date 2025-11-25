package modeloDominio;

import java.util.Objects;

/**
 * Representa una casilla de la ruleta, definida por un número y un color asociado.
 * La clase es inmutable.
 */
public class Casilla {
	
	
	// Enumeración para definir los posibles colores

	private final int numero;
	private final COLOR color;
	
	/**
	 * Constructor: Inicializa la casilla y asigna su color basado en las reglas de la ruleta.
	 * PRE: 'num' debe ser un número entero >= 0.
	 */
	public Casilla(int num) {
		
		this.numero = num;
		
		if (num == 0) {
			this.color = COLOR.VERDE;
		} else if ((num >= 1 && num <= 10) || (num >= 19 && num <= 28)) {
			// Reglas comunes: 1-10 y 19-28: Impares son Rojos, Pares son Negros
			this.color = (num % 2 != 0) ? COLOR.ROJO : COLOR.NEGRO;
		} else if ((num >= 11 && num <= 18) || (num >= 29 && num <= 36)) {
			// Reglas comunes: 11-18 y 29-36: Impares son Negros, Pares son Rojos
			this.color = (num % 2 != 0) ? COLOR.NEGRO : COLOR.ROJO;
		} else {
             // Fallback, aunque los números de ruleta están entre 0 y 36
             this.color = COLOR.VERDE;
        }
	}

	
	// --- GETTERS ---
	
	public int getNumero() {
		return numero;
	}

	public String getColor() {
		return color.toString();
	}

	/**
	 * POST: Devuelve la docena a la que pertenece el número (1, 2, 3) o 0 si es el 0.
	 */
	public int getDocena() {
		if (this.numero >= 1 && this.numero <= 12) {
			return 1;
		}
		if (this.numero >= 13 && this.numero <= 24) {
			return 2;
		}
		if (this.numero >= 25 && this.numero <= 36) {
			return 3;
		}
		return 0; // Para el 0
	}
    
    // --- MÉTODOS DE OBJETO ---

    /**
     * Devuelve una representación en cadena del objeto Casilla.
     */
    @Override
    public String toString() {
    	 	
    	return "[" + numero + " - " + color.toString() + " - DOCENA:" + this.getDocena()+"]";
    	
    }

    /**
     * Comprueba si dos objetos Casilla son iguales, basándose en su número.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Casilla otraCasilla = (Casilla) obj;
        
        // Una casilla es igual a otra si su número y color coinciden.
        return numero == otraCasilla.numero && color == otraCasilla.color;
    }

    /**
     * Devuelve un valor hash consistente con el método equals().
     */
    @Override
    public int hashCode() {
        return Objects.hash(numero, color);
    }
}