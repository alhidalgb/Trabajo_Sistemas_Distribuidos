package logicaRuleta.core;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import modeloDominio.Apuesta;
import modeloDominio.Jugador;

/**
 * Clase AtenderJugador
 * --------------------
 * Hilo dedicado (Worker) que gestiona la comunicación exclusiva con un cliente conectado.
 * Implementa el protocolo de comunicación Servidor <-> Cliente.
 * * Responsabilidades:
 * 1. Gestionar el Handshake inicial (Login/Registro).
 * 2. Escuchar peticiones del cliente (Apuestas, Recargas, Comandos).
 * 3. Enviar respuestas directas sincronizadas con el resto del sistema.
 */
public class AtenderJugador implements Runnable {

    // --- ATRIBUTOS ---
    private final Socket cliente;
    private final ServicioRuleta rule;
    private Jugador jugador;
    private ObjectOutputStream out; 

    // Constantes de estilo (Protocolo visual)
    private static final String AZUL = "\u001B[34m";
    private static final String ROJO = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    // --- CONSTRUCTOR ---
    /**
     * @param cliente Socket de la conexión entrante.
     * @param rule Referencia al servicio central para operaciones de lógica.
     */
    public AtenderJugador(Socket cliente, ServicioRuleta rule) {
        this.cliente = cliente;
        this.rule = rule;
        this.jugador = null;
    }

    // --- CICLO DE VIDA ---
    @Override
    public void run() {
        try (
            // Inicialización de Streams (Output primero para evitar bloqueo de cabeceras de Java)
            ObjectOutputStream outStream = new ObjectOutputStream(this.cliente.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(this.cliente.getInputStream())
        ) {
            this.out = outStream;

            // ---------------------------------------------------------
            // FASE 1: HANDSHAKE Y LOGIN
            // ---------------------------------------------------------
            if (!gestionarLogin(in, outStream)) {
                return; // Fallo en login o desconexión prematura
            }

            // ---------------------------------------------------------
            // FASE 2: SINCRONIZACIÓN DE ESTADO
            // ---------------------------------------------------------
            // Informamos si la mesa está bloqueada para que el cliente configure su barrera local
            outStream.writeBoolean(this.rule.isNoVaMas());
            outStream.flush();
            outStream.reset(); // Limpieza preventiva

            // ---------------------------------------------------------
            // FASE 3: BUCLE DE ESCUCHA (LISTENER)
            // ---------------------------------------------------------
            // Bucle infinito hasta desconexión o interrupción.
            // Solo leemos aquí. Las escrituras se delegan a métodos sincronizados.
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Object mensaje = in.readObject();

                    // Despacho polimórfico de mensajes
                    if (mensaje instanceof Apuesta) {
                        procesarApuesta((Apuesta) mensaje);
                    } 
                    else if (mensaje instanceof Double) {
                        procesarSaldo((Double) mensaje);
                    } 
                    else if (mensaje instanceof String) {
                        if ("SALIR".equalsIgnoreCase((String) mensaje)) {
                            break; // Salida ordenada
                        }
                    }

                } catch (EOFException e) {
                    break; // El cliente cortó la conexión (cierre de ventana)
                }
            }

        } catch (SocketException e) {
            // Desconexión esperada o abrupta
            // System.out.println("Info: Cliente desconectado."); 
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error en comunicación con cliente: " + e.getMessage());
        } finally {
            this.desconectar();
        }
    }

    /**
     * Gestiona la lógica de autenticación inicial.
     * @return true si el jugador se ha autenticado correctamente, false si no.
     */
    private boolean gestionarLogin(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String opcionLogin = (String) in.readObject();

        if ("1".equals(opcionLogin)) {
            // LOGIN
            String id = (String) in.readObject();
            this.jugador = this.rule.inicioSesionDefinitivo(id, out);
        } else {
            // REGISTRO
            Jugador jTemp = (Jugador) in.readObject();
            this.jugador = this.rule.registroSesionDefinitivo(jTemp.getID(), jTemp.getSaldo(), out);
        }

        // Respuesta al cliente
        out.writeObject(this.jugador);
        out.flush();
        out.reset(); // IMPORTANTE: Evitar caching de objetos en el stream

        return this.jugador != null;
    }

    // --- MÉTODOS DE PROCESAMIENTO (THREAD-SAFE) ---

    /**
     * Procesa una solicitud de apuesta.
     * PRE: El jugador debe estar autenticado.
     * POST: Se envía confirmación o rechazo al cliente de forma sincronizada.
     */
    private void procesarApuesta(Apuesta apuesta) throws IOException {
        // Delegación lógica al servicio (Atomicidad garantizada por el servicio)
        boolean aceptada = this.rule.anadirApuesta(this.jugador, apuesta);
        
        // Sincronización OBLIGATORIA para escritura:
        // Evita colisión con hilos de 'MandarPremios' o 'Broadcast'
        synchronized(this.jugador) {
            if (aceptada) {
                // Confirmación visual
                out.writeObject(AZUL + "✅ Apuesta registrada: " + apuesta.getCantidad() + "€ al " + apuesta.getValor() + RESET);
                // Protocolo técnico: Actualizar saldo local del cliente
                out.writeObject("actualizar saldo:" + (-apuesta.getCantidad()));
            } else {
                // Rechazo
                out.writeObject(ROJO + "⛔ Apuesta rechazada (Mesa cerrada o saldo insuficiente)." + RESET);
            }
            out.flush();
            out.reset(); // Liberar memoria de referencias enviadas
        }
    }

    /**
     * Procesa una solicitud de recarga de saldo.
     * PRE: Cantidad > 0.
     * POST: Actualiza el saldo en servidor y notifica al cliente.
     */
    private void procesarSaldo(Double cantidad) throws IOException {
        if (cantidad <= 0) return;

        // Operación thread-safe en el modelo
        this.jugador.sumaRestaSaldo(cantidad);
        
        // Bloqueo para escritura exclusiva
        synchronized(this.jugador) {
            // 1. Comando técnico
            out.writeObject("actualizar saldo:" + cantidad);
            
            // 2. Feedback visual
            out.writeObject(AZUL + "✅ Saldo añadido. Nuevo total: " + this.jugador.getSaldo() + "€" + RESET);
            
            out.flush();
            out.reset();
        }
    }

    /**
     * Cierre ordenado de recursos y desvinculación del servidor.
     */
    public void desconectar() {
        try {
            // 1. Desvincular del juego (Thread-safe en ServicioRuleta)
            if (jugador != null) {
                this.rule.desconectarJugador(this.jugador);
            }

            // 2. Despedida y cierre de Socket
            if (cliente != null && !cliente.isClosed()) {
                if (this.out != null && jugador != null) {
                    try {
                        // Intentamos despedirnos respetando el turno de escritura
                        synchronized(jugador) {
                            this.out.writeObject(AZUL + "MUCHAS GRACIAS POR JUGAR" + RESET);
                            this.out.flush();
                        }
                    } catch (IOException ignored) {
                        // Si el socket ya está roto, ignoramos el error de escritura
                    }
                }
                cliente.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar recursos: " + e.getMessage());
        } finally {
            // 3. Limpieza final de referencias
            if (jugador != null) {
                jugador.setSesionIniciada(false);
                jugador = null;
            }
        }
    }
}