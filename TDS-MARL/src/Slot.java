public class Slot {
	
	public static enum enum_operacion {VIAJE_VACIO, CARGA, VIAJE_CARGADO, DESCARGA }; 
	private String maquinaPropietariaSchedule;
	private long horaInicio;
	private long horaFin;
	private String maquina;
	private enum_operacion operacion;
	private String ubicacion;
	private double cantidadMaterial;
	private long tiempoInactivoDesdeActividadAnterior;
	
	public long getTiempoInactivoDesdeActividadAnterior() {
		return tiempoInactivoDesdeActividadAnterior;
	}
	public void setTiempoInactivoDesdeActividadAnterior(long tiempoInactivoDesdeActividadAnterior) {
		this.tiempoInactivoDesdeActividadAnterior = tiempoInactivoDesdeActividadAnterior;
	}
	public long getHoraInicio() {
		return horaInicio;
	}
	public void setHoraInicio(long horaInicio) {
		this.horaInicio = horaInicio;
	}
	public long getHoraFin() {
		return horaFin;
	}
	public void setHoraFin(long horaFin) {
		this.horaFin = horaFin;
	}
	public String getMaquina() {
		return maquina;
	}
	public void setMaquina(String maquina) {
		this.maquina = maquina;
	}
	public enum_operacion getOperacion() {
		return operacion;
	}
	public void setOperacion(enum_operacion operacion) {
		this.operacion = operacion;
	}
	public String getUbicacion() {
		return ubicacion;
	}
	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}
	public double getCantidadMaterial() {
		return cantidadMaterial;
	}
	public void setCantidadMaterial(double cantidadMaterial) {
		this.cantidadMaterial = cantidadMaterial;
	}
	public String getMaquinaPropietariaSchedule() {
		return maquinaPropietariaSchedule;
	}
	public void setMaquinaPropietariaSchedule(String maquinaPropietariaSchedule) {
		this.maquinaPropietariaSchedule = maquinaPropietariaSchedule;
	}
	
}