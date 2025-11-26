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

/**
 * Clase XMLServidor
 * -----------------
 * Se encarga de persistir en un fichero XML el historial de apuestas de la ruleta.
 *
 * PRECONDICIONES:
 *  - El fichero debe existir o poder crearse en el sistema de archivos.
 *  - Las estructuras de datos (map de jugadores y apuestas) deben estar inicializadas.
 *
 * POSTCONDICIONES:
 *  - El fichero XML contendrá un nodo raíz <historial>.
 *  - Cada llamada a guardarJugadorApuesta añadirá un nuevo bloque <listapuestas> con fecha, ganadora y jugadores.
 *  - El XML se escribirá con formato indentado (pretty print).
 */
public class XMLServidor {

    private final File file;

    /**
     * Constructor de XMLServidor.
     *
     * PRECONDICIONES:
     *  - nameFile no debe ser null ni vacío.
     *
     * POSTCONDICIONES:
     *  - Se crea el fichero si no existe.
     *  - Si está vacío, se inicializa con raíz <historial>.
     */
    public XMLServidor(String nameFile) {
        File fichero = new File(nameFile);

        if (!fichero.exists() || !fichero.isFile()) {
            File carpetaPadre = fichero.getParentFile();
            if (carpetaPadre != null && !carpetaPadre.exists()) {
                carpetaPadre.mkdirs();
            }
            try {
                fichero.createNewFile();
                inicializarDocumento(fichero);
            } catch (IOException e) {
                System.err.println("⚠️ Error creando fichero XML: " + e.getMessage());
            }
        }
        this.file = fichero;
    }

    /**
     * Inicializa el documento XML con raíz <historial>.
     *
     * PRECONDICIONES:
     *  - El fichero debe existir y ser accesible.
     *
     * POSTCONDICIONES:
     *  - El fichero contendrá un documento XML válido con raíz <historial>.
     */
    private void inicializarDocumento(File fichero) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            Element root = doc.createElement("historial");
            doc.appendChild(root);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            t.transform(new DOMSource(doc), new StreamResult(fichero));
        } catch (Exception e) {
            System.err.println("⚠️ Error inicializando documento XML: " + e.getMessage());
        }
    }

    /**
     * Guarda las apuestas de los jugadores en el fichero XML.
     *
     * PRECONDICIONES:
     *  - El map de jugadores y apuestas no debe ser null.
     *  - La casilla ganadora debe estar inicializada.
     *
     * POSTCONDICIONES:
     *  - Se añade un nuevo nodo <listapuestas> al documento XML con:
     *      - Atributo fecha (momento actual).
     *      - Atributo ganadora (número de la casilla).
     *      - Hijos <jugador> con atributo id y sus <apuesta>.
     *  - El fichero se actualiza con formato indentado.
     */
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
            listapuestas.setAttribute("ganadora", (ganador.toString()));

            for (Entry<Jugador, List<Apuesta>> entrada : map.entrySet()) {
                Element jugador = doc.createElement("jugador");
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
            
            //pretty print
            t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
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
