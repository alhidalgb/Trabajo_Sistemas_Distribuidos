package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Clase ClienteRuleta
 * -------------------
 * Representa el cliente que se conecta al servidor de la ruleta.
 * Gestiona la comunicación bidireccional con el servidor:
 *  - Recibe mensajes informativos.
 *  - Responde a solicitudes explícitas del servidor.
 *  - Permite al usuario introducir datos desde teclado.
 *
 * PRECONDICIONES:
 *  - El servidor debe estar activo y escuchando en la IP y puerto indicados.
 *  - El cliente debe tener acceso a la red y permisos para abrir sockets.
 *
 * POSTCONDICIONES:
 *  - Se establece una conexión con el servidor.
 *  - Se reciben y muestran mensajes del servidor.
 *  - Se envían respuestas al servidor cuando son solicitadas.
 *  - Si la conexión se pierde o el usuario no responde en 30s, se cierra el socket y el programa.
 */
public class ClienteRuleta {

    // --- ATRIBUTOS ---
    private Socket socket;

    // --- CONSTRUCTOR ---
    /**
     * Constructor que intenta conectar al servidor en la IP y puerto indicados.
     *
     * @param ip     Dirección IP o hostname del servidor.
     * @param puerto Puerto TCP donde escucha el servidor.
     */
    public ClienteRuleta(String ip, int puerto) {
        try {
            this.socket = new Socket(ip, puerto);
        } catch (IOException e) {
            System.err.println("❌ No se pudo conectar con el servidor en " + ip + ":" + puerto);
        }
    }

    // --- LÓGICA DE NEGOCIO ---
    /**
     * Inicia la comunicación con el servidor.
     * Si el usuario no responde en 30 segundos, se cierra la conexión y el programa termina.
     */
    public void IniciarCliente() {
        if (this.socket == null || this.socket.isClosed()) return;

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("✅ Conectado al Casino. Esperando instrucciones...");

            String msgServidor;
            while ((msgServidor = in.readLine()) != null) {

                if (msgServidor.equals("NECESITO RESPUESTA")) {
                    System.out.print("> "); // Prompt visual para el usuario
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<String> future = executor.submit(() -> teclado.readLine());

                    String respuesta = null;
                    try {
                        // ⏳ Esperamos hasta 30 segundos
                        respuesta = future.get(30, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        System.out.println("⏳ Tiempo de espera agotado (30s). Se cerrará la conexión.");
                        future.cancel(true);
                        this.cerrarConexion();
                        return; // salimos del método → termina el cliente
                    } catch (Exception e) {
                        System.err.println("⚠️ Error leyendo respuesta: " + e.getMessage());
                    } finally {
                        executor.shutdownNow();
                    }

                    if (respuesta != null) {
                        out.println(respuesta);
                    }

                } else {
                    // Mensaje informativo del servidor
                    System.out.println(msgServidor);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Se ha perdido la conexión con el servidor.");
        } finally {
            this.cerrarConexion();
        }
    }

    // --- MÉTODO AUXILIAR ---
    /**
     * Cierra el socket y termina el programa.
     */
    private void cerrarConexion() {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
                System.out.println("🔒 Conexión cerrada con el servidor.");
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error cerrando socket: " + e.getMessage());
        }
        System.exit(0); // cerramos todo el programa
    }
}