package principal;

import cliente.ClienteRuleta;

public class PrincipalCliente {

	public static void main(String[] args) {
		
        new ClienteRuleta("LOCALHOST",8000).IniciarCliente();
	
	}

}
