
public class ParametrosConfiguracion {

	public static int NUMERODESHOVELS;//en main container se inicializa.
	public static int NUMERODETRUCKS; //en main container se inicializa.
	public static long DEADLINE_MS=1000L;
	public static final long SEGUNDO_FINAL_HORARIO = 6*60*60*1000; // turno de 6 horas en milisegundos;
	public static final String LEARNING_ENABLED = "false";
	public static final String NOMBRE_ARCHIVO_FLOTA = "datosShovelTrucksPequeno.owl";
	
	public static final String prefijoArchivos() {
		String prefijo;
		int index = NOMBRE_ARCHIVO_FLOTA.indexOf('.');
        
		if (LEARNING_ENABLED.equals("false")) {
			prefijo = NOMBRE_ARCHIVO_FLOTA.substring(0, index)+"SinRL";
		} else {
			prefijo = NOMBRE_ARCHIVO_FLOTA.substring(0, index)+"ConRL";

		}
		 
		return prefijo;
		
	}
}
