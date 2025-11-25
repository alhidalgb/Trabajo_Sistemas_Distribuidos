package servidor;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class XMLServidor {

    private final File file;

    public XMLServidor(String nameFile) {
        File fichero = new File(nameFile);

        if (!fichero.exists() || !fichero.isFile()) {
            File carpetaPadre = fichero.getParentFile();
            if (carpetaPadre != null && !carpetaPadre.exists()) {
                carpetaPadre.mkdirs();
            }
            try {
                fichero.createNewFile();
                // Inicializar con raíz si está vacío
                inicializarDocumento(fichero);
            } catch (IOException e) {
                System.err.println("⚠️ Error creando fichero XML: " + e.getMessage());
            }
        }
        this.file = fichero;
    }

    
    //SI EL FICHERO ESTA VACIO A QUE NOS UNIMOS!!!!!!!!
    
    private void inicializarDocumento(File fichero) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            Element root = doc.createElement("historial");
            doc.appendChild(root);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.transform(new DOMSource(doc), new StreamResult(fichero));
        } catch (Exception e) {
            System.err.println("⚠️ Error inicializando documento XML: " + e.getMessage());
        }
    }
    
    //¿Es necesario synchronized?
    public synchronized void guardarJugadorApuesta(Map<Jugador, List<Apuesta>> map, Casilla ganador) {
        try {
            Date fechaHoraActual = new Date();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);

            Element root = doc.getDocumentElement();
            if (root == null) {
                root = doc.createElement("historial");
                doc.appendChild(root);
            }

            Element listapuestas = doc.createElement("listapuestas");
            listapuestas.setAttribute("fecha", fechaHoraActual.toString());
            listapuestas.setAttribute("ganadora", Integer.toString(ganador.getNumero()));

            for (Entry<Jugador, List<Apuesta>> entrada : map.entrySet()) {
                Element jugador = doc.createElement("jugador");
                jugador.setIdAttribute("id", true);
                jugador.setAttribute("id", entrada.getKey().getID());

                for (Apuesta ap : entrada.getValue()) {
                    Element apuesta = doc.createElement("apuesta");
                    apuesta.setAttribute("tipo", ap.getTipo().toString());
                    apuesta.setAttribute("valor", ap.getValor());
                    apuesta.setAttribute("cantidad", Double.toString(ap.getCantidad()));
                    jugador.appendChild(apuesta);
                }
                listapuestas.appendChild(jugador);
            }

            root.appendChild(listapuestas);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.transform(new DOMSource(doc), new StreamResult(file));

        } catch (IOException e) {
            System.err.println("⚠️ Error de E/S guardando apuestas: " + e.getMessage());
        } catch (ParserConfigurationException | SAXException e) {
            System.err.println("⚠️ Error de parser XML: " + e.getMessage());
        } catch (TransformerException e) {
            System.err.println("⚠️ Error transformando XML: " + e.getMessage());
        }
    }
}

