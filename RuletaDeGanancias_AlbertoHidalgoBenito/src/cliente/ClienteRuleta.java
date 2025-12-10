package cliente;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import modeloDominio.Apuesta;
import modeloDominio.Jugador;
import modeloDominio.TipoApuesta;

/**
 * Clase ClienteRuleta
 * -------------------
 * Controlador principal del cliente (Consola).
 * Gestiona la conexión TCP, la interfaz de usuario por texto y la sincronización con el estado del juego.
 */
public class ClienteRuleta {

    // --- ATRIBUTOS DE CONEXIÓN ---
    private Socket socket;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private Jugador jugador;

    // --- SINCRONIZACIÓN ---
    private volatile CountDownLatch latchEspera = new CountDownLatch(1);
    private volatile boolean isNoVaMas = true; 

    // --- CONSTRUCTOR ---
    public ClienteRuleta(String ip, int puerto) {
        try {
            this.socket = new Socket(ip, puerto);
        } catch (IOException e) {
            System.err.println("❌ No se pudo conectar con el servidor en " + ip + ":" + puerto);
            System.exit(1);
        }
        this.jugador = new Jugador();
    }

    // --- ENTRY POINT ---
    public static void main(String[] args) {
        new ClienteRuleta("localhost", 8000).IniciarCliente();
    }

