import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Schedule {
	
	private ArrayList<Slot> slots;
	
	public Schedule(){
		slots = new ArrayList<Slot>();
	}

	public ArrayList<Slot> getSlots() {
		return slots;
	}

	public void setSlots(ArrayList<Slot> slots) {
		this.slots = slots;
	}

	public boolean horarioSuperaTurno() {
		if (slots.isEmpty()) {
			return false;
		} else {
			if (slots.get(slots.size()-1).getHoraFin()>=ParametrosConfiguracion.SEGUNDO_FINAL_HORARIO) {				
				return true;
			} else {
				return false;
			}
		}
		
		
	}

	public long getHoraFinUltimoSlot(String pala) {
		if (slots.isEmpty()) {
			//System.out.println("no hay slots. retorno 0");
			return 0;
		} else {
			//System.out.println(pala +": hora inicio carga ultimo slot"+slots.get(slots.size()-1).getHoraFin());
			return slots.get(slots.size()-1).getHoraFin();
		}
	}

	public void agregarSlot(String maquinaPropietariaSchedule, String maquina, long horaInicio, long horaFin, String ubicacion,  Slot.enum_operacion operacion , double cantidadMaterial) {
		Slot previo = this.buscarSlotAnterior(horaInicio);
		
		
		Slot s = new Slot();
		s.setMaquinaPropietariaSchedule(maquinaPropietariaSchedule);
		s.setMaquina(maquina);
		s.setHoraInicio(horaInicio);
		s.setHoraFin(horaFin);
		s.setOperacion(operacion);
		s.setUbicacion(ubicacion);
		s.setCantidadMaterial(cantidadMaterial);
		if (previo==null) {
			s.setTiempoInactivoDesdeActividadAnterior(0);
		} else {
			s.setTiempoInactivoDesdeActividadAnterior(s.getHoraInicio()-previo.getHoraFin());
		}
		slots.add(s);
		//ordeno el schedule segun la hora de inicio
		Collections.sort(slots, Comparator.comparingLong(p -> p.getHoraInicio()));
		CsvSchedules.logSchedule(maquinaPropietariaSchedule, 
				s.getMaquina(), 
				s.getOperacion().toString(), 
				Long.toString(s.getHoraInicio() ), 
				Long.toString(s.getHoraFin()), 
				s.getUbicacion(), 
				Double.toString(s.getCantidadMaterial()),
				Utilidades.longToHHMMSS(horaInicio),
				Utilidades.longToHHMMSS(horaFin),
				Long.toString(s.getTiempoInactivoDesdeActividadAnterior())
				);
		comprobarSolapamiento(s);
	}

	private void comprobarSolapamiento(Slot s) {
		// TODO Auto-generated method stub
		for (int i=0;i<slots.size();i++) {
			Slot actual = slots.get(i);
			if ((actual.getHoraInicio()<s.getHoraInicio()) && (actual.getHoraFin()>s.getHoraInicio()) ) {
				System.out.println(s.getMaquinaPropietariaSchedule()+ ": ERROR de solapamiento hora Inicio");
				
			}
			if ((actual.getHoraInicio()<s.getHoraFin()) && (actual.getHoraFin()>s.getHoraFin()) ) {
				System.out.println(s.getMaquinaPropietariaSchedule()+ "ERROR de solapamiento hora fin");
				
			}
		}
		
	}
	
	

	public boolean estaCamionOcupado(DocumentoOfertaPropuesta dop) {
		/*
		 * si el camion esta desocupado retorna true, de lo contrario retorna false
		 * 
		 */
		Slot actual;

			
		//System.out.println(dop.toString());
		long horaDeCargaOfertadaPorPala = dop.getHoraInicioCargaOfertadaPorPala();
		long horafinDescarga = dop.getHoraFinDeDescargaPropuestoPorCamion();
		long horaInicioCarga = dop.getHoraInicioCargaPropuestoPorCamion();
		long horaInicioViajeCargado = dop.getHoraInicioViajeCargadoPropuestoPorCamion();
		long horaInicioViajeVacio = dop.getHoraInicioViajeVacioPropuestoPorCamion();
		long horaLlegadaAlugarDeDescarga = dop.getHoraLlegadaAlugarDeDescargaPropuestoPorCamion();
		long horaLlegadaApala = dop.getHoraLlegadaApalaPropuestoPorCamion();




		if (slots.isEmpty()) {
			return false;
		} else {
			for (int i=0;i<slots.size()-1;i++) {
				actual=slots.get(i);
				
				/*
				
				if (horafinDescarga==0 && horaInicioCarga==0 && horaInicioViajeCargado==0 && horaInicioViajeVacio==0 && horaLlegadaAlugarDeDescarga==0 && horaLlegadaApala==0) { 	
					if (horaDeCargaOfertadaPorPala>=actual.getHoraInicio() &&horaDeCargaOfertadaPorPala<=actual.getHoraFin()) {
						return true; //esto es en caso que no ha se enviando propuesta aun 
					} 

				} else { //estas condiciones son para evaluar si la propuesta aceptada por la pala
							//puede ser incorporada al schedule
*/

					if (horafinDescarga>=actual.getHoraInicio()
							&& horafinDescarga<=actual.getHoraFin()) {
						return true;
					}
					if (horaInicioCarga>=actual.getHoraInicio()
							&& horaInicioCarga<=actual.getHoraFin()) {
						return true;
					} if (horaInicioViajeCargado>=actual.getHoraInicio()
							&& horaInicioViajeCargado<=actual.getHoraFin()) {
						return true;
					} if (horaInicioViajeVacio>=actual.getHoraInicio()
							&& horaInicioViajeVacio<=actual.getHoraFin()) {
						return true;
					} if (horaLlegadaAlugarDeDescarga>=actual.getHoraInicio()
							&& horaLlegadaAlugarDeDescarga<=actual.getHoraFin()) {
						return true;
					} if (horaLlegadaApala>=actual.getHoraInicio()
							&& horaLlegadaApala<=actual.getHoraFin()) {
						return true;
					} 

			//	}

			}

		}

		return false;

		/*
		long horaEstimadaInicioCargaOfertadaPorPala = dop.getHoraInicioCargaOfertadaPorPala();
		Slot sAnterior = buscarSlotAnterior(horaEstimadaInicioCargaOfertadaPorPala);
		Slot sSiguiente = buscarSlotSiguiente(horaEstimadaInicioCargaOfertadaPorPala);



		if (sAnterior!=null && 
				sAnterior.getHoraInicio()>horaEstimadaInicioCargaOfertadaPorPala && 
				horaEstimadaInicioCargaOfertadaPorPala<sAnterior.getHoraFin()){
			return true;
		}

		if (sSiguiente!=null && 
				sSiguiente.getHoraInicio()>horaEstimadaInicioCargaOfertadaPorPala && 
				horaEstimadaInicioCargaOfertadaPorPala<sSiguiente.getHoraFin()){
			return true;
		}

		if (sAnterior==null && sSiguiente==null) { 
			return false;
		}

		if (sAnterior==null && sSiguiente!=null && horaEstimadaInicioCargaOfertadaPorPala<sSiguiente.getHoraInicio()){
			return false;
		}

		if (sAnterior!=null && sSiguiente==null && horaEstimadaInicioCargaOfertadaPorPala>sAnterior.getHoraFin()){
			return false;
		}

		return false;
		 */

	}

	public Slot buscarSlotSiguiente(long hora) {
		//se asume lista ordenada. hora esta fuera de los slots.
		Slot sig=null;
		Slot actual=null;
		if (slots.isEmpty()) {
			return null;
		} else {
			for (int i=0;i<slots.size()-1;i++) {
				actual=slots.get(i);
				if (hora<=actual.getHoraFin()) {
					sig=actual;
				} else {
					return sig;
				}
			}

		}
		/*
		for (int i=0;i<slots.size();i++) {
			Slot s = slots.get(i);			
			System.out.println(i+" "
			+s.getMaquinaPropietariaSchedule()+" "
			+s.getMaquina()+" "
			+Utilidades.longToHHMMSS(s.getHoraInicio())+" "
			+Utilidades.longToHHMMSS(s.getHoraFin())+" "
			+s.getOperacion()+" "
			+s.getUbicacion());
		}
		System.out.println("hora buscada es:"+Utilidades.longToHHMMSS(hora)
		+ " el siguiente es "+Utilidades.longToHHMMSS(sig.getHoraInicio()));
		*/
		return sig;

	}

	public Slot buscarSlotAnterior(long hora) {
		//se asume lista ordenada. 
		Slot previo=null;
		Slot actual=null;
		if (slots.isEmpty()) {
			return null;
		} else {
			for (int i=0;i<slots.size();i++) {
				actual=slots.get(i);
				if (actual.getHoraInicio()>=hora) {
					return previo;
				} else {
					previo=actual;
				}
			}
			
		}
		/*
		for (int i=0;i<slots.size();i++) {
			Slot s = slots.get(i);			
			System.out.println(i+" "
			+s.getMaquinaPropietariaSchedule()+" "
			+s.getMaquina()+" "
			+Utilidades.longToHHMMSS(s.getHoraInicio())+" "
			+Utilidades.longToHHMMSS(s.getHoraFin())+" "
			+s.getOperacion()+" "
			+s.getUbicacion());
		}
		System.out.println("hora buscada es:"+Utilidades.longToHHMMSS(hora)
			+" el previo es "+Utilidades.longToHHMMSS(previo.getHoraFin()));
			*/
		return previo;
		
		
	}
	
	

	public void imprimirSchedule(String nombreAgente) {
		// TODO Auto-generated method stub
		for (int i=0;i<slots.size();i++) {
			Slot s = slots.get(i);			
			System.out.println(i+" "
			+nombreAgente+" "
			+s.getMaquina()+" "
			+Utilidades.longToHHMMSS(s.getHoraInicio())+" "
			+Utilidades.longToHHMMSS(s.getHoraFin())+" "
			+s.getOperacion()+" "
			+s.getUbicacion());
		}
	}

	/*
	public boolean slotOcupado(DocumentoOfertaPropuesta dop) {
		if (slots.isEmpty()) {
			return false;
		} else {

			for (int i=0;i<slots.size();i++) {
				Slot s = slots.get(i);
				if (s.getOperacion().equals("carga")) {
					return true;
				}
			}

		}

		return false;
	}
	*/
	
	/*
	private void guardaSchedule(String maquinaPropietariaSchedule, String maquina, enum_operacion operacion, long horaInicio, long horaFin, String ubicacion, double cantidadMaterial) {
	    String csvFile = "schedule.csv";
	    boolean isNewFile = !Files.exists(Paths.get(csvFile));

	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, !isNewFile))) {
	        // Write headers if it's a new file
	        if (isNewFile) {
	            writer.write("maquinaPropietariaSchedule, maquina, operacion, horaInicio, horaFin, ubicacion, cantidadMaterial, horaInicioHHMMSS, horaFinHHMSS");
	            writer.newLine();
	        }

	        // Convert the timestamp to a formatted string
	        //Instant instant = Instant.ofEpochMilli(timestamp);
	        //String formattedTimestamp = DateTimeFormatter.ISO_INSTANT.format(instant);

	        // Append the message flow to the CSV file
           
	        writer.write(maquinaPropietariaSchedule + "," + maquina + ","  + operacion + "," + horaInicio + "," + horaFin + "," + ubicacion + "," + cantidadMaterial+","+Utilidades.longToHHMMSS(horaInicio)+","+Utilidades.longToHHMMSS(horaFin));
	        writer.newLine();
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	*/

	public boolean camionPuedeRealizarCicloPropuesto(DocumentoOfertaPropuesta dop) {
		// TODO Auto-generated method stub

		Slot sAnterior = this.buscarSlotAnterior(dop.getHoraInicioViajeVacioPropuestoPorCamion());
		Slot sSiguiente = this.buscarSlotSiguiente(dop.getHoraFinDeDescargaPropuestoPorCamion());
		
		if (sAnterior==null && sSiguiente==null) {
			return true;
		}
		
		if (sAnterior!=null && sSiguiente==null && 
				sAnterior.getOperacion().equals(Slot.enum_operacion.DESCARGA) && sAnterior.getHoraFin()<dop.getHoraInicioViajeVacioPropuestoPorCamion()) {
			return true;
		}
		
		if (sAnterior==null && sSiguiente!=null && 
				sSiguiente.getOperacion().equals(Slot.enum_operacion.VIAJE_VACIO) && sSiguiente.getHoraInicio()>dop.getHoraFinDeDescargaPropuestoPorCamion()) {
			return true;
		}
		
		if (sAnterior!=null && sSiguiente!=null && 
				sSiguiente.getOperacion().equals(Slot.enum_operacion.VIAJE_VACIO) && 
				sSiguiente.getHoraInicio()>dop.getHoraFinDeDescargaPropuestoPorCamion() &&
				sAnterior.getOperacion().equals(Slot.enum_operacion.DESCARGA) && 
				sAnterior.getHoraFin()<dop.getHoraInicioViajeVacioPropuestoPorCamion()) {

			return true;
		}

		return false;
	}

	public boolean existeSolapamiento(DocumentoOfertaPropuesta dop) {
		// TODO Auto-generated method stub
		for (int i=0;i<slots.size();i++) {
			Slot actual = slots.get(i);
			if (actual.getHoraInicio()<=dop.getHoraInicioViajeVacioPropuestoPorCamion() &&  
					actual.getHoraFin()>=dop.getHoraInicioViajeVacioPropuestoPorCamion()) {
				return true;
			}
			if (actual.getHoraInicio()<=dop.getHoraLlegadaApalaPropuestoPorCamion() &&  
					actual.getHoraFin()>=dop.getHoraLlegadaApalaPropuestoPorCamion()) {
				return true;
			}
			if (actual.getHoraInicio()<=dop.getHoraInicioCargaPropuestoPorCamion() &&  
					actual.getHoraFin()>=dop.getHoraInicioCargaPropuestoPorCamion()) {
				return true;
			}
			if (actual.getHoraInicio()<=dop.getHoraInicioViajeCargadoPropuestoPorCamion() &&  
					actual.getHoraFin()>=dop.getHoraInicioViajeCargadoPropuestoPorCamion()) {
				return true;
			}
			if (actual.getHoraInicio()<=dop.getHoraLlegadaAlugarDeDescargaPropuestoPorCamion() &&  
					actual.getHoraFin()>=dop.getHoraLlegadaAlugarDeDescargaPropuestoPorCamion()) {
				return true;
			}
			if (actual.getHoraInicio()<=dop.getHoraFinDeDescargaPropuestoPorCamion() &&  
					actual.getHoraFin()>=dop.getHoraFinDeDescargaPropuestoPorCamion()) {
				return true;
			}
			
			
		}
		return false;
	}
	
	

}