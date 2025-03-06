import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase utilitaria para loggear en CSV los mensajes de un sistema multiagente.
 */
public class CsvSchedules {
    // Nombre (o ruta) del archivo CSV donde se guardarán los mensajes.
    private static final String CSV_FILE = ParametrosConfiguracion.prefijoArchivos()+"schedules.csv";
    private static PrintWriter writer;

    // Cabecera para el CSV (opcionales).
    private static final String HEADER = "maquinaPropietariaSchedule, maquina, operacion, horaInicio, "
    		+ "horaFin, ubicacion, cantidadMaterial, "
    		+ "horaInicioHHMMSS, horaFinHHMSS, "
    		+ "duracionActividad, tiempoInactivoDesdeActividadAnterior";

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
    public static synchronized void logSchedule(
    		String maquinaPropietariaSchedule, 
    		String maquina, 
    		String operacion, 
    		String horaInicio, 
    		String horaFin, 
    		String ubicacion, 
    		String cantidadMaterial, 
    		String horaInicioHHMMSS, 
    		String horaFinHHMSS,
    		String tiempoInactivoDesdeActividadAnterior
    ) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        // Reemplazar comas en el contenido para no romper el CSV (puedes usar un parser CSV robusto).
        long duracionActividad = Long.parseLong(horaFin)-Long.parseLong(horaInicio);

        // Construir la línea de CSV
        String line = String.join(",",
        		maquinaPropietariaSchedule, 
        		maquina, 
        		operacion, 
        		horaInicio, 
        		horaFin, 
        		ubicacion, 
        		cantidadMaterial, 
        		horaInicioHHMMSS, 
        		horaFinHHMSS,
        		Long.toString(duracionActividad),
        		tiempoInactivoDesdeActividadAnterior
        );

        writer.println(line);
        writer.flush();
    }
}

