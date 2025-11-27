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
 *  - Si la conexión se pierde, se informa al usuario y se cierra el socket.
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
     * PRE: ip != null, puerto válido (>1024).
     * POST: Se crea un socket conectado al servidor, o se informa de error si no se pudo conectar.
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
     * PRE: El socket debe estar conectado y no cerrado.
     * POST:
     *  - Se leen mensajes del servidor en bucle.
     *  - Si el servidor solicita respuesta ("NECESITO RESPUESTA"), se espera entrada del usuario.
     *  - El usuario tiene 30 segundos para responder; si no lo hace, se informa de timeout.
     *  - Los mensajes informativos del servidor se muestran directamente en consola.
     *  - Si la conexión se pierde, se informa y se cierra el socket.
     */
    public void IniciarCliente() {
        if (socket == null || socket.isClosed()) return;

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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
                        respuesta = future.get(30, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        System.out.println("⏳ Tiempo de espera agotado (30s). No se recibió respuesta.");
                        future.cancel(true);
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
            try { if (socket != null) socket.close(); } catch (IOException e) {}
        }
    }
}
