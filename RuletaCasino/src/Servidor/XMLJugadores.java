package Servidor;

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

import ModeloDominio.Apuesta;
import ModeloDominio.Jugador;

public class XMLJugadores {
	
	
	private File file;
	
	public XMLJugadores(String nameFile) {this.file=new File(nameFile);}
	
	
	public void cargarJugadores(String nameFile) {
		
		
		
		
			
		
	}
	
	public void descargarJugadores() {
		
	}
	
	
	
	public void guardarJugadorApuesta(Map<Jugador,List<Apuesta>> map) {
		
		
		try {
			
			Date fechaHoraActual = new Date();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			Element root = doc.getDocumentElement();
			
			Element listapuestas = doc.createElement("listapuestas");
			listapuestas.setAttribute("fecha", fechaHoraActual.toString());
			
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
