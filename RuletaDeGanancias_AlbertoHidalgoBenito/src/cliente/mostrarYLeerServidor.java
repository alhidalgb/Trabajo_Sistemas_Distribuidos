package cliente;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import modeloDominio.Jugador;

/**
 * Clase mostrarYLeerServidor
 * --------------------------
 * Hilo "Listener" del cliente. Se encarga de recibir mensajes asíncronos del servidor
 * y actualizar el estado local del juego (saldo, barreras, mensajes de chat).
 */
public class mostrarYLeerServidor implements Runnable {

    private final ObjectInputStream in;
    private final Jugador jugador;
    private final ClienteRuleta cliente; // Controlador principal

    public mostrarYLeerServidor(ObjectInputStream in, Jugador jugador, ClienteRuleta cliente) {
        this.in = in;
        this.jugador = jugador;
        this.cliente = cliente;
    }

    @Override
    public void run() {
        try {
            Object mensaje;
            
            // Bucle infinito de lectura bloqueante
            while ((mensaje = in.readObject()) != null) {
                
                if (mensaje instanceof String) {
                    procesarMensajeTexto((String) mensaje);
                } 
                // Aquí podrías procesar otros objetos si el servidor los mandara (ej. List<Jugador>)
            }
        } catch (EOFException e) {
            // Cierre normal de conexión por parte del servidor
            System.out.println("\n--- El servidor ha cerrado la conexión ---");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("\n❌ Error de conexión: " + e.getMessage());
        } finally {
            // Pase lo que pase, desbloqueamos al cliente para que no se quede colgado
            cliente.abrirMesa(); 
        }
    }

    /**
     * Procesa los comandos de texto que vienen del servidor.
     */
    private void procesarMensajeTexto(String texto) {
        String textoUpper = texto.toUpperCase();

        // 1. SINCRONIZACIÓN DE ESTADOS (Mesa Cerrada)
        if (textoUpper.contains("NO_VA_MAS") || textoUpper.contains("NO VA MAS") || textoUpper.contains("NO VA MÁS")) {
            this.cliente.cerrarMesa(); // Bloquea al usuario local
            imprimirMensaje(texto);
        } 
        
        // 2. SINCRONIZACIÓN DE ESTADOS (Mesa Abierta)
        else if (textoUpper.contains("ABRIR_MESA") || textoUpper.contains("ABRIR MESA")) {
            this.cliente.abrirMesa(); // Desbloquea al usuario
            imprimirMensaje(texto);
        }
        
        // 3. ACTUALIZACIÓN TÉCNICA DE SALDO
        else if (texto.toLowerCase().startsWith("actualizar saldo:")) {
            try {
                String[] partes = texto.split(":");
                double cantidad = Double.parseDouble(partes[1].trim());
                
                // Actualizamos el modelo local
                synchronized(this.jugador) {
                    this.jugador.sumaRestaSaldo(cantidad);
                }
                // No imprimimos nada aquí, el servidor ya manda otro mensaje visual después
            } catch (Exception e) {
                // Error de formato, ignoramos
            }
        } 
        
        // 4. MENSAJES GENÉRICOS (Chat, Premios, Errores)
        else {
            imprimirMensaje("\n(Servidor): " + texto);
        }
    }

    /**
     * Imprime mensajes intentando respetar el prompt del usuario.
     */
    private void imprimirMensaje(String msg) {
        // \r borra la línea actual (el prompt "> "), imprime el mensaje y vuelve a poner el prompt
        System.out.println("\r" + msg);
        System.out.print("> "); 
    }
}