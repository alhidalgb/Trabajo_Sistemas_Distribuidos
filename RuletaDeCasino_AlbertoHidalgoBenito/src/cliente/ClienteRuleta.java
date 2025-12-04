package cliente;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class ClienteRuleta {

    private Socket socket;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ClienteRuleta(String ip, int puerto) {
        try {
            this.socket = new Socket(ip, puerto);
        } catch (IOException e) {
            System.err.println("❌ No se pudo conectar con el servidor en " + ip + ":" + puerto);
        }
    }

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
                    System.out.print("> ");
                    Future<String> future = executor.submit(() -> teclado.readLine());

                    String respuesta = null;
                    try {
                        respuesta = future.get(3, TimeUnit.SECONDS); // ⏳ 30 segundos
                    } catch (TimeoutException e) {
                        System.out.println("⏳ Tiempo de espera agotado (30s). Se cerrará la conexión.");
                        future.cancel(true);
                        
                        return;
                    } catch (Exception e) {
                        System.err.println("⚠️ Error leyendo respuesta: " + e.getMessage());
                    }

                    if (respuesta != null) {
                        out.println(respuesta);
                    }

                } else {
                    System.out.println(msgServidor);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Se ha perdido la conexión con el servidor.");
        } finally {
            this.cerrarConexion();
       
        }
    }

    private void cerrarConexion() {
        try {
        	
    
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
                System.out.println("🔒 Conexión cerrada con el servidor.");
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error cerrando socket: " + e.getMessage());
        } finally {
            executor.shutdownNow(); // 🔒 cerramos el pool de hilos
        }
        //System.exit(0); // cerramos todo el programa
    }

    public static void main(String[] args) {
        new ClienteRuleta("localhost", 8000).IniciarCliente();
    }
}
