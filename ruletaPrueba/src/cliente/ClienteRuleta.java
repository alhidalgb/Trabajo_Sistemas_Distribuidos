package cliente;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import modeloDominio.Apuesta;
import modeloDominio.Jugador;
import modeloDominio.TipoApuesta;

public class ClienteRuleta {

    private Socket socket;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private Jugador jugador;

    // --- SINCRONIZACIÓN ---
    // Volatile asegura que los cambios hechos por el hilo escucha sean visibles inmediatamente
    private volatile CountDownLatch latchEspera = new CountDownLatch(1);
    private volatile boolean isNoVaMas = true; 

    public ClienteRuleta(String ip, int puerto) {
        try {
            this.socket = new Socket(ip, puerto);
        } catch (IOException e) {
            System.err.println("❌ No se pudo conectar con el servidor en " + ip + ":" + puerto);
        }
        this.jugador = new Jugador();
    }

    public static void main(String[] args) {
        new ClienteRuleta("localhost", 8000).IniciarCliente();
    }

    public void IniciarCliente() {
        if (this.socket == null || this.socket.isClosed()) return;

        try {
            // Importante: Crear Output antes que Input para evitar bloqueo de cabeceras
            ObjectOutputStream out = new ObjectOutputStream(this.socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(this.socket.getInputStream());
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("✅ Conectado al Casino.");

            // 1. SESIÓN (Protocolo estricto inicial)
            // Aquí sí enviamos "1" o "2" porque el servidor espera ese handshake inicial
            this.Sesion(in, out, teclado);
            
            // 2. SINCRONIZACIÓN INICIAL
            // El servidor nos dice inmediatamente cómo está la mesa
            this.isNoVaMas = in.readBoolean();
            
            if (this.isNoVaMas) {
                // Mesa cerrada: Bloqueamos
                this.latchEspera = new CountDownLatch(1);
                System.out.println("ℹ️ La mesa está girando. Espera a la siguiente ronda...");
            } else {
                // Mesa abierta: Desbloqueamos
                if(this.latchEspera != null) this.latchEspera.countDown();
            }

            // 3. ARRANCAR HILO ESCUCHA
            // Se encargará de recibir mensajes, premios y señales de NO_VA_MAS / ABRIR_MESA
            pool.execute(new mostrarYLeerServidor(in, this.jugador, this));

            // 4. BUCLE PRINCIPAL
            boolean salir = false;
            while (!salir && !this.socket.isClosed()) {
                System.out.println("\n--- MENÚ PRINCIPAL ---");
                System.out.println("1. Añadir saldo");
                System.out.println("2. Jugar (Esperar ronda)");
                System.out.println("3. Salir");
                System.out.print("> ");

                String seleccion = teclado.readLine();
                if (seleccion == null) break;

                // NOTA: No enviamos la selección al servidor. 
                // El servidor reaccionará al TIPO de objeto que enviemos dentro de cada opción.

                switch (seleccion) {
                    case "1":
                        // Enviaremos un objeto Double
                        opcionAnadirSaldo(out, teclado);
                        break;
                    case "2":
                        // Entramos en el bucle local de apuestas (enviaremos objetos Apuesta)
                        opcionJugar(out, teclado);
                        break;
                    case "3":
                        // Aquí enviamos el comando explícito de salida
                        out.writeObject("SALIR");
                        out.flush();
                        System.out.println("¡Hasta pronto!");
                        salir = true;
                        break;
                    default:
                        System.out.println("❌ Opción incorrecta.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.desconectar();
        }
    }

    // --- MÉTODOS DE CONTROL (Llamados por mostrarYLeerServidor) ---

    public void cerrarMesa() {
        this.isNoVaMas = true;
        this.latchEspera = new CountDownLatch(1); // Echamos el cerrojo
    }

    public void abrirMesa() {
        this.isNoVaMas = false;
        if (this.latchEspera != null) {
            this.latchEspera.countDown(); // Abrimos el cerrojo
        }
    }

    // --- LÓGICA DE JUEGO ---

    private void opcionJugar(ObjectOutputStream out, BufferedReader teclado) {
        try {
            System.out.println("⏳ Entrando a la mesa... (Esperando apertura)");
            
            // 1. BLOQUEO: Si la mesa está cerrada, el hilo se duerme aquí
            latchEspera.await(); 

            // 2. MESA ABIERTA: El hilo despierta
            // El mensaje visual "HAGAN JUEGO" lo imprime el Hilo Escucha

            // 3. BUCLE DE APUESTAS
            while (!isNoVaMas) {
                System.out.println("\nEscribe '1' para Apostar o 'fin' para volver al menú:");
                
                // Lectura bloqueante (espera a que el usuario escriba)
                String linea = teclado.readLine();

                // Check post-lectura: ¿Se cerró la mesa mientras escribía?
                if (isNoVaMas) {
                    System.out.println("⛔ ¡NO VA MÁS! Mesa cerrada.");
                    break; 
                }

                if ("fin".equalsIgnoreCase(linea)) break;

                if ("1".equals(linea)) {
                    Apuesta apuesta = crearApuesta(teclado);
                    
                    // Último check antes de enviar
                    if (isNoVaMas) {
                        System.out.println("⛔ ¡NO VA MÁS! No dio tiempo a enviar.");
                        break;
                    }

                    if (apuesta != null) {
                        // Enviamos OBJETO APUESTA -> Servidor detecta instanceof Apuesta
                        out.writeObject(apuesta);
                        out.flush();
                        System.out.println("📨 Enviando apuesta...");
                    }
                }
            }
            // Al salir del bucle, volvemos al menú principal
            
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public Apuesta crearApuesta(BufferedReader teclado) throws IOException {
        if (isNoVaMas) return null;

        System.out.println("--- NUEVA APUESTA ---");
        System.out.println("Saldo disponible: " + jugador.getSaldo() + "€");

        // 1. CANTIDAD
        double cantidad = 0;
        boolean cantidadValida = false;

        while (!cantidadValida) {
            if (isNoVaMas) return null;

            System.out.println("Cantidad a apostar:");
            String entrada = teclado.readLine();

            if (isNoVaMas) return null;
            if (entrada == null) return null;

            try {
                cantidad = Double.parseDouble(entrada);
                if (cantidad >= 5 && cantidad <= jugador.getSaldo()) {
                    cantidadValida = true;
                } else {
                    System.out.println("❌ Cantidad inválida (Min 5€) o saldo insuficiente.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Introduce un número.");
            }
        }

        // 2. TIPO
        TipoApuesta tipo = null;
        while (tipo == null) {
            if (isNoVaMas) return null;
            
            System.out.println("Tipo: 1-NUMERO, 2-COLOR, 3-PAR/IMPAR, 4-DOCENA");
            String s = teclado.readLine();
            
            if (isNoVaMas) return null;
            
            try {
                int op = Integer.parseInt(s);
                if (op >= 1 && op <= 4) tipo = TipoApuesta.values()[op - 1];
                else System.out.println("❌ Opción inválida.");
            } catch (Exception e) { System.out.println("❌ Error formato."); }
        }

        // 3. VALOR
        String valor = "";
        while (valor.isEmpty()) {
            if (isNoVaMas) return null;
            
            System.out.println("Valor (ej: ROJO, 14, PAR):");
            String s = teclado.readLine();
            
            if (isNoVaMas) return null;
            if (s != null && !s.trim().isEmpty()) valor = s.toUpperCase();
        }

        if (isNoVaMas) return null;

        return new Apuesta(jugador, tipo, valor, cantidad);
    }

    // --- AÑADIR SALDO (Protocolo Polimórfico) ---

    private void opcionAnadirSaldo(ObjectOutputStream out, BufferedReader teclado) throws IOException {
        if (jugador == null) return;

        System.out.println("¿Cuánto dinero quieres ingresar?");
        
        while (!this.socket.isClosed()) {
            try {
                String cantStr = teclado.readLine();
                if (cantStr == null) return;

                double cantidad = Double.parseDouble(cantStr);

                if (cantidad <= 0 || cantidad > 10000) {
                    System.out.println("⚠️ Cantidad inválida (Máx 10.000€).");
                } else {
                    // CAMBIO CLAVE: Enviamos un OBJETO Double
                    // El servidor detectará: if (mensaje instanceof Double)
                    out.writeObject(Double.valueOf(cantidad));
                    out.flush();
                    break; 
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Error: Introduce un número válido.");
            }
        }
    }

    // --- LOGIN / REGISTRO ---

    private void Sesion(ObjectInputStream in, ObjectOutputStream out, BufferedReader teclado) throws IOException, ClassNotFoundException {
        System.out.println("=== BIENVENIDO AL CASINO ===");
        System.out.println("1. Iniciar Sesion");
        System.out.println("2. Registrarse");
        System.out.print("> ");

        String opcion = teclado.readLine();
        // Enviamos la opción como String (Handshake inicial)
        out.writeObject(opcion);
        out.flush();

        if ("1".equals(opcion)) {
            if (!this.iniciarSesion(in, out, teclado)) {
                System.out.println("Fallo al iniciar sesión. Saliendo...");
                this.desconectar();
            }
        } else {
            if (!registrarSesion(in, out, teclado)) {
                System.out.println("Fallo al registrar. Saliendo...");
                this.desconectar();
            }
        }
    }

    private boolean iniciarSesion(ObjectInputStream in, ObjectOutputStream out, BufferedReader teclado) throws IOException, ClassNotFoundException {
        System.out.println("Usuario:");
        String usuario = teclado.readLine();
        // Enviamos String (ID)
        out.writeObject(usuario);
        out.flush();

        Object respuesta = in.readObject();
        if (respuesta instanceof Jugador) {
            this.jugador = (Jugador) respuesta;
            System.out.println("✅ Login correcto. Hola " + jugador.getID());
            return true;
        }
        return false;
    }

    private boolean registrarSesion(ObjectInputStream in, ObjectOutputStream out, BufferedReader teclado) throws IOException, ClassNotFoundException {
        System.out.println("--- REGISTRO ---");
        System.out.println("Nuevo Usuario:");
        String id = teclado.readLine();
        System.out.println("Saldo inicial:");
        double saldo = Double.parseDouble(teclado.readLine());

        // Enviamos Objeto Jugador (Datos registro)
        out.writeObject(new Jugador(id, saldo));
        out.flush();

        Object respuesta = in.readObject();
        if (respuesta instanceof Jugador) {
            this.jugador = (Jugador) respuesta;
            System.out.println("✅ Registro completado.");
            return true;
        }
        return false;
    }

    private void desconectar() {
        try { if (socket != null) socket.close(); } catch (Exception e) {}
        pool.shutdownNow();
    }
    
    // Getter necesario para el Hilo Escucha
    public Jugador getJugador() { return this.jugador; }
}