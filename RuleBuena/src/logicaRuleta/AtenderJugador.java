package logicaRuleta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter; // Usamos esto para facilitar los envíos
import java.net.Socket;
import java.time.LocalDate;

import modeloDominio.Jugador;

public class AtenderJugador implements Runnable {

    private Socket cliente;
    private ServicioRuletaServidor rule; // Tu lógica de negocio compartida
    
    // El jugador de este hilo.
    private Jugador jugador; 

    public AtenderJugador(Socket cliente, ServicioRuletaServidor rule) {
        this.cliente = cliente;
        this.rule = rule;
        this.jugador = null;
    }

    @Override
    public void run() {
        // Usamos try-with-resources para asegurar que el socket se cierra al final
        try (
            // Wrappers para facilitar lectura/escritura
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true) // true = AutoFlush
        ) {
            
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
            	
                //System.out.println("Jugador " + jugador.getID() + " conectado.");
                boolean salir = false;

                while (!salir) {
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

        } catch (IOException e) {
        	
        	//Este mensaje es para el servidor.
            System.out.println("Error de conexión con cliente: " + e.getMessage());
            
        } finally {
        	
        	
        	this.desconectar();
        		
         
 
        }
    }

    // --- MÉTODOS DEL MENÚ ---

    private void opcionAnadirSaldo(BufferedReader in, PrintWriter out) throws IOException {
    	
        out.println("¿Cuánto dinero quieres ingresar?");
        
        while(true) {
        	
            
            try {    	    	
                out.println("NECESITO RESPUESTA");

                String cantStr = in.readLine();
                double cantidad = Double.parseDouble(cantStr);
                
                if (cantidad > 0) {
                    // Actualizamos en la BD/XML a través del servicio      	
                	jugador.setSaldo(jugador.getSaldo()+cantidad); // Actualizamos la referencia local
                    //Por el paso por referencia de Java, se actualiza solo.
                	
                    out.println("✅ Saldo añadido correctamente.");
                    break;
                } else {
                    out.println("⚠️ La cantidad debe ser positiva.");
                }
            } catch (NumberFormatException e) {
                out.println("⚠️ Error: Introduce un número válido.");
            }
        }
        	
        }
 

    
    private void opcionJugar(BufferedReader in, PrintWriter out) {
    	
        try {
            out.println("⏳ Esperando a que se abra la mesa...");
            
            // 1. Sincronización: Esperamos a que empiece la ronda de apuestas
            
            this.rule.VaMasAwait(); 
            
            out.println("--- ¡HAGAN JUEGO! (Mesa Abierta) ---");
            
            // 2. Ciclo de apuestas
            // Mientras la mesa esté abierta, permitimos apostar
            boolean seguirApostando = true;
            
            CrearApuestas apuesta=null;
            while (seguirApostando) { 
                out.println("1. Apostar");
                out.println("2. Terminar apuestas (Esperar resultado)");
                out.println("NECESITO RESPUESTA");
                
                String op = in.readLine();
                if (op == null) return;

                if (op.equals("1")) {
                	
                
                	this.rule.getPool().execute(new CrearApuestas(this.rule,this.jugador));
                	break;
        			
                   
                } else {
                    out.println("Apuestas finalizadas por el jugador.");
                    seguirApostando = false;
                }
            }

            
            
            // 3. Esperamos al resultado (Sincronización)
            out.println("⏳ Esperando a que gire la bola... ");
            this.rule.noVaMasAwait();
            
            
            
        
            // NOTA: El resultado se envía por BROADCAST desde la clase RondaDeJuego/Servidor,
            // así que aquí simplemente volvemos al menú principal.
            out.println("--- FIN DE LA RONDA ---");

        } catch (InterruptedException | IOException e) {
            out.println("Error durante el juego.");
        }
    }
    
    // Método auxiliar simple para gestionar una apuesta (Metodo inutil)
    private void procesarNuevaApuesta(BufferedReader in, PrintWriter out) throws IOException {
    	
        
        out.println("Introduce Tipo (NUMERO, COLOR...):");
        out.println("NECESITO RESPUESTA");
        String tipo = in.readLine();
        
        out.println("Introduce Valor (7, ROJO...):");
        out.println("NECESITO RESPUESTA");
        String valor = in.readLine();
        
        out.println("Introduce Cantidad:");
        out.println("NECESITO RESPUESTA");
        try {
            double cant = Double.parseDouble(in.readLine());
            // Validar saldo y registrar
            if (jugador.getSaldo() >= cant) {
                // rule.registrarApuesta(new Apuesta(...));
                out.println("✅ Apuesta aceptada.");
            } else {
                out.println("❌ Saldo insuficiente.");
            }
        } catch (Exception e) {
            out.println("❌ Error en la apuesta.");
        }
    }

    // --- TUS MÉTODOS DE LOGIN (Adaptados a PrintWriter) ---

    private boolean iniciarSesion(BufferedReader in, PrintWriter out) throws IOException {
        out.println("--- INICIANDO SESION ---");
        out.println("Nombre de usuario:");
        out.println("NECESITO RESPUESTA");

        String id = in.readLine();
        // Lógica de buscar jugador...
        this.jugador = this.rule.inicioSesionDefinitivo(id, cliente); // Asumo que esto devuelve Jugador o null

        if (this.jugador == null) {
            out.println("No ha sido posible iniciar sesion. ¿Prefiere registrar? (si/no)");
            out.println("NECESITO RESPUESTA");
            if ("si".equalsIgnoreCase(in.readLine())) {
                return registrarSesion(in, out);
            } else {
                return iniciarSesion(in, out); // Recursivo (cuidado con stackoverflow si abusa)
            }
        }
        return true;
    }

    private boolean registrarSesion(BufferedReader in, PrintWriter out) throws IOException {
        out.println("--- REGISTRO ---");
        out.println("Nombre deseado:");
        out.println("NECESITO RESPUESTA");
        String id = in.readLine();
        
        out.println("Saldo inicial:");
        out.println("NECESITO RESPUESTA");
        try {
            double saldo = Double.parseDouble(in.readLine());
            // Lógica de registro...
            this.jugador = this.rule.registroSesionDefinitivo(id, saldo, cliente);
            return this.jugador != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    public void desconectar() {
    	
    	//Actualizar BD, cerrar Socket sin tumbar el servidor, y actulizar la lista.
    	
    	//La lista se va actualizando sola por el paso por referencia. La BD de datos la guarda el servidor.
    	
    	
    	try (PrintWriter out = new PrintWriter(cliente.getOutputStream(), true);){
    		
    		
    		out.println("MUCHAS GRACIAS POR JUGAR");
    		
    		
    	}catch(IOException e){e.printStackTrace();}
    	finally {
    		
    		this.jugador.setSesionIniciada(false);
    		//Marcamos la referencia de este jugador a null;
        	this.jugador=null;
        	
    	}
    	
    }
    
    
    
    
    
    
    
    
    
    
    
}