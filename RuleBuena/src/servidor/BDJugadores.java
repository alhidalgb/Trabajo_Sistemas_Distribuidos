package servidor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import modeloDominio.*;


public class BDJugadores {
	
	
	public static List<Jugador> UnmarshallingJugadores(String nameFile) {
		
		
		
		try {
			
			 JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
			 Unmarshaller um = context.createUnmarshaller();
			 FileReader file = new FileReader(nameFile);
			 ListaJugadores listJugadores = (ListaJugadores) um.unmarshal(file);
			 
			 
			 			 
			 return listJugadores.getLista();
			 
			 
			 
			 //Si ha surgido algun error devolvemos una lista vaciada.
			 } catch (JAXBException | FileNotFoundException e) { return new ArrayList<>();}
			 
		
		
		
		
		
	}
	
	
	public static void MarshallingJugadores(List<Jugador> lj,String nameFile) {
		
		
		try {
			
			 JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
			 Marshaller m = context.createMarshaller();
			 m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			 m.marshal(lj, new File(nameFile));
			 
			 } catch (JAXBException e) {e.printStackTrace();}
		
		
	}
	
	

}
