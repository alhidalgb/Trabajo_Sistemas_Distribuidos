package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteRuleta {

    private Socket socket;
    
    public ClienteRuleta(int puerto) {
        try {
            // Conectamos al localhost (o IP del servidor)
            this.socket = new Socket("localhost", puerto);
        } catch (IOException e) {
            System.err.println("❌ No se pudo conectar con el servidor en el puerto " + puerto);
        }
    }
    
    public void IniciarCliente() {
        if (socket == null || socket.isClosed()) return;

        try (
            // Flujo de ENTRADA del servidor (para leer mensajes)
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Flujo de SALIDA al servidor (para enviar respuestas) - autoFlush=true
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // Flujo de ENTRADA de teclado (para que el usuario escriba)
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            
            System.out.println("✅ Conectado al Casino. Esperando instrucciones...");
            
            String msgServidor;
            // Leemos continuamente lo que dice el servidor
            while ((msgServidor = in.readLine()) != null) {
                
                // PROTOCOLO: El servidor nos pide datos explícitamente
                if (msgServidor.equals("NECESITO RESPUESTA")) {
                    System.out.print("> "); // Prompt visual para el usuario
                    String respuesta = teclado.readLine();
                    
                    // Enviamos la respuesta al servidor
                    // println añade el salto de línea correcto automáticamente
                    out.println(respuesta); 
                    
                } else {
                    // Es un mensaje normal (informativo), solo lo mostramos
                    System.out.println(msgServidor);
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Se ha perdido la conexión con el servidor.");
        } finally {
            try { if (socket != null) socket.close(); } catch (IOException e) {}
        }
    }
    
    // Main para probarlo independientemente
    public static void main(String[] args) {
        new ClienteRuleta(8080).IniciarCliente();
    }
}