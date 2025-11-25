package servidor;

import java.util.List;
import java.util.Map;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class guardarApuestas implements Runnable {

    private final Map<Jugador, List<Apuesta>> apuestas;
    private final Casilla ganadora;
    private final XMLServidor xml;

    public guardarApuestas(Map<Jugador, List<Apuesta>> apuestas, Casilla ganadora, XMLServidor xml) {
        this.apuestas = apuestas;
        this.ganadora = ganadora;
        this.xml = xml;
    }

    @Override
    public void run() {
        try {
            // Persistir apuestas y resultado en XML
            xml.guardarJugadorApuesta(apuestas, ganadora);
            
        } catch (Exception e) {
            System.err.println("⚠️ Error inesperado en guardarApuestas: " + e.getMessage());
        } finally {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt(); // restaurar estado de interrupción
            }
        }
    }
}

