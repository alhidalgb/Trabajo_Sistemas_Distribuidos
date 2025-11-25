package servidor;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import logicaRuleta.ServicioRuletaServidor;
import modeloDominio.Casilla;

public class GiraPelotita implements Runnable {

    private ServicioRuletaServidor rule;
    private ExecutorService pool;
    private XMLServidor xml;
    
    public GiraPelotita(ServicioRuletaServidor rule, ExecutorService pool, XMLServidor xml) {
        this.rule = rule;
        this.pool = pool;
        this.xml = xml;
    }
    
    @Override
    public void run() {
            
        // 1. CERRAR APUESTAS
        this.rule.NoVaMas();
        
        // 2. ESPERAR 2 segundos para que los hilos de apuestas terminen
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        
        // 3. GENERAR NÚMERO GANADOR
        int numeroGanador = new Random().nextInt(37); // 0-36
        Casilla ganadora = new Casilla(numeroGanador);
        
        
        // 4. REPARTIR PREMIOS 
        this.rule.repartirPremio(ganadora);
        
        // 5. GUARDAR EN XML (en paralelo)
        pool.execute(new guardarApuestas(this.rule.getCopiaJugadorApuestas(), ganadora, xml));
                
        // 6. ESPERAR 2 SEGUNDOS ANTES DE ABRIR NUEVA RONDA
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 7. RESETEAR Y ABRIR NUEVA RONDA
        this.rule.resetNoVaMas();

    }
}