    // --- LÓGICA PRINCIPAL ---
    public void IniciarCliente() {
        if (this.socket == null || this.socket.isClosed()) return;

        try {
            ObjectOutputStream out = new ObjectOutputStream(this.socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(this.socket.getInputStream());
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("✅ Conectado al Casino.");

            // 1. HANDSHAKE Y SESIÓN (Validado)
            if (!gestionarSesion(in, out, teclado)) {
                return; // Si falla el login, cerramos
            }
            
            // 2. SINCRONIZACIÓN INICIAL DE ESTADO
            this.isNoVaMas = in.readBoolean();
            
            if (this.isNoVaMas) {
                this.latchEspera = new CountDownLatch(1); 
                System.out.println("ℹ️ La mesa está girando. Espera a la siguiente ronda...");
            } else {
                if(this.latchEspera != null) this.latchEspera.countDown(); 
            }

            // 3. ARRANCAR HILO ESCUCHA
            pool.execute(new mostrarYLeerServidor(in, this.jugador, this));

            // 4. BUCLE DE MENÚ PRINCIPAL
            while (!this.socket.isClosed()) {
                mostrarMenuPrincipal();
                String seleccion = teclado.readLine();
                
                if (seleccion == null) break; 

                switch (seleccion) {
                    case "1":
                        opcionAnadirSaldo(out, teclado);
                        break;
                    case "2":
                        opcionJugar(out, teclado);
                        break;
                    case "3":
                        out.writeObject("SALIR"); 
                        out.flush();
                        System.out.println("¡Hasta pronto!");
                        return;
                    default:
                        System.out.println("❌ Opción incorrecta.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error en cliente: " + e.getMessage());
        } finally {
            this.desconectar();
        }
    }

    // --- MÉTODOS DE CONTROL (Sincronización) ---

    public void cerrarMesa() {
        this.isNoVaMas = true;
        this.latchEspera = new CountDownLatch(1); 
    }

    public void abrirMesa() {
        this.isNoVaMas = false;
        if (this.latchEspera != null) {
            this.latchEspera.countDown(); 
        }
    }

    // --- LÓGICA DE JUEGO ---

    private void opcionJugar(ObjectOutputStream out, BufferedReader teclado) {
        try {
            System.out.println("⏳ Entrando a la mesa... (Esperando apertura)");
            latchEspera.await(); 

            while (!isNoVaMas) {
                System.out.println("\nEscribe '1' para Apostar o 'fin' para volver al menú:");
                String linea = teclado.readLine();

                if (isNoVaMas) {
                    System.out.println("⛔ ¡NO VA MÁS! Mesa cerrada.");
                    break; 
                }

                if ("fin".equalsIgnoreCase(linea)) break;

                if ("1".equals(linea)) {
                    Apuesta apuesta = crearApuesta(teclado);
                    
                    if (isNoVaMas) {
                        System.out.println("⛔ ¡NO VA MÁS! No dio tiempo a enviar.");
                        break;
                    }

                    if (apuesta != null) {
                        out.writeObject(apuesta);
                        out.flush();
                        System.out.println("📨 Enviando apuesta...");
                    } else {
                        continue;
                    }
                }
            }
        } catch (InterruptedException | IOException e) {
            System.out.println("Interrupción en juego.");
        }
    }

    /**
     * Asistente para crear un objeto Apuesta validado.
     */
    public Apuesta crearApuesta(BufferedReader teclado) throws IOException {
        // 1. CHEQUEO INICIAL DE MESA CERRADA
        if (isNoVaMas) return null;

        // 2. VALIDACIONES DE USUARIO (Sesión y Saldo mínimo)
        if (jugador == null) {
            System.out.println("❌ Error: No hay sesión iniciada.");
            return null;
        }
        
        if (this.jugador.getSaldo() < 5) {
            System.out.println("❌ No tienes saldo suficiente para la apuesta mínima (5€).");
            return null;
        }

        System.out.println("\n--- NUEVA APUESTA ---");
        System.out.println("Saldo actual: " + jugador.getSaldo() + "€");

        // =====================================================================
        // 1. PEDIR CANTIDAD
        // =====================================================================
        double cantidad = 0;
        boolean cantidadValida = false;

        while (!cantidadValida) {
            if (isNoVaMas) return null; // Si cierran mesa, salimos

            System.out.println("¿Cuánto quieres apostar? (Mín 5€ - Máx 10.000€) [0 para Cancelar]");
            
            String entrada = teclado.readLine();
            
            // Si cierran mesa mientras escribía o corta conexión
            if (isNoVaMas || entrada == null) return null;

            try {
                cantidad = Double.parseDouble(entrada);

                // Opción de cancelar
                if (cantidad == 0) {
                    System.out.println("⚠️ Apuesta cancelada.");
                    return null;
                }

                if (cantidad < 5) {
                    System.out.println("❌ La cantidad mínima es 5€.");
                } else if (cantidad > 10000) {
                    System.out.println("❌ El máximo permitido es 10.000€.");
                } else if (cantidad > jugador.getSaldo()) {
                    System.out.println("❌ No tienes suficiente saldo.");
                } else {
                    cantidadValida = true;
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Introduce un número válido.");
            }
        }

        // =====================================================================
        // 2. PEDIR TIPO DE APUESTA
        // =====================================================================
        System.out.println("¿Qué tipo de apuesta quieres hacer?");
        System.out.println("1- NUMERO (Pleno)");
        System.out.println("2- COLOR");
        System.out.println("3- PAR / IMPAR");
        System.out.println("4- DOCENA");

        TipoApuesta tipoSeleccionado = null;
        while (tipoSeleccionado == null) {
            if (isNoVaMas) return null;

            System.out.print("Opción > ");
            try {
                String s = teclado.readLine();
                if (isNoVaMas || s == null) return null;

                int op = Integer.parseInt(s);
                if (op >= 1 && op <= 4) {
                    tipoSeleccionado = TipoApuesta.values()[op - 1];
                } else {
                    System.out.println("❌ Elige entre 1 y 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Introduce un número válido.");
            }
        }

        // =====================================================================
        // 3. PEDIR VALOR ESPECÍFICO 
        // =====================================================================
        String valorApostado = "";
        boolean valorValido = false;

        while (!valorValido) {
            if (isNoVaMas) return null;

            try {
                switch (tipoSeleccionado) {
                    case NUMERO:
                        System.out.println("Elige número (0-36):");
                        String lineaNum = teclado.readLine();
                        if (isNoVaMas || lineaNum == null) return null;

                        int num = Integer.parseInt(lineaNum);
                        
                        // --- VALIDACIÓN DE RANGO ---
                        if (num >= 0 && num <= 36) {
                            valorApostado = String.valueOf(num);
                            valorValido = true;
                        } else {
                            System.out.println("❌ El número debe estar entre 0 y 36.");
                        }
                        break;

                    case COLOR:
                        System.out.println("Elige color (ROJO / NEGRO):");
                        String color = teclado.readLine();
                        if (isNoVaMas || color == null) return null;

                        color = color.toUpperCase().trim();
                        if (color.equals("ROJO") || color.equals("NEGRO")) {
                            valorApostado = color;
                            valorValido = true;
                        } else {
                            System.out.println("❌ Escribe ROJO o NEGRO.");
                        }
                        break;

                    case PAR_IMPAR:
                        System.out.println("Elige paridad (PAR / IMPAR):");
                        String paridad = teclado.readLine();
                        if (isNoVaMas || paridad == null) return null;

                        paridad = paridad.toUpperCase().trim();
                        if (paridad.equals("PAR") || paridad.equals("IMPAR")) {
                            valorApostado = paridad;
                            valorValido = true;
                        } else {
                            System.out.println("❌ Escribe PAR o IMPAR.");
                        }
                        break;

                    case DOCENA:
                        System.out.println("Elige docena (1, 2 o 3):");
                        String docena = teclado.readLine();
                        if (isNoVaMas || docena == null) return null;

                        docena = docena.trim();
                        if (docena.equals("1") || docena.equals("2") || docena.equals("3")) {
                            valorApostado = docena;
                            valorValido = true;
                        } else {
                            System.out.println("❌ Escribe 1, 2 o 3.");
                        }
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Formato incorrecto.");
            }
        }

        // Chequeo final
        if (isNoVaMas) return null;

        return new Apuesta(jugador, tipoSeleccionado, valorApostado, cantidad);
    }

    // --- MÉTODOS AUXILIARES ---

    private void opcionAnadirSaldo(ObjectOutputStream out, BufferedReader teclado) throws IOException {
        if (jugador == null) return;
        System.out.println("¿Cuánto dinero quieres ingresar?");
        
        String cantStr = teclado.readLine();
        if (cantStr == null) return;

        try {
            double cantidad = Double.parseDouble(cantStr);
            if (cantidad <= 0 || cantidad > 10000) {
                System.out.println("⚠️ Cantidad inválida (Máx 10.000€).");
            } else {
                out.writeObject(Double.valueOf(cantidad));
                out.flush();
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Error: Introduce un número válido.");
        }
    }

    private void mostrarMenuPrincipal() {
        System.out.println("\n--- MENÚ PRINCIPAL ---");
        System.out.println("1. Añadir saldo");
        System.out.println("2. Jugar (Entrar a Mesa)");
        System.out.println("3. Salir");
        System.out.print("> ");
    }

    // --- GESTIÓN DE SESIÓN (VALIDACIONES RESTAURADAS) ---

    private boolean gestionarSesion(ObjectInputStream in, ObjectOutputStream out, BufferedReader teclado) throws IOException, ClassNotFoundException {
        System.out.println("=== BIENVENIDO AL CASINO ===");
        
        String opcion = "";
        // Validación local de opción antes de enviar al servidor
        while (!"1".equals(opcion) && !"2".equals(opcion)) {
            System.out.println("1. Iniciar Sesion");
            System.out.println("2. Registrarse");
            System.out.print("> ");
            opcion = teclado.readLine();
            if (opcion == null) return false;
            if (!"1".equals(opcion) && !"2".equals(opcion)) {
                System.out.println("❌ Opción inválida.");
            }
        }

        out.writeObject(opcion); // Handshake
        out.flush();

        if ("1".equals(opcion)) {
            return iniciarSesion(in, out, teclado);
        } else {
            return registrarSesion(in, out, teclado);
        }
    }

    private boolean iniciarSesion(ObjectInputStream in, ObjectOutputStream out, BufferedReader teclado) throws IOException, ClassNotFoundException {
        String usuario = "";
        // Validación local de ID no vacío
        while (usuario.trim().isEmpty()) {
            System.out.print("Usuario: ");
            usuario = teclado.readLine();
            if (usuario == null) return false;
        }

        out.writeObject(usuario);
        out.flush();

        Object respuesta = in.readObject();
        
        // Validación de tipo de respuesta del servidor
        if (respuesta instanceof Jugador) {
            this.jugador = (Jugador) respuesta;
            System.out.println("✅ Login correcto. Hola " + jugador.getID());
            return true;
        } else if (respuesta instanceof String) {
            System.out.println("❌ Error del servidor: " + respuesta);
            return false;
        } else {
            System.out.println("❌ Respuesta desconocida del servidor.");
            return false;
        }
    }

    private boolean registrarSesion(ObjectInputStream in, ObjectOutputStream out, BufferedReader teclado) throws IOException, ClassNotFoundException {
        System.out.println("--- REGISTRO ---");
        
        // Validación ID
        String id = "";
        while (id.trim().isEmpty()) {
            System.out.print("Nuevo Usuario: ");
            id = teclado.readLine();
            if (id == null) return false;
        }

        // Validación Saldo Numérico
        double saldo = -1;
        while (saldo < 0) {
            System.out.print("Saldo inicial: ");
            try {
                String input = teclado.readLine();
                if (input == null) return false;
                saldo = Double.parseDouble(input);
                if (saldo < 0) System.out.println("❌ El saldo debe ser positivo.");
            } catch(NumberFormatException e) {
                System.out.println("❌ Por favor, introduce un número válido.");
            }
        }

        out.writeObject(new Jugador(id, saldo));
        out.flush();

        Object respuesta = in.readObject();
        
        // Validación de tipo de respuesta
        if (respuesta instanceof Jugador) {
            this.jugador = (Jugador) respuesta;
            System.out.println("✅ Registro completado.");
            return true;
        } else if (respuesta instanceof String) {
            System.out.println("❌ Error en registro: " + respuesta);
            return false;
        } else {
            System.out.println("❌ Respuesta desconocida.");
            return false;
        }
    }

    private void desconectar() {
        try { 
            if (socket != null) socket.close(); 
        } catch (Exception e) {}
        
        if (pool != null) pool.shutdownNow(); 
        //Añado el system.exit, porque sino el cliente siempre se puede quedar bloqueado en un .readLine() del teclado.2
        System.exit(0);

    }
    
    public Jugador getJugador() { return this.jugador; }
}