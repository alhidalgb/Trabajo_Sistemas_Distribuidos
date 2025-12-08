package logicaRuleta.core;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import modeloDominio.Apuesta;
import modeloDominio.Jugador;

/**
 * Hilo servidor encargado de la comunicación dedicada con un cliente.
 * Gestiona el ciclo de vida de la conexión: Login -> Juego -> Desconexión.
 */
public class AtenderJugador implements Runnable {

    private final Socket cliente;
    private final ServicioRuleta rule;
    private Jugador jugador;
    private ObjectOutputStream out; // Referencia para desconexión y asignación al jugador

    // Constantes de color para la consola del cliente
    private static final String AZUL = "\u001B[34m";
    private static final String ROJO = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    public AtenderJugador(Socket cliente, ServicioRuleta rule) {
        this.cliente = cliente;
        this.rule = rule;
        this.jugador = null;
    }

    @Override
    public void run() {
        try (
            // Inicializamos los streams (Orden: Output primero para evitar bloqueo de cabeceras)
            ObjectOutputStream outStream = new ObjectOutputStream(this.cliente.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(this.cliente.getInputStream())
        ) {
            this.out = outStream;

            // =================================================================
            // FASE 1: INICIO DE SESIÓN / REGISTRO
            // =================================================================
            
            // Leemos la opción del menú inicial ("1" o "2")
            String opcionLogin = (String) in.readObject();

            if ("1".equals(opcionLogin)) {
                // Opción 1: Iniciar Sesión -> Cliente envía String (ID)
                String id = (String) in.readObject();
                // Pasamos el stream 'out' para que ServicioRuleta lo guarde en el jugador
                this.jugador = this.rule.inicioSesionDefinitivo(id, out);
            } else {
                // Opción 2: Registrarse -> Cliente envía objeto Jugador temporal
                Jugador jTemp = (Jugador) in.readObject();
                // Pasamos el stream 'out'
                this.jugador = this.rule.registroSesionDefinitivo(jTemp.getID(), jTemp.getSaldo(), out);
            }

            // Enviamos el objeto Jugador actualizado (o null si falló)
            out.writeObject(this.jugador);
            out.flush();
            out.reset(); // Evitamos referencias cacheadas

            // Si el login falló, terminamos el hilo (el cliente gestionará el reintento)
            if (this.jugador == null) return;

            // =================================================================
            // FASE CRÍTICA: SINCRONIZACIÓN DE ESTADO INICIAL
            // =================================================================
            // Enviamos el estado de la mesa para que el cliente configure su Latch
            // true = CERRADA (No va más), false = ABIERTA
            out.writeBoolean(this.rule.isNoVaMas());
            out.flush();

            // =================================================================
            // FASE 2: BUCLE PRINCIPAL (POLIMÓRFICO)
            // =================================================================
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Leemos cualquier objeto que llegue por el socket
                    Object mensaje = in.readObject();

                    // CASO A: APUESTA (Juego)
                    if (mensaje instanceof Apuesta) {
                        procesarApuesta((Apuesta) mensaje);
                    } 
                    // CASO B: DOUBLE (Añadir Saldo)
                    else if (mensaje instanceof Double) {
                        procesarSaldo((Double) mensaje);
                    } 
                    // CASO C: STRING (Comandos de control)
                    else if (mensaje instanceof String) {
                        String texto = (String) mensaje;
                        if ("SALIR".equalsIgnoreCase(texto)) {
                            this.desconectar();
                            return; // Salimos del bucle y del run()
                        }
                        // Si llegan otros strings antiguos, los ignoramos.
                    }

                } catch (EOFException e) {
                    // El cliente cerró la conexión abruptamente
                    break;
                }
            }

        } catch (SocketException e) {
            System.out.println("Cliente desconectado (" + (jugador != null ? jugador.getID() : "Anon") + "): " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error comunicación: " + e.getMessage());
        } finally {
            this.desconectar();
        }
    }

    // --- MÉTODOS DE PROCESAMIENTO ---

    private void procesarApuesta(Apuesta apuesta) throws IOException {
        // Delegamos validación lógica (mesa abierta, saldo) al Servicio
        boolean aceptada = this.rule.anadirApuesta(this.jugador, apuesta);
        
        if (aceptada) {
            // Confirmación visual en AZUL
            out.writeObject(AZUL + "✅ Apuesta registrada: " + apuesta.getCantidad() + "€ al " + apuesta.getValor() + RESET);
            // Actualización técnica de saldo para el cliente
            out.writeObject("actualizar saldo:" + (-apuesta.getCantidad()));
        } else {
            // Rechazo en ROJO
            out.writeObject(ROJO + "⛔ Apuesta rechazada (Mesa cerrada o saldo insuficiente)." + RESET);
        }
        out.flush();
    }

    private void procesarSaldo(Double cantidad) throws IOException {
        if (cantidad > 0) {
            // El método sumaRestaSaldo ya es synchronized en tu modelo Jugador
            this.jugador.sumaRestaSaldo(cantidad);
            
            // 1. Mensaje técnico para el Hilo Escucha del cliente (actualiza variable local)
            out.writeObject("actualizar saldo:" + cantidad);
            
            // 2. Mensaje visual para el Usuario
            out.writeObject(AZUL + "✅ Saldo añadido. Nuevo total: " + this.jugador.getSaldo() + "€" + RESET);
            
            out.flush();
        }
    }

    public void desconectar() {
        try {
            // 1. Lógica de negocio: sacar al jugador de las listas del servidor
            if (jugador != null) {
                this.rule.desconectarJugador(this.jugador);
            }

            // 2. Despedida y cierre de red
            if (cliente != null && !cliente.isClosed()) {
                if (this.out != null) {
                    try {
                        // Enviamos despedida en AZUL antes de cortar
                        this.out.writeObject(AZUL + "MUCHAS GRACIAS POR JUGAR" + RESET);
                        this.out.flush();
                    } catch (IOException ignored) {
                        // Si falla al despedirse (socket roto), ignoramos
                    }
                }
                cliente.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar recursos: " + e.getMessage());
        } finally {
            // 3. Limpieza de referencia local
            if (jugador != null) {
                jugador.setSesionIniciada(false);
                jugador = null;
            }
        }
    }
}