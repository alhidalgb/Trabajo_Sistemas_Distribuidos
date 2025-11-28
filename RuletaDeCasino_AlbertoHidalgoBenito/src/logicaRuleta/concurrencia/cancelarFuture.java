package logicaRuleta.concurrencia;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Clase cancelarFuture
 * --------------------
 * Tarea concurrente que cancela una lista de objetos Future.
 * Se utiliza para detener la ejecución de tareas pendientes o en curso,
 * evitando que sigan consumiendo recursos del sistema.
 *
 * PRECONDICIONES:
 *  - La lista de Future (lft) no debe ser nula.
 *  - Los objetos Future contenidos pueden estar en estado pendiente, en ejecución o finalizados.
 *
 * POSTCONDICIONES:
 *  - Todos los Future de la lista son cancelados con la opción de interrupción activada.
 *  - Si un Future ya ha terminado, la cancelación no tiene efecto.
 */
public class cancelarFuture<T> implements Runnable {

    // --- ATRIBUTOS ---
    private final List<Future<T>> lft;

    // --- CONSTRUCTOR ---
    /**
     * Inicializa la tarea con la lista de Future a cancelar.
     *
     * @param lft Lista de objetos Future (no nula).
     */
    public cancelarFuture(List<Future<T>> lft) {
        this.lft = lft;
    }

    // --- LÓGICA DE NEGOCIO ---
    /**
     * Cancela todos los Future de la lista.
     * 
     * PRE: La lista no debe ser nula.
     * POST: Cada Future es cancelado con interrupción (cancel(true)).
     */
    @Override
    public void run() {
    	
    
        if (lft == null || lft.isEmpty()) {
            return;
        }

        for (Future<T> ft : lft) {
        	
        	//Si se cancela por algun motivo salimos del bucle.
        	if(Thread.currentThread().isInterrupted()) {return;}
        	
            if (ft != null) {
                ft.cancel(true);
            }
        }
    }
}
