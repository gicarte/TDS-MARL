import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Clase utilitaria para loggear en CSV los mensajes de un sistema multiagente.
 */
public class CsvNegotiationsLogger {
    // Nombre (o ruta) del archivo CSV donde se guardarán los mensajes.
    private static final String CSV_FILE = ParametrosConfiguracion.prefijoArchivos()+"negotiations.csv";
    private static PrintWriter writer;

    // Cabecera para el CSV (opcionales).
    private static final String HEADER = "timestamp,agent,inicioNegociacion,finNegociacion,conversationId,content, duracion, ganador, refusesRecibidas, proposesRecibidas, informRecibidos, cancelRecibidos";

    // Se ejecuta una vez al cargar la clase
    static {
        try {
            boolean fileExists = new java.io.File(CSV_FILE).exists();
            writer = new PrintWriter(new FileWriter(CSV_FILE, true));
            // Si el archivo estaba vacío o no existía, escribimos la cabecera
            if (!fileExists) {
                writer.println(HEADER);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registra un mensaje en el archivo CSV (forma sincronizada para evitar conflictos).
     * 
     * @param agentAction   "SEND" o "RECEIVE"
     * @param sender        nombre local del agente emisor
     * @param receivers     cadena con la lista de receptores (separados por '|')
     * @param performative  ejemplo: "CFP", "PROPOSE", "ACCEPT_PROPOSAL", etc.
     * @param conversationId ID de la conversación
     * @param content       contenido del mensaje
     */
    public static synchronized void logNegotiation(String agent, 
    		ShovelAgent.NegotiationData data) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        // Reemplazar comas en el contenido para no romper el CSV (puedes usar un parser CSV robusto).

        // Construir la línea de CSV
        String line = String.join(",",
        		timestamp,
        		agent,
        		Long.toString(data.inicioDeNegociacion),
        		Long.toString(data.finNegociacion), 
        		data.conversationId,
        		data.motivoFinNegociacion, 
        		Long.toString(data.finNegociacion-data.inicioDeNegociacion),
        		data.ganador,
        		Integer.toString(data.numProposes) ,
        		Integer.toString(data.numRefuses),
        		Integer.toString(data.numInforms), 
        		Integer.toString(data.numCancels)
        		);

        writer.println(line);
        writer.flush();
    }
}
