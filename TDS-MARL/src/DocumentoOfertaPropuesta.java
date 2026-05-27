import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.UUID;

public class DocumentoOfertaPropuesta implements Serializable {
	
	private UUID uuid ;
	private String content;

	//datos oferta pala
	private String idPala;	
	private String ubicacionPala;
	private double capacidadPala;
	private long duracionPalada;
	private String destinoMateriales;
	private long horaInicioCargaOfertadaPorPala;

	//datos propuesta camion
	private String idCamion;
	private long horaInicioViajeVacioPropuestoPorCamion;
	private long horaLlegadaApalaPropuestoPorCamion;
	private long horaInicioCargaPropuestoPorCamion;
	private long horaInicioViajeCargadoPropuestoPorCamion;
	private long horaLlegadaAlugarDeDescargaPropuestoPorCamion;
	private long horaFinDeDescargaPropuestoPorCamion;	
	private double cantidadEstimadaDeMaterialATransportar;
	private String motivoRefuse;
	private long tiempoInactivoDesdeActividadAnterior;
	
	
	
	public long getTiempoInactivoDesdeActividadAnterior() {
		return tiempoInactivoDesdeActividadAnterior;
	}

	public void setTiempoInactivoDesdeActividadAnterior(long tiempoInactivoDesdeActividadAnterior) {
		this.tiempoInactivoDesdeActividadAnterior = tiempoInactivoDesdeActividadAnterior;
	}

	public DocumentoOfertaPropuesta(){
		uuid = UUID.randomUUID(); 
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getIdPala() {
		return idPala;
	}

	public void setIdPala(String idPala) {
		this.idPala = idPala;
	}

	public String getUbicacionPala() {
		return ubicacionPala;
	}

	public void setUbicacionPala(String ubicacionPala) {
		this.ubicacionPala = ubicacionPala;
	}

	public double getCapacidadPala() {
		return capacidadPala;
	}

	public void setCapacidadPala(double capacidadPala) {
		this.capacidadPala = capacidadPala;
	}

	public long getDuracionPalada() {
		return duracionPalada;
	}

	public void setDuracionPalada(long duracionPalada) {
		this.duracionPalada = duracionPalada;
	}

	public String getDestinoMateriales() {
		return destinoMateriales;
	}

	public void setDestinoMateriales(String destinoMateriales) {
		this.destinoMateriales = destinoMateriales;
	}

	public long getHoraInicioCargaOfertadaPorPala() {
		return horaInicioCargaOfertadaPorPala;
	}

	public void setHoraInicioCargaOfertadaPorPala(long horaInicioCargaOfertadaPorPala) {
		this.horaInicioCargaOfertadaPorPala = horaInicioCargaOfertadaPorPala;
	}

	public String getIdCamion() {
		return idCamion;
	}

	public void setIdCamion(String idCamion) {
		this.idCamion = idCamion;
	}

	public long getHoraInicioViajeVacioPropuestoPorCamion() {
		return horaInicioViajeVacioPropuestoPorCamion;
	}

	public void setHoraInicioViajeVacioPropuestoPorCamion(long horaInicioViajeVacioPropuestoPorCamion) {
		this.horaInicioViajeVacioPropuestoPorCamion = horaInicioViajeVacioPropuestoPorCamion;
	}

	public long getHoraLlegadaApalaPropuestoPorCamion() {
		return horaLlegadaApalaPropuestoPorCamion;
	}

	public void setHoraLlegadaApalaPropuestoPorCamion(long horaLlegadaApalaPropuestoPorCamion) {
		this.horaLlegadaApalaPropuestoPorCamion = horaLlegadaApalaPropuestoPorCamion;
	}

	public long getHoraInicioCargaPropuestoPorCamion() {
		return horaInicioCargaPropuestoPorCamion;
	}

	public void setHoraInicioCargaPropuestoPorCamion(long horaInicioCargaPropuestoPorCamion) {
		this.horaInicioCargaPropuestoPorCamion = horaInicioCargaPropuestoPorCamion;
	}

	public long getHoraInicioViajeCargadoPropuestoPorCamion() {
		return horaInicioViajeCargadoPropuestoPorCamion;
	}

	public void setHoraInicioViajeCargadoPropuestoPorCamion(long horaInicioViajeCargadoPropuestoPorCamion) {
		this.horaInicioViajeCargadoPropuestoPorCamion = horaInicioViajeCargadoPropuestoPorCamion;
	}

	public long getHoraLlegadaAlugarDeDescargaPropuestoPorCamion() {
		return horaLlegadaAlugarDeDescargaPropuestoPorCamion;
	}

	public void setHoraLlegadaAlugarDeDescargaPropuestoPorCamion(long horaLlegadaAlugarDeDescargaPropuestoPorCamion) {
		this.horaLlegadaAlugarDeDescargaPropuestoPorCamion = horaLlegadaAlugarDeDescargaPropuestoPorCamion;
	}

	public long getHoraFinDeDescargaPropuestoPorCamion() {
		return horaFinDeDescargaPropuestoPorCamion;
	}

	public void setHoraFinDeDescargaPropuestoPorCamion(long horaFinDeDescargaPropuestoPorCamion) {
		this.horaFinDeDescargaPropuestoPorCamion = horaFinDeDescargaPropuestoPorCamion;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		  String newLine = System.getProperty("line.separator");

		  result.append( this.getClass().getName() );
		  result.append( " Object {" );
		  result.append(newLine);

		  //determine fields declared in this class only (no fields of superclass)
		  Field[] fields = this.getClass().getDeclaredFields();

		  //print field names paired with their values
		  for ( Field field : fields  ) {
		    result.append("  ");
		    try {
		      result.append( field.getName() );
		      result.append(": ");
		      //requires access to private field:
		      result.append( field.get(this) );
		    } catch ( IllegalAccessException ex ) {
		      System.out.println(ex);
		    }
		    result.append(newLine);
		  }
		  result.append("}");

		  return result.toString();
	}

	public Double getCantidadEstimadaDeMaterialATransportar() {
		return cantidadEstimadaDeMaterialATransportar;
	}

	public void setCantidadEstimadaDeMaterialATransportar(Double cantidadEstimadaDeMaterialATransportar) {
		this.cantidadEstimadaDeMaterialATransportar = cantidadEstimadaDeMaterialATransportar;
	}

	public String getMotivoRefuse() {
		return motivoRefuse;
	}

	public void setMotivoRefuse(String motivoRefuse) {
		this.motivoRefuse = motivoRefuse;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	
	}