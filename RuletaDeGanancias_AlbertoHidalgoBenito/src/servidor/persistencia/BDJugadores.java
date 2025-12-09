package servidor.persistencia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import modeloDominio.Jugador;
import modeloDominio.ListaJugadores;

/**
 * Clase BDJugadores
 * -----------------
 * Se encarga de persistir y recuperar la lista de jugadores desde un fichero XML.
 * Utiliza JAXB para realizar el marshalling (Java → XML) y unmarshalling (XML → Java).
 *
 * PRECONDICIONES:
 *  - El fichero XML debe existir y tener raíz <jugadores> con elementos <jugador>.
 *  - Las clases Jugador y ListaJugadores deben estar anotadas correctamente con JAXB.
 *
 * POSTCONDICIONES:
 *  - Unmarshalling devuelve una lista de jugadores cargada desde el XML.
 *  - Marshalling guarda la lista de jugadores en el fichero XML con formato indentado.
 */
public class BDJugadores {

    /**
     * Carga la lista de jugadores desde un fichero XML.
     *
     * PRECONDICIONES:
     *  - nameFile debe ser la ruta a un fichero XML válido con raíz <jugadores>.
     *
     * POSTCONDICIONES:
     *  - Devuelve una lista de Jugador cargada desde el fichero.
     *  - Si ocurre un error, devuelve una lista vacía.
     */
    public static List<Jugador> UnmarshallingJugadores(File BBDD) {
        try {
            JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
            Unmarshaller um = context.createUnmarshaller();

            // SUPER IMPORTANTE: ignorar validación contra DTD para evitar JAXBException
            um.setSchema(null);

           
            ListaJugadores listJugadores = (ListaJugadores) um.unmarshal(BBDD);
            return listJugadores.getLista();
        } catch (JAXBException e) {
            System.err.println("⚠️ Error unmarshalling jugadores: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda la lista de jugadores en un fichero XML.
     *
     * PRECONDICIONES:
     *  - lj no debe ser null.
     *  - nameFile debe ser una ruta válida donde se pueda escribir.
     *
     * POSTCONDICIONES:
     *  - El fichero contendrá raíz <jugadores> y cada jugador con atributo id y elemento saldo.
     *  - El XML se guarda con formato indentado.
     */
    public static void MarshallingJugadores(List<Jugador> lj, File BBDD) {
        try {
        	
            JAXBContext context = JAXBContext.newInstance(ListaJugadores.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Añadir cabecera DOCTYPE si quieres que aparezca en el XML
            //m.setProperty("com.sun.xml.bind.xmlHeaders","<!DOCTYPE jugadores SYSTEM \"jugadores.dtd\">");

            // Envolver la lista en ListaJugadores
            ListaJugadores wrapper = new ListaJugadores();
            wrapper.setLista(lj);

            m.marshal(wrapper, BBDD);
        } catch (JAXBException e) {
            System.err.println("⚠️ Error marshalling jugadores: " + e.getMessage());
        }
    }
}
