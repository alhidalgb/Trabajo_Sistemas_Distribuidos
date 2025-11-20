package Principal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CrearApuestas implements Runnable {

	
	private ServicioRuleta rule;
	private Jugador jugador;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		
		try(BufferedReader bff = new BufferedReader(new InputStreamReader(System.in))){
			
		
			while(true) {
				
				
				
				this.rule.anadirApuesta(this.crearApuesta(jugador, bff));
				
			}
			
			
			
			
			
		}catch(IOException e) {e.printStackTrace();
		
		}
		
		
		
		
	}
	
	
	// Método estático para crear la apuesta
    public  Apuesta crearApuesta(Jugador jugador, BufferedReader bff) {
        
        System.out.println("\n--- NUEVA APUESTA ---");
        System.out.println("Saldo actual: " + jugador.getSaldo() + "€");

        try {
            // 1. PEDIR CANTIDAD (Validando saldo)
            double cantidad = 5;
            boolean cantidadValida = false;
            
            while (!cantidadValida) {
                System.out.print("¿Cuánto quieres apostar? ");
                try {
                    String entrada = bff.readLine();
                    cantidad = Double.parseDouble(entrada);
                    
                    if (cantidad < 5) {
                        System.out.println("❌ La cantidad debe ser mayor a 5.");
                    } else if (cantidad > jugador.getSaldo()) {
                        System.out.println("❌ No tienes suficiente saldo (Tienes: " + jugador.getSaldo() + ")");
                    } else {
                        cantidadValida = true;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Introduce un número válido (ej: 10.5).");
                }
            }

            // 2. PEDIR TIPO DE APUESTA
            System.out.println("¿Qué tipo de apuesta quieres hacer?");
            System.out.println("1- NUMERO (Pleno)");
            System.out.println("2- COLOR");
            System.out.println("3- PAR / IMPAR");
            System.out.println("4- DOCENA");
            
            TipoApuesta tipoSeleccionado = null;
            while (tipoSeleccionado == null) {
                try {
                    String s = bff.readLine();
                    int op = Integer.parseInt(s);
                    // Asumiendo que tu Enum tiene este orden
                    if (op >= 1 && op <= 4) {
                        tipoSeleccionado = TipoApuesta.values()[op - 1];
                    } else {
                        System.out.println("❌ Elige entre 1 y 4.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Introduce un número.");
                }
            }

            // 3. PEDIR VALOR ESPECÍFICO (Depende del tipo)
            String valorApostado = "";
            boolean valorValido = false;

            while (!valorValido) {
                switch (tipoSeleccionado) {
                    case NUMERO:
                        System.out.print("Elige número (0-36): ");
                        try {
                            int num = Integer.parseInt(bff.readLine());
                            if (num >= 0 && num <= 36) {
                                valorApostado = String.valueOf(num);
                                valorValido = true;
                            } else System.out.println("❌ Número fuera de rango.");
                        } catch (NumberFormatException e) { System.out.println("❌ Error de formato."); }
                        break;

                    case COLOR:
                        System.out.print("Elige color (ROJO / NEGRO): ");
                        String color = bff.readLine().toUpperCase().trim();
                        if (color.equals("ROJO") || color.equals("NEGRO")) {
                            valorApostado = color;
                            valorValido = true;
                        } else System.out.println("❌ Escribe ROJO o NEGRO.");
                        break;

                    case PAR_IMPAR:
                        System.out.print("Elige paridad (PAR / IMPAR): ");
                        String paridad = bff.readLine().toUpperCase().trim();
                        if (paridad.equals("PAR") || paridad.equals("IMPAR")) {
                            valorApostado = paridad;
                            valorValido = true;
                        } else System.out.println("❌ Escribe PAR o IMPAR.");
                        break;

                    case DOCENA:
                        System.out.print("Elige docena (1, 2 o 3): ");
                        String docena = bff.readLine().trim();
                        if (docena.equals("1") || docena.equals("2") || docena.equals("3")) {
                            valorApostado = docena;
                            valorValido = true;
                        } else System.out.println("❌ Escribe 1, 2 o 3.");
                        break;
                }
            }

            // 4. CONSTRUIR Y DEVOLVER EL OBJETO
            // Usamos el ID del jugador que pasamos por parámetro
            return new Apuesta(jugador, tipoSeleccionado, valorApostado, cantidad);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
	
	

}
