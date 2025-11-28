package principal;

import servidor.red.ServidorRuleta;

public class PrincipalServidor {

	
	public static void main(String [ ] args) {
		
		ServidorRuleta server = new ServidorRuleta();
		server.IniciarServidor(8000,"historial.xml","jugadores.xml");

	}
	
	
}
