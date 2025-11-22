package ModeloDominio;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class CargarJugadores {
	
	
	public List<Jugador> cargarJugadores(String nameFile) {
		
		
		
		
		try {
			
			 JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
			 Unmarshaller um = context.createUnmarshaller();
			 FileReader file = new FileReader(nameFile);
			 ListaJugadores listJugadores = (ListaJugadores) um.unmarshal(file);
			 
			 
			 			 
			 return listJugadores.getLista();
			 
			 
			 
			 //Si ha surgido algun error devolvemos una lista vaciada.
			 } catch (JAXBException | FileNotFoundException e) { return new ArrayList<>();}
			 
		
		
		
		
		
	}
	
	

}
