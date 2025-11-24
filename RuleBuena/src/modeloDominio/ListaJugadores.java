package modeloDominio;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;
import modeloDominio.Jugador;

@XmlRootElement(name = "jugadores") // La raíz del XML
public class ListaJugadores {

   
    private List<Jugador> lista = new ArrayList<>();

    @XmlElement(name = "jugador") // Nombre de cada item en la lista
    public List<Jugador> getLista() { return lista; }
    public void setLista(List<Jugador> lista) { this.lista = lista; }
    
}