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

import modeloDominio.Jugador;
import modeloDominio.ListaJugadores;

public class BDJugadores {

    public static List<Jugador> UnmarshallingJugadores(String nameFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
            Unmarshaller um = context.createUnmarshaller();
            
            //SUPER IMPORTANTE, sino no hace el Unmarshaller
            um.setSchema(null); // Ignora validación contra DTD
            
            FileReader file = new FileReader(nameFile);
            ListaJugadores listJugadores = (ListaJugadores) um.unmarshal(file);
            return listJugadores.getLista();
        } catch (JAXBException e) {
            System.err.println("⚠️ Error unmarshalling jugadores: " + e.getMessage());
            return new ArrayList<>();
        }catch(FileNotFoundException e) {
        	System.err.println("⚠️ Error unmarshalling jugadoress: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void MarshallingJugadores(List<Jugador> lj, String nameFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Envolver la lista en ListaJugadores
            ListaJugadores wrapper = new ListaJugadores();
            wrapper.setLista(lj);

            m.marshal(wrapper, new File(nameFile));
        } catch (JAXBException e) {
            System.err.println("⚠️ Error marshalling jugadores: " + e.getMessage());
        }
    }
}
