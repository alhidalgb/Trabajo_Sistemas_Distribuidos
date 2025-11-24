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
	
	
	
	//Tengo que estar todo el rato cerrandole y abriendolo??
	
	private  File file;
	public XMLServidor(String nameFile) {
		
		
		File fichero = new File(nameFile);

	    if (!fichero.exists() && !fichero.isFile()) {
	        // 1. Aseguramos que el directorio padre exista
	        File carpetaPadre = fichero.getParentFile();
	        if (carpetaPadre != null && !carpetaPadre.exists()) {
	            carpetaPadre.mkdirs(); // Crea todas las carpetas necesarias
	        }

	        // 2. Creamos el fichero vacío
	        try {
				fichero.createNewFile();
			} catch (IOException e) {
				
				fichero = new File(nameFile);
			}
	    }

	    this.file = fichero;}
	
	
	
	public void guardarJugadorApuesta(Map<Jugador,List<Apuesta>> map, Casilla ganador) {
		
		
		try {
			
			Date fechaHoraActual = new Date();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			Element root = doc.getDocumentElement();
			
			Element listapuestas = doc.createElement("listapuestas");
			listapuestas.setAttribute("fecha", fechaHoraActual.toString());
			listapuestas.setAttribute("ganadora", Integer.toString(ganador.getNumero()));
			
			//Todo el map esta dentro de lista apuestas
			for (Entry<Jugador, List<Apuesta>> entrada : map.entrySet()) {
				
				Element jugador = doc.createElement("jugador");
				jugador.setAttribute("id", entrada.getKey().getID());
				
				for(Apuesta ap : entrada.getValue()) {
					
					Element apuesta = doc.createElement("apuesta");
					apuesta.setAttribute("tipo", ap.getTipo().toString());
					apuesta.setAttribute("valor", ap.getValor());					
					apuesta.setAttribute("cantidad", Double.toString(ap.getCantidad()));
					
					jugador.appendChild(apuesta);
					
				}
				
				listapuestas.appendChild(jugador);
				
			}
			
			root.appendChild(listapuestas);
			
			
			//Ya estan todas las apuestas.
			
			
			try {
				
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer t = tf.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(file);
				t.transform(source, result);
				
				
				
			} catch (TransformerException e) {e.printStackTrace();}
			
			
			
		}
		catch(IOException e) {e.printStackTrace();} 
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	
	
	}
}
