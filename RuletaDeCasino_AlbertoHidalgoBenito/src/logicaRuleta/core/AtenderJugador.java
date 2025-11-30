package logicaRuleta.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import modeloDominio.Apuesta;
import modeloDominio.Jugador;
import modeloDominio.TipoApuesta;

/**
 * Clase AtenderJugador
 * --------------------
 * Hilo que atiende la conexión de un cliente (jugador) desde su llegada hasta su desconexión.
 * Gestiona el ciclo completo de interacción: login/registro, menú principal y juego en la ruleta.
 *
 * RESPONSABILIDADES:
 *  - Autenticación: inicio de sesión o registro de nuevos jugadores.
 *  - Menú principal: añadir saldo, jugar, desconectar.
 *  - Juego: sincronización con rondas de la ruleta, creación de apuestas, espera de resultados.
 *  - Robustez: manejo de timeouts, desconexiones abruptas y errores de comunicación.
 *
 * CONCURRENCIA:
 *  - Cada instancia se ejecuta en un hilo independiente del pool del servidor.
 *  - Se sincroniza con otros jugadores mediante latches (VaMas/NoVaMas).
 *
 * TIMEOUT:
 *  - El socket tiene un timeout de 45 segundos para lectura.
 *  - Si el cliente no responde en ese tiempo, se cierra la conexión automáticamente.
 */
public class AtenderJugador implements Runnable {

    // --- ATRIBUTOS ---
    private Socket cliente;
    private ServicioRuletaServidor rule; // Lógica de negocio compartida
    private Jugador jugador; // El jugador asociado a este hilo (null hasta login/registro)

    // --- CONSTRUCTOR ---
    /**
     * Inicializa el hilo de atención al jugador.
     *
     * PRE:
     *  - cliente != null && !cliente.isClosed()
     *  - rule != null
     *
     * POST:
     *  - El hilo está listo para ejecutarse.
     *  - Si el socket es inválido, lanza IllegalArgumentException.
     *
     * @param cliente Socket de conexión con el cliente.
     * @param rule    Servicio central de la ruleta.
     * @throws IllegalArgumentException Si el socket o rule son nulos/inválidos.
     */
    public AtenderJugador(Socket cliente, ServicioRuletaServidor rule) {
        if (cliente == null || cliente.isClosed() || rule == null) {
            throw new IllegalArgumentException("Socket inválido: nulo o cerrado.");
        }
        
        this.cliente = cliente;
        this.rule = rule;
        this.jugador = null; // Se asignará en login/registro
    }

