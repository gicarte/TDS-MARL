

import java.util.Iterator;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;

public class Utilidades {

	public static String longToHHMMSS(long duration) {		

		int seconds = (int) (duration / 1000) % 60 ;
		int minutes = (int) ((duration / (1000*60)) % 60);
		int hours   = (int) ((duration / (1000*60*60)) % 24);
		return hours+":"+minutes+":"+seconds;
	}

	public static Vertice retornaVerticeDesdeGrafoMina(Graph<Vertice, DefaultWeightedEdge> grafo,  String id) {
		// TODO Auto-generated method stub

		Set<Vertice> vertices = grafo.vertexSet();
		Iterator<Vertice> it = vertices.iterator();
		while(it.hasNext()){
			Vertice v = it.next();
			if (v.getId().equals(id)) {
				return v;
			}

		}
		System.out.println("vertice "+id+" no encontrado en el grafo");
		return null;
	}
	
	public static GraphPath<Vertice, DefaultWeightedEdge> calcularRuta(Graph<Vertice, DefaultWeightedEdge> grafo, Vertice inicio, Vertice destino) {
		// TODO Auto-generated method stub
		
		return DijkstraShortestPath.findPathBetween(grafo, inicio, destino);
		
		
		
	}
	
	
}
