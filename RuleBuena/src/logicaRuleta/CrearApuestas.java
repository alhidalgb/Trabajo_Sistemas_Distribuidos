package logicaRuleta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import modeloDominio.Apuesta;
import modeloDominio.Jugador;
import modeloDominio.TipoApuesta;

public class CrearApuestas extends Thread {

	private ServicioRuletaServidor rule;
	private Jugador jugador;

	public CrearApuestas(ServicioRuletaServidor rule, Jugador jug) {
		this.jugador = jug;
		this.rule = rule;
	}

	@Override
	public void run() {
		
		try {
			// 1. INICIALIZAR STREAMS DE TEXTO
			// 'true' en el constructor de PrintWriter activa el autoFlush (envía datos al hacer println)
			PrintWriter out = new PrintWriter(new OutputStreamWriter(this.jugador.getConexion().getOutputStream()), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(this.jugador.getConexion().getInputStream()));

			// 2. LLAMAR A CREAR APUESTA PASANDO LOS WRAPPERS
			// Nota: Es mejor capturar la apuesta y luego añadirla, por si devuelve null o hay error
			
			
			while(true) {
				
				Apuesta apuestaCreada = this.crearApuesta(in, out);
				if (apuestaCreada != null && this.rule.anadirApuesta(jugador,apuestaCreada)) {
					
					out.println("Apuesta añadida con exito");
					
				} else {
					out.println("No se ha podido añadir la apuesta. Lo sentimos.");
				}
				
				
				
				out.println("¿Quieres seguir apostando?");
				out.println("NECESITO RESPUESTA");
				String msg = in.readLine().toLowerCase();
				
				if(!msg.equals("si")) {
					break;
				}
				
				
			}
			
			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método modificado para recibir BufferedReader y PrintWriter
	public Apuesta crearApuesta(BufferedReader in, PrintWriter out) throws IOException {

		out.println("\n--- NUEVA APUESTA ---");
		out.println("Saldo actual: " + jugador.getSaldo() + "€");

		// 1. PEDIR CANTIDAD (Validando saldo)
		double cantidad = 5;
		boolean cantidadValida = false;

		while (!cantidadValida) {
			out.println("¿Cuánto quieres apostar? ");
			
			// Si tu cliente necesita esta señal específica para desbloquear el input, la mantenemos:
			out.println("NECESITO RESPUESTA"); 

			try {
				String entrada = in.readLine();
				if (entrada == null) throw new IOException("Cliente cerró conexión"); // Seguridad básica
				
				cantidad = Double.parseDouble(entrada);

				if (cantidad < 5) {
					out.println("❌ La cantidad debe ser mayor a 5.");
				} else if (cantidad > jugador.getSaldo()) {
					out.println("❌ No tienes suficiente saldo (Tienes: " + jugador.getSaldo() + ")");
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
			try {
				out.println("NECESITO RESPUESTA");
				String s = in.readLine();
				if (s == null) throw new IOException("Cliente cerró conexión");
				
				int op = Integer.parseInt(s);

				if (op >= 1 && op <= 4) {
					tipoSeleccionado = TipoApuesta.values()[op - 1];
				} else {
					out.println("❌ Elige entre 1 y 4.");
				}
			} catch (NumberFormatException e) {
				out.println("❌ Introduce un número.");
			}
		}

		// 3. PEDIR VALOR ESPECÍFICO (Depende del tipo)
		String valorApostado = "";
		boolean valorValido = false;

		while (!valorValido) {
			switch (tipoSeleccionado) {
			case NUMERO:
				out.println("Elige número (0-36): ");
				try {
					out.println("NECESITO RESPUESTA");
					String linea = in.readLine();
					if (linea == null) break; 
					
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
				if (color != null) {
					color = color.toUpperCase().trim();
					if (color.equals("ROJO") || color.equals("NEGRO")) {
						valorApostado = color;
						valorValido = true;
					} else {
						out.println("❌ Escribe ROJO o NEGRO.");
					}
				}
				break;

			case PAR_IMPAR:
				out.println("Elige paridad (PAR / IMPAR): ");
				out.println("NECESITO RESPUESTA");
				
				String paridad = in.readLine();
				if (paridad != null) {
					paridad = paridad.toUpperCase().trim();
					if (paridad.equals("PAR") || paridad.equals("IMPAR")) {
						valorApostado = paridad;
						valorValido = true;
					} else {
						out.println("❌ Escribe PAR o IMPAR.");
					}
				}
				break;

			case DOCENA:
				out.println("Elige docena (1, 2 o 3): ");
				out.println("NECESITO RESPUESTA");
				
				String docena = in.readLine();
				if (docena != null) {
					docena = docena.trim();
					if (docena.equals("1") || docena.equals("2") || docena.equals("3")) {
						valorApostado = docena;
						valorValido = true;
					} else {
						out.println("❌ Escribe 1, 2 o 3.");
					}
				}
				break;
			}
		}

		// 4. CONSTRUIR Y DEVOLVER EL OBJETO
		return new Apuesta(jugador, tipoSeleccionado, valorApostado, cantidad);
	}
}