    // --- LÓGICA PRINCIPAL ---
    /**
     * Ejecuta el flujo completo de atención al jugador.
     *
     * FLUJO:
     *  1. Configurar timeout del socket (45s).
     *  2. Login/Registro: bucle hasta autenticación exitosa o desconexión.
     *  3. Menú principal: bucle hasta que el jugador elija desconectar o se pierda conexión.
     *  4. Desconexión limpia en bloque finally.
     *
     * MANEJO DE ERRORES:
     *  - SocketException: timeout agotado → cliente inactivo, se desconecta.
     *  - IOException: error de red → se desconecta.
     *  - NullPointerException: cliente cerró conexión inesperadamente → se desconecta.
     */
    @Override
    public void run() {
        // Usamos try-with-resources para asegurar que los streams se cierran al final
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true) // autoFlush activado
        ) {
            
            // El servidor se puede quedar atascado leyendo al cliente, lo que provocará un cliente fantasma.
            // El cliente solo tiene 30 segundos para mandar un mensaje, sino se cerrará la conexión.
            // Entonces el servidor solo tiene 45 segundos para intentar leer al cliente (tiene 15 segundos de gracia)
            // sino cierra conexión.
            this.cliente.setSoTimeout(45000);
            
            // --- FASE 1: LOGIN / REGISTRO ---
            boolean logueado = false;
            
            // Bucle hasta que se loguee o se desconecte
            while (!logueado) {
                out.println("=== BIENVENIDO AL CASINO ===");
                out.println("1. Iniciar Sesion");
                out.println("2. Registrarse");
                out.println("NECESITO RESPUESTA"); // Protocolo para obtener respuesta

                String opcion = safeReadLine(in, out);
                if (opcion == null) return; // Cliente cerró conexión

                if (opcion.equals("1")) {
                    logueado = iniciarSesion(in, out);
                } else {
                    // Aquí no dejamos escapar a nadie, si no inicias sesión te registras.
                    logueado = registrarSesion(in, out);
                } 
            }

            // --- FASE 2: MENÚ PRINCIPAL ---
            if (logueado) {
                boolean salir = false;

                while (!salir && !Thread.currentThread().isInterrupted()) {
                    // Mostramos el menú
                    out.println("\n--- MENÚ PRINCIPAL ---");
                    out.println("Saldo actual: " + jugador.getSaldo() + "€");
                    out.println("1. Añadir saldo");
                    out.println("2. Entrar a la Ruleta (Jugar)");
                    out.println("3. Desconectar");
                    out.println("Elige una opción:");
                    out.println("NECESITO RESPUESTA");

                    String seleccion = safeReadLine(in, out);
                    if (seleccion == null) break;

                    switch (seleccion) {
                        case "1":
                            opcionAnadirSaldo(in, out);
                            break;
                        case "2":
                            opcionJugar(in, out);
                            break;
                        case "3":
                            out.println("¡Hasta pronto!");
                            this.desconectar();
                            salir = true;
                            break;
                        default:
                            out.println("❌ Opción incorrecta.");
                    }
                }
            }
            
        } catch (SocketException e) {
            System.out.println("El servidor estuvo mucho tiempo atascado intentando leer al cliente: " + e.getMessage());
        } catch (IOException e) {
            // Este mensaje es para el servidor.
            System.out.println("Error de conexión con cliente: " + e.getMessage());
        } finally {
            this.desconectar();
        }
    }

    // --- MÉTODOS DEL MENÚ ---

    /**
     * Opción 1: Añadir saldo a la cuenta del jugador.
     *
     * PRE:
     *  - jugador != null && jugador.isSesionIniciada()
     *  - in, out != null
     *
     * POST:
     *  - Si la cantidad es válida (0 < cantidad <= 10000), se añade al saldo del jugador.
     *  - Si hay error de comunicación o sesión inválida, se desconecta al jugador.
     *
     * @param in  BufferedReader del cliente.
     * @param out PrintWriter del cliente.
     */
    private void opcionAnadirSaldo(BufferedReader in, PrintWriter out) {
        // Validación de sesión
        if (jugador == null || !jugador.isSesionIniciada()) {
            out.println("❌ No tienes sesión iniciada. Se cerrará la conexión.");
            this.desconectar();
            return;
        }

        out.println("¿Cuánto dinero quieres ingresar?");
        
        while (true) {
            try {
                out.println("NECESITO RESPUESTA");
                String cantStr = safeReadLine(in, out);
                if (cantStr == null) return; // Cliente cerró conexión

                double cantidad = Double.parseDouble(cantStr);

                if (cantidad <= 0) {
                    out.println("⚠️ La cantidad debe ser positiva.");
                } else if (cantidad > 10000) {
                    out.println("⚠️ El máximo permitido por operación es 10.000€.");
                } else {
                    jugador.setSaldo(jugador.getSaldo() + cantidad);
                    out.println("✅ Saldo añadido correctamente. Nuevo saldo: " + jugador.getSaldo() + "€");
                    break; // Salimos del bucle tras éxito
                }

            } catch (NumberFormatException e) {
                out.println("⚠️ Error: Introduce un número válido (ej: 100 o 250.5).");
            }
        }
    }

    /**
     * Opción 2: Entrar a la ruleta y realizar apuestas.
     *
     * PRE:
     *  - jugador != null && jugador.isSesionIniciada()
     *  - in, out != null
     *
     * POST:
     *  - El jugador espera a que se abra la mesa (VaMas).
     *  - Puede realizar múltiples apuestas mientras la mesa esté abierta.
     *  - Espera al resultado (NoVaMas).
     *  - Si hay error de comunicación, se desconecta.
     *
     * SINCRONIZACIÓN:
     *  - VaMasAwait(): espera a que el servidor abra la mesa.
     *  - noVaMasAwait(): espera a que el servidor cierre la mesa y reparta premios.
     *
     * @param in  BufferedReader del cliente.
     * @param out PrintWriter del cliente.
     */
    private void opcionJugar(BufferedReader in, PrintWriter out) {
        // Validación de sesión
        if (jugador == null || !jugador.isSesionIniciada()) {
            out.println("❌ No tienes sesión iniciada. Se cerrará la conexión.");
            this.desconectar();
            return;
        }

        try {
            out.println("⏳ Esperando a que se abra la mesa...");
            
            // Sincronización con la mesa 
            this.rule.VaMasAwait(); 
            out.println("--- ¡HAGAN JUEGO! (Mesa Abierta) ---");
            
            boolean seguirApostando = true;
            while (seguirApostando) {
                
                if (this.jugador.getSaldo() < 5) {
                    out.println("❌ No tienes saldo suficiente");
                    break;
                }
                
                out.println("1. Apostar");
                out.println("2. Terminar apuestas (Esperar resultado)");
                out.println("NECESITO RESPUESTA");
                
                String op = safeReadLine(in, out);
                if (op == null) return; // Cliente cerró conexión

                if (op.equals("1")) {
                    Apuesta ap = this.crearApuesta(in, out);
                    if (ap != null && this.rule.anadirApuesta(jugador, ap)) {
                        out.println("✅ Apuesta guardada con éxito.");
                    } else {
                        out.println("⚠️ No se pudo guardar la apuesta.");
                        // Solo informativo, no desconectamos
                    }
                } else {
                    out.println("Apuestas finalizadas por el jugador.");
                    seguirApostando = false;
                }
            }

            // Esperamos al resultado (sin timeout, según tu decisión)
            out.println("⏳ Esperando a que gire la bola... ");
            this.rule.noVaMasAwait();
            out.println("--- FIN DE LA RONDA ---");

        } catch (InterruptedException e) {
            out.println("❌ El hilo fue interrumpido. Se cerrará la conexión.");
            this.desconectar();
        } catch (NullPointerException e) {
            out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
            this.desconectar();
        }
    }

    // --- MÉTODOS DE LOGIN (Adaptados a PrintWriter) ---

    /**
     * Iniciar sesión con un usuario existente.
     *
     * PRE:
     *  - in, out != null
     *
     * POST:
     *  - Si el usuario existe y no tiene sesión activa, se establece la conexión.
     *  - Si no existe, se ofrece registrarse.
     *  - Si hay error, se desconecta.
     *  - Retorna true si el login fue exitoso, false en caso contrario.
     *  - Si existe y tiene la sesion activa, se ofrece registrarse.
     *
     * @param in  BufferedReader del cliente.
     * @param out PrintWriter del cliente.
     * @return true si el login fue exitoso, false en caso contrario.
     */
    private boolean iniciarSesion(BufferedReader in, PrintWriter out) {
    	
        // Validación de sesión previa. No tiene sentido, el jugador antes de iniciar sesion va ser null.
        if (jugador != null && jugador.isSesionIniciada()) {
            out.println("⚠️ Ya tienes sesión iniciada.¿Prefiere registrar? (si/no)");
            
            String respuesta = safeReadLine(in, out);
		    if (respuesta == null) return false; // Cliente cerró conexión

		    if ("si".equalsIgnoreCase(respuesta)) {
		        return registrarSesion(in, out);
		    } else {
		        out.println("❌ No se pudo iniciar sesión tras varios intentos. Se cerrará la conexión.");
		        this.desconectar();
		        return false;
		    }
        }

        out.println("--- INICIANDO SESION ---");
		out.println("Nombre de usuario:");
		out.println("NECESITO RESPUESTA");

		String id = safeReadLine(in, out);
		if (id == null) return false; // Cliente cerró conexión

		// Intentamos iniciar sesión en el servidor
		try {
		    this.jugador = this.rule.inicioSesionDefinitivo(id, cliente);
		} catch (Exception e) {
		    out.println("❌ Error al acceder al servidor de ruleta. Se cerrará la conexión.");
		    this.desconectar();
		    return false;
		}

		// Si no se pudo iniciar sesión
		if (this.jugador == null) {
		    out.println("No ha sido posible iniciar sesión. ¿Prefiere registrar? (si/no)");
		    out.println("NECESITO RESPUESTA");

		    String respuesta = safeReadLine(in, out);
		    if (respuesta == null) return false; // Cliente cerró conexión

		    if ("si".equalsIgnoreCase(respuesta)) {
		        return registrarSesion(in, out);
		    } else {
		        out.println("❌ No se pudo iniciar sesión tras varios intentos. Se cerrará la conexión.");
		        this.desconectar();
		        return false;
		    }
		}

		// Sesión iniciada correctamente
		return true;
    }

    /**
     * Registrar un nuevo jugador en el sistema.
     *
     * PRE:
     *  - in, out != null
     *
     * POST:
     *  - Si el nombre no existe, se crea el jugador con el saldo inicial (5 <= saldo <= 10000).
     *  - Si el nombre ya existe, se pide uno nuevo.
     *  - Si hay error, se desconecta.
     *  - Retorna true si el registro fue exitoso, false en caso contrario.
     *
     * @param in  BufferedReader del cliente.
     * @param out PrintWriter del cliente.
     * @return true si el registro fue exitoso, false en caso contrario.
     */
    private boolean registrarSesion(BufferedReader in, PrintWriter out) {
        out.println("--- REGISTRO ---");

		// 1. Pedir nombre de usuario
		String id = null;
		while (id == null || id.trim().isEmpty()) {
		    out.println("Nombre deseado:");
		    out.println("NECESITO RESPUESTA");

		    id = safeReadLine(in, out);
		    if (id == null) return false; // Cliente cerró conexión

		    id = id.trim();
		    if (id.isEmpty()) {
		        out.println("❌ El nombre no puede estar vacío.");
		        id = null; // forzar repetir
		    }
		}

		// 2. Pedir saldo inicial (mínimo 5€, máximo 10.000€)
		double saldo = 0;
		boolean saldoValido = false;
		while (!saldoValido) {
		    out.println("Saldo inicial:");
		    out.println("NECESITO RESPUESTA");

		    String entrada = safeReadLine(in, out);
		    if (entrada == null) return false; // Cliente cerró conexión

		    try {
		        saldo = Double.parseDouble(entrada);
		        if (saldo < 5) {
		            out.println("❌ El saldo inicial debe ser al menos 5€.");
		        } else if (saldo > 10000) {
		            out.println("❌ El saldo inicial no puede superar los 10.000€.");
		        } else {
		            saldoValido = true;
		        }
		    } catch (NumberFormatException e) {
		        out.println("❌ Introduce un número válido (ej: 100 o 250.5).");
		    }
		}

		// 3. Intentar registrar en el servidor
		try {
		    this.jugador = this.rule.registroSesionDefinitivo(id, saldo, cliente);
		} catch (Exception e) {
		    out.println("❌ Error al registrar en el servidor. Se cerrará la conexión.");
		    this.desconectar();
		    return false;
		}

		// 4. Validar resultado
		if (this.jugador == null) {
		    out.println("❌ El nombre de usuario ya existe. Intenta con otro.");
		    return registrarSesion(in, out); // reintento recursivo
		}

		out.println("✅ Registro completado. Bienvenido " + jugador.getID() + "!");
		return true;
    }

    // --- CREACIÓN DE APUESTAS ---

    /**
     * Crea una apuesta interactuando con el cliente.
     *
     * PRE:
     *  - jugador != null && jugador.isSesionIniciada()
     *  - jugador.getSaldo() >= 5
     *  - in, out != null
     *
     * POST:
     *  - Retorna un objeto Apuesta válido o null si hubo error/desconexión.
     *  - La apuesta tiene:
     *    - Cantidad: 5 <= cantidad <= 10000 y cantidad <= saldo del jugador.
     *    - Tipo: NUMERO, COLOR, PAR_IMPAR o DOCENA.
     *    - Valor: según el tipo seleccionado (ej: "17", "ROJO", "PAR", "2").
     *
     * @param in  BufferedReader del cliente.
     * @param out PrintWriter del cliente.
     * @return Apuesta creada o null si hubo error.
     */
    public Apuesta crearApuesta(BufferedReader in, PrintWriter out) {
        // Validación de sesión
        if (jugador == null || !jugador.isSesionIniciada()) {
            out.println("❌ No tienes sesión iniciada. Se cerrará la conexión.");
            this.desconectar();
            return null;
        }
        
        if (this.jugador.getSaldo() < 5) {
            out.println("❌ No tienes saldo suficiente");
            return null;
        }

        out.println("\n--- NUEVA APUESTA ---");
		out.println("Saldo actual: " + jugador.getSaldo() + "€");

		// 1. PEDIR CANTIDAD (validando saldo y máximo absoluto)
		double cantidad = 0;
		boolean cantidadValida = false;

		while (!cantidadValida) {
		    out.println("¿Cuánto quieres apostar?");
		    out.println("NECESITO RESPUESTA");

		    String entrada = safeReadLine(in, out);
		    if (entrada == null) return null; // Cliente cerró conexión

		    try {
		        cantidad = Double.parseDouble(entrada);

		        if (cantidad < 5) {
		            out.println("❌ La cantidad mínima es 5€.");
		        } else if (cantidad > 10000) {
		            out.println("❌ El máximo permitido por apuesta es 10.000€.");
		        } else if (cantidad > jugador.getSaldo()) {
		            out.println("❌ No tienes suficiente saldo (Tienes: " + jugador.getSaldo() + "€).");
		        } else {
		            cantidadValida = true;
		        }
		    } catch (NumberFormatException e) {
		        out.println("❌ Introduce un número válido (ej: 10.5).");
		    }
		}

		// 2. PEDIR TIPO DE APUESTA
		out.println("¿Qué tipo de apuesta quieres hacer?");
		out.println("1- NUMERO (Pleno)");
		out.println("2- COLOR");
		out.println("3- PAR / IMPAR");
		out.println("4- DOCENA");

		TipoApuesta tipoSeleccionado = null;
		while (tipoSeleccionado == null) {
		    out.println("NECESITO RESPUESTA");
		    String s = safeReadLine(in, out);
		    if (s == null) return null; // Cliente cerró conexión

		    try {
		        int op = Integer.parseInt(s);
		        if (op >= 1 && op <= 4) {
		            tipoSeleccionado = TipoApuesta.values()[op - 1];
		        } else {
		            out.println("❌ Elige entre 1 y 4.");
		        }
		    } catch (NumberFormatException e) {
		        out.println("❌ Introduce un número válido.");
		    }
		}

		// 3. PEDIR VALOR ESPECÍFICO
		String valorApostado = "";
		boolean valorValido = false;

		while (!valorValido) {
		    switch (tipoSeleccionado) {
		        case NUMERO:
		            out.println("Elige número (0-36): ");
		            out.println("NECESITO RESPUESTA");
		            String linea = safeReadLine(in, out);
		            if (linea == null) return null; // Cliente cerró conexión
		            
		            try {
		                int num = Integer.parseInt(linea);
		                if (num >= 0 && num <= 36) {
		                    valorApostado = String.valueOf(num);
		                    valorValido = true;
		                } else {
		                    out.println("❌ Número fuera de rango.");
		                }
		            } catch (NumberFormatException e) {
		                out.println("❌ Error de formato.");
		            }
		            break;

		        case COLOR:
		            out.println("Elige color (ROJO / NEGRO): ");
		            out.println("NECESITO RESPUESTA");
		            String color = safeReadLine(in, out);
		            if (color == null) return null; // Cliente cerró conexión
		            
		            color = color.toUpperCase().trim();
		            if (color.equals("ROJO") || color.equals("NEGRO")) {
		                valorApostado = color;
		                valorValido = true;
		            } else {
		                out.println("❌ Escribe ROJO o NEGRO.");
		            }
		            break;

		        case PAR_IMPAR:
		            out.println("Elige paridad (PAR / IMPAR): ");
		            out.println("NECESITO RESPUESTA");
		            String paridad = safeReadLine(in, out);
		            if (paridad == null) return null; // Cliente cerró conexión
		            
		            paridad = paridad.toUpperCase().trim();
		            if (paridad.equals("PAR") || paridad.equals("IMPAR")) {
		                valorApostado = paridad;
		                valorValido = true;
		            } else {
		                out.println("❌ Escribe PAR o IMPAR.");
		            }
		            break;

		        case DOCENA:
		            out.println("Elige docena (1, 2 o 3): ");
		            out.println("NECESITO RESPUESTA");
		            String docena = safeReadLine(in, out);
		            if (docena == null) return null; // Cliente cerró conexión
		            
		            docena = docena.trim();
		            if (docena.equals("1") || docena.equals("2") || docena.equals("3")) {
		                valorApostado = docena;
		                valorValido = true;
		            } else {
		                out.println("❌ Escribe 1, 2 o 3.");
		            }
		            break;
		    }
		}

		// 4. Construir y devolver la apuesta
		return new Apuesta(jugador, tipoSeleccionado, valorApostado, cantidad);
    }

    // --- MÉTODO AUXILIAR DE LECTURA ---
    /**
     * Lee una línea del cliente con manejo robusto de timeout y desconexión.
     *
     * PRE:
     *  - in != null
     *  - out != null
     *
     * POST:
     *  - Retorna la línea leída (String) si fue exitosa.
     *  - Retorna null si:
     *    - El cliente cerró la conexión (readLine() → null).
     *    - Se agotó el timeout del socket (45s).
     *    - Hubo un error de comunicación (IOException).
     *  - En caso de error, se invoca this.desconectar() automáticamente.
     *
     * MANEJO DE ERRORES:
     *  - SocketTimeoutException: timeout agotado → desconecta y retorna null.
     *  - IOException: error de red → desconecta y retorna null.
     *  - readLine() == null: cliente cerró → desconecta y retorna null.
     *
     * @param in  BufferedReader del cliente.
     * @param out PrintWriter para enviar mensajes de error al cliente.
     * @return Línea leída o null si hubo error/desconexión.
     */
    private String safeReadLine(BufferedReader in, PrintWriter out) {
        try {
            String linea = in.readLine();
            if (linea == null) {
                out.println("❌ Conexión cerrada por el cliente.");
                this.desconectar();
                return null;
            }
            return linea;
        } catch (SocketTimeoutException e) {
            out.println("⏳ Tiempo de espera agotado (45s). Se cerrará la conexión.");
            this.desconectar();
            return null;
        } catch (IOException e) {
            out.println("❌ Error de comunicación. Se cerrará la conexión.");
            this.desconectar();
            return null;
        }
    }

    // --- DESCONEXIÓN ---
    /**
     * Desconecta al jugador de forma limpia.
     *
     * PRE: Ninguna (puede llamarse en cualquier momento).
     *
     * POST:
     *  - Se actualiza el estado del jugador en el servidor (marca sesión como cerrada).
     *  - Se envía mensaje de despedida al cliente (si aún está conectado).
     *  - Se cierra el socket.
     *  - Se limpia la referencia al jugador (jugador = null).
     *
     * CONCURRENCIA:
     *  - Es seguro llamar este método desde múltiples puntos (idempotente).
     */
    public void desconectar() {
        // 1. Actualizar BD / servidor
        try {
            if (jugador != null) {
                this.rule.desconectarJugador(this.jugador);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error al desconectar jugador en servidor: " + e.getMessage());
            // seguimos cerrando socket igualmente
        }

        // 2. Intentar enviar mensaje de despedida
        try {
            if (cliente != null && !cliente.isClosed()) {
                try (PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)) {
                    out.println("MUCHAS GRACIAS POR JUGAR");
                }
                cliente.close(); // cerramos socket explícitamente
            }
        } catch (IOException e) {
            System.out.println("⚠️ El cliente ya estaba desconectado.");
            // no hacemos printStackTrace, solo informativo
        }

        // 3. Actualizar estado del jugador
        if (jugador != null) {
            jugador.setSesionIniciada(false);
            jugador = null;
        }
    }
}