package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import logicaRuleta.*;
import modeloDominio.Jugador;

import java.util.*;

public class ServidorRuleta {

    public void IniciarServidor(int puerto, String historial, String bd) {
        XMLServidor xml = new XMLServidor(historial);

        List<Jugador> jugadoresConSesion = BDJugadores.UnmarshallingJugadores(bd);

        for (Jugador j : jugadoresConSesion) {
            System.out.println("Jugador cargado: " + j);
        }

        ServicioRuletaServidor rule = new ServicioRuletaServidor(jugadoresConSesion);

        ExecutorService pool = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        try (ServerSocket server = new ServerSocket(puerto)) {
            // Crupier automático cada 20 segundos
            scheduler.scheduleWithFixedDelay(new GiraPelotita(rule, pool, xml), 10, 20, TimeUnit.SECONDS);
            scheduler.scheduleAtFixedRate(new , 3, 3, TimeUnit.HOURS);
            
            
            while (!server.isClosed()) {
                try {
                    Socket cliente = server.accept();
                    pool.execute(new AtenderJugador(cliente, rule));
                } catch (IOException e) {
                    System.err.println("⚠️ Error aceptando cliente: " + e.getMessage());
                } catch (RejectedExecutionException e) {
                    System.err.println("⚠️ Pool saturado: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error iniciando servidor: " + e.getMessage());
        } finally {
            // Guardar estado de jugadores al cerrar
            BDJugadores.MarshallingJugadores(jugadoresConSesion, bd);

            scheduler.shutdown();
            pool.shutdown();
            try {
                scheduler.awaitTermination(3, TimeUnit.SECONDS);
                pool.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

