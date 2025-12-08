package cliente;

import java.io.IOException;
import java.io.ObjectInputStream;
import modeloDominio.Jugador;

public class mostrarYLeerServidor implements Runnable {

    private final ObjectInputStream in;
    private final Jugador jugador;
    private final ClienteRuleta cliente; // Referencia al controlador principal

    // Constructor: Recibimos el ClienteRuleta para poder comunicarnos con él
    public mostrarYLeerServidor(ObjectInputStream in, Jugador jugador, ClienteRuleta cliente) {
        this.in = in;
        this.jugador = jugador;
        this.cliente = cliente;
    }

    @Override
    public void run() {
        try {
            Object mensaje;
            // Bucle infinito leyendo objetos del servidor
            while ((mensaje = in.readObject()) != null) {
                
                if (mensaje instanceof String) {
                    String texto = (String) mensaje;

                    // --- SINCRONIZACIÓN DE ESTADOS ---
                    // Usamos las constantes definidas en el servidor: NO_VA_MAS / ABRIR_MESA
                    
                    if (texto.contains("NO_VA_MAS") || texto.contains("NO VA MAS")) {
                        // Avisamos al ClienteRuleta para que cierre SU puerta
                    		this.cliente.cerrarMesa();
                    		
                        System.out.println("\n>>> ⛔ ¡NO VA MÁS! <<<");
                        System.out.print("> "); // Prompt visual
                    } 
                    else if (texto.contains("ABRIR_MESA") || texto.contains("ABRIR MESA")) {
                        // Avisamos al ClienteRuleta para que abra SU puerta
                        this.cliente.abrirMesa();
                        
                        System.out.println("\n>>> 🟢 ¡HAGAN JUEGO! (Mesa Abierta) <<<");
                        System.out.print("> ");
                    }
                    
                    // --- ACTUALIZACIÓN DE SALDO ---
                    else if (texto.startsWith("actualizar saldo:")) {
                        try {
                            String[] partes = texto.split(":");
                            // Usamos Double por si acaso, aunque el servidor mande int
                            double cantidad = Double.parseDouble(partes[1].trim());
                            
                            synchronized(this.jugador) {
                                 this.jugador.sumaRestaSaldo(cantidad);
                            }
                        } catch (Exception e) {
                            System.out.println("Error procesando saldo: " + e.getMessage());
                        }
                    } 
                    
                    // --- MENSAJES GENÉRICOS ---
                    else {
                        System.out.println("\n(Servidor): " + texto);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Desconectado del servidor.");
            // Liberamos al cliente por si estaba bloqueado esperando
            cliente.abrirMesa(); 
            System.exit(0);
        }
    }
}