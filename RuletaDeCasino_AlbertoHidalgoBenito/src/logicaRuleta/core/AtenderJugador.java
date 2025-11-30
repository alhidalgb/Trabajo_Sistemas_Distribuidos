package logicaRuleta.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter; // Usamos esto para facilitar los envíos
import java.net.Socket;
import java.net.SocketException;

import modeloDominio.Apuesta;
import modeloDominio.Jugador;
import modeloDominio.TipoApuesta;

public class AtenderJugador implements Runnable {

    private Socket cliente;
    private ServicioRuletaServidor rule; // Tu lógica de negocio compartida
    
    // El jugador de este hilo.
    private Jugador jugador; 

    public AtenderJugador(Socket cliente, ServicioRuletaServidor rule) {
        if (cliente == null || cliente.isClosed()||rule == null) {
            throw new IllegalArgumentException("Socket inválido: nulo o cerrado.");
        }
        
        this.cliente = cliente;
        this.rule = rule;
        this.jugador = null; // Se asignará en login/registro
    }


    @Override
    public void run() {
        // Usamos try-with-resources para asegurar que el socket se cierra al final
        try (
            // Wrappers para facilitar lectura/escritura
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true) // true = AutoFlush
        ) {
            
        	//El servidor se puede quedar atascado leyendo al cliente, lo que provocara un cliente fantasma. El cliente solo tiene 30 segundos para mandar un mensaje sino se cerrera la conexion. Entonces
        	// el servidor solo tiene 45 segundos para intentar leer al cliente (tiene 15  segundos de gracia) sino cerra conexion.
        	this.cliente.setSoTimeout(45000);
        	
            // --- FASE 1: LOGIN / REGISTRO ---
            boolean logueado = false;
            
            // Bucle hasta que se loguee o se desconecte
            while (!logueado) {
                out.println("=== BIENVENIDO AL CASINO ===");
                out.println("1. Iniciar Sesion");
                out.println("2. Registrarse");
                out.println("NECESITO RESPUESTA"); //Protocolo para obtener respuesta

                String opcion = in.readLine();
                if (opcion == null) return; // Cliente cerró conexión

                if (opcion.equals("1")) {
                    logueado = iniciarSesion(in, out);
                } else{
                	
                	//Aqui no dejamos escapar a nadie, si no inicias sesion te registras.
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

                    String seleccion = in.readLine();
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
            
            
        } catch (SocketException e) {System.out.println("El servidor estuvo mucho tiempo atascado intentando leer al cliente: " + e.getMessage());
        } catch (IOException e) {
        	
        	//Este mensaje es para el servidor.
            System.out.println("Error de conexión con cliente: " + e.getMessage());
            
        } finally {
        	
        	
        	this.desconectar();
        		
         
 
        }
    }

    // --- MÉTODOS DEL MENÚ ---

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
                String cantStr = in.readLine();

                if (cantStr == null) {
                    // Cliente cerró conexión
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return;
                }

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
            } catch (IOException e) {
                out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
                this.desconectar();
                return;
            }
        }
    }

 

    
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
            	
            	if(this.jugador.getSaldo()<5) {out.println("❌ No tienes saldo suficiente"); break;}
            	
                out.println("1. Apostar");
                out.println("2. Terminar apuestas (Esperar resultado)");
                out.println("NECESITO RESPUESTA");
                
                String op = in.readLine();
                if (op == null) {
                    // Cliente cerró conexión
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return;
                }

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
        } catch (IOException e) {
            out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
            this.desconectar();
        } catch (NullPointerException e) {
        	out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
            this.desconectar();
        }
        
    }

    

    // --- TUS MÉTODOS DE LOGIN (Adaptados a PrintWriter) ---

    private boolean iniciarSesion(BufferedReader in, PrintWriter out) {
        // Validación de sesión previa
        if (jugador != null && jugador.isSesionIniciada()) {
            out.println("⚠️ Ya tienes sesión iniciada.");
            return true;
        }

        try {
            out.println("--- INICIANDO SESION ---");
            out.println("Nombre de usuario:");
            out.println("NECESITO RESPUESTA");

            String id = in.readLine();
            if (id == null) {
                out.println("❌ Conexión cerrada por el cliente.");
                this.desconectar();
                return false;
            }

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

                String respuesta = in.readLine();
                if (respuesta == null) {
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return false;
                }

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

        } catch (IOException e) {
            out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
            this.desconectar();
            return false;
        }
    }


    private boolean registrarSesion(BufferedReader in, PrintWriter out) {
        try {
            out.println("--- REGISTRO ---");

            // 1. Pedir nombre de usuario
            String id = null;
            while (id == null || id.trim().isEmpty()) {
                out.println("Nombre deseado:");
                out.println("NECESITO RESPUESTA");

                id = in.readLine();
                if (id == null) {
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return false;
                }

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

                String entrada = in.readLine();
                if (entrada == null) {
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return false;
                }

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

        } catch (IOException e) {
            out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
            this.desconectar();
            return false;
        }
    }

    
    
    
    public Apuesta crearApuesta(BufferedReader in, PrintWriter out) {
        // Validación de sesión
        if (jugador == null || !jugador.isSesionIniciada()) {
            out.println("❌ No tienes sesión iniciada. Se cerrará la conexión.");
            this.desconectar();
            return null;
        }
        
        if(this.jugador.getSaldo()<5) {out.println("❌ No tienes saldo suficiente");return null; }

        try {
            out.println("\n--- NUEVA APUESTA ---");
            out.println("Saldo actual: " + jugador.getSaldo() + "€");

            // 1. PEDIR CANTIDAD (validando saldo y máximo absoluto)
            double cantidad = 0;
            boolean cantidadValida = false;

            while (!cantidadValida) {
                out.println("¿Cuánto quieres apostar?");
                out.println("NECESITO RESPUESTA");

                String entrada = in.readLine();
                if (entrada == null) {
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return null;
                }

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
                String s = in.readLine();
                if (s == null) {
                    out.println("❌ Conexión cerrada por el cliente.");
                    this.desconectar();
                    return null;
                }

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
                        String linea = in.readLine();
                        if (linea == null) {
                            out.println("❌ Conexión cerrada por el cliente.");
                            this.desconectar();
                            return null;
                        }
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
                        String color = in.readLine();
                        if (color == null) {
                            out.println("❌ Conexión cerrada por el cliente.");
                            this.desconectar();
                            return null;
                        }
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
                        String paridad = in.readLine();
                        if (paridad == null) {
                            out.println("❌ Conexión cerrada por el cliente.");
                            this.desconectar();
                            return null;
                        }
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
                        String docena = in.readLine();
                        if (docena == null) {
                            out.println("❌ Conexión cerrada por el cliente.");
                            this.desconectar();
                            return null;
                        }
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

        } catch (IOException e) {
            out.println("❌ Error de comunicación con el cliente. Se cerrará la conexión.");
            this.desconectar();
            return null;
        }
    }

    
    
    
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