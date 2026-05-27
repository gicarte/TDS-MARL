import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

/**
 * Ejemplo de ShovelAgent (Manager) que:
 * 1) Tiene una lista de momentos (hora, minuto) en los que estará disponible.
 * 2) Inicia una negociación (CNP) manual por cada momento (una a la vez).
 * 3) Envía CFP a TruckAgents, recibe PROPOSE/REFUSE en un CyclicBehaviour.
 * 4) Usa un WakerBehaviour para controlar un deadline (4000 ms) y,
 *    si expira, decide y cierra la negociación con la info disponible.
 * 5) No usa ContractNetInitiator.
 */
public class ShovelAgent extends Agent {

    private Schedule schedule;
    private boolean isNegotiating = false;
    private boolean negociacionFinalizadaSinGanadores=false;
	private long ultima_HoraInicioCargaOfertadaPorPalaNegociada;
    private Map<String, NegotiationData> negotiations;  // Clave: conversationId -> NegotiationData    
    private String ubicacionInicial;
    private double capacidad;
    private String destinoMateriales;
    private long duracionPalada ;
	private boolean recibiDisconfirm=false;
	private Date deadline;
	private ArrayList<AID> noEnviarCFP;
	protected boolean esperandoConfirmacion=false;
	private boolean recibiRefuseDeUnRequest=false;

    @Override
    protected void setup() {
    	
        schedule = new Schedule();
        negotiations = new HashMap<>(); //aqui se almacena la info de las negociaciones
        Object[] args = getArguments();
        noEnviarCFP = new ArrayList<AID>();
        if (args != null && args.length > 0) {
        	ubicacionInicial=(String) args[0]; 
        	capacidad=(double) args[1]; 
        	destinoMateriales=(String) args[2];
        	duracionPalada=(long) args[3]; 
        } 
        System.out.println(getLocalName() + " listo.");
        addBehaviour(new TickerBehaviour(this, 2000) {         // 3. TickerBehaviour: cada 2s revisa si puede iniciar una nueva negociación
            @Override
            protected void onTick() {
                if (!isNegotiating && HayTareaPorAsignar()) {
                    startNegotiation();
                }
            }
        });
        

        // 4. CyclicBehaviour para procesar mensajes (CFP, PROPOSE, REFUSE, INFORM, etc.).
        //    En este caso, como Shovel es Manager, esperamos PROPOSE/REFUSE de los trucks,
        //    y luego INFORM cuando aceptamos la propuesta.
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myReceive();
                if (msg != null) {
                	if (msg.getReplyByDate()!=null && System.currentTimeMillis() > msg.getReplyByDate().getTime())  { 
                		System.out.println("mensaje llego despues del repplybydate");
                	} else {
                		String convId = msg.getConversationId();
                		NegotiationData data = negotiations.get(convId);
                		if (data == null) {
                			// Mensaje que no pertenece a ninguna negociación actual
	                        return;
	                    }

	                    switch (msg.getPerformative()) {
	                        case ACLMessage.PROPOSE:
	                            handlePropose(msg, data);
	                            break;
	                        case ACLMessage.REFUSE:
	                            handleRefuse(msg, data);
	                            break;
	                        case ACLMessage.CONFIRM:
	                            handleConfirm(msg, data);
	                            break;
	                        case ACLMessage.DISCONFIRM:
	                            handleDisconfirm(msg, data);
	                            break;
	                        default:
	                            System.out.println(msg.toString());
	                            break;
	                    }

	                    // Si ya se recibieron todas las respuestas y no hay decisión tomada, 
	                    // forzamos la decisión (aunque el deadline no haya llegado todavía).
	                    
	                    if (!data.isDecisionMade 
	                        && (data.numResponses == data.numReceivers) && !esperandoConfirmacion) {
	                    	CsvLogger.logMessage(
	                    			getLocalName(),
	                                "llegaron todas las respuestas. numero de trucks "+data.numReceivers+". numero de propuestas "+data.listaPropuestas.size(),
	                                "sin sender", 
	                                "sin receptores",
	                                "sin performative",
	                                "sin conversationID",
	                                "sin contenido"
	                        );
	                    	if (!data.listaPropuestas.isEmpty()) {
	                    		decideAndSendAcceptReject(data);
	                    		
	                    	} else {
	                    		data.isDecisionMade=true;
	                    		isNegotiating=false;
	                    		noEnviarCFP.clear();
	                    		esperandoConfirmacion=false;
	                    		negociacionFinalizadaSinGanadores=true;
	                    		data.finNegociacion=System.currentTimeMillis();
	                    		data.motivoFinNegociacion="No hubieron propuestas";
	                    		CsvLogger.logMessage(
	                            		getLocalName(),
	                                    " - Negociacion " + convId + " finalizada. No hubieron propuestas.",
	                                    "sin sender",
	                                    "sin receptores",
	                                    "sin performative",
	                                    convId,
	                                    "sin contenido"
	                            );
	                    		CsvNegotiationsLogger.logNegotiation(
	                            		getLocalName(), data                                
	                            );
	                    		
	                    	}
	                        
	                    }

	                    // Si la negociación está decidida, revisamos si finalizó
	                    
	                    if (data.isFinished()) {
	                        negotiations.remove(convId);
	                        isNegotiating = false;
	                        esperandoConfirmacion=false;
	                        data.isDecisionMade=true;
	                		//noEnviarCFP.clear();

	                        CsvLogger.logMessage(
	                        		getLocalName(),
	                                " - Negociacion " + convId + " finalizada.",
	                                "sin sender",
	                                "sin receptores",
	                                "sin performative",
	                                convId,
	                                "sin contenido"
	                        );
	                       
	                    }
                	
		            }
                    
                } else {
                    block();
                }
            }

			
        });
    }
    
    /**
     * Método "wrapper" que invoca a super.receive() y, si obtiene un mensaje,
     * lo loggea como "RECEIVE" antes de retornarlo.
     *
     * Úsalo en lugar de 'receive()' en tus comportamientos.
     */
    public ACLMessage myReceive() {
        ACLMessage msg = super.receive();
        if (msg != null) {
            // Extraemos datos
            String sender = (msg.getSender() != null) ? msg.getSender().getLocalName() : "unknown";
            String receivers = getReceiverString(msg);
            String performative = ACLMessage.getPerformative(msg.getPerformative());
            String conversationId = (msg.getConversationId() != null) ? msg.getConversationId() : "";
            Date replyByDate = msg.getReplyByDate();
            Date currentTime = new Date();
            String content="";

          
                 
          
            DocumentoOfertaPropuesta dop=null;;
           
			try {
				dop = (DocumentoOfertaPropuesta) msg.getContentObject();
				if (dop!=null && dop.getContent()!=null) {
	            	content = dop.getContent();	
	            } else {
	            	content = "dop es null o no hay contenido";
	            }
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				 System.out.println("msg received "+sender+" / "+performative);
				e.printStackTrace();
			}
            
            

            switch (msg.getPerformative()) {
            case ACLMessage.PROPOSE:
            	if (currentTime.after(deadline)) {
                    System.out.println(this.getLocalName()+": Mensaje recibido después del límite de tiempo (replyBy: " + deadline + ")");
                }
            	content = "hora de llegada estimada propuesta por camion "
            				+Utilidades.longToHHMMSS(dop.getHoraLlegadaApalaPropuestoPorCamion())
            				+" hora inicio viaje vacio "+Utilidades.longToHHMMSS(dop.getHoraInicioViajeVacioPropuestoPorCamion())
            				+" hora fin descarga "+Utilidades.longToHHMMSS(dop.getHoraFinDeDescargaPropuestoPorCamion())		
            				;
                break;
            case ACLMessage.REFUSE:
                
                break;
            case ACLMessage.DISCONFIRM:
                break;
            default:
                // Podríamos manejar más casos (FAILURE, etc.)
                break;
        }
            
            // Loggear como "RECEIVE"
            CsvLogger.logMessage(
            		this.getLocalName(),
                    "RECEIVE",
                    sender,
                    "sin receivers",
                    performative,
                    conversationId,
                  content
            );
        }
        return msg;
    }
  
    public void mySend(ACLMessage msg) {
        // 1. Identificar datos del mensaje
        String sender = getLocalName();  // el localName de este agente
        String receivers = getReceiverString(msg);
        String performative = ACLMessage.getPerformative(msg.getPerformative());
        String conversationId = (msg.getConversationId() != null) ? msg.getConversationId() : "";
        String content="no se abrio el dop";
        DocumentoOfertaPropuesta dop = negotiations.get(conversationId).getDop();
		content = "hora de inicio ofertada por pala "
				+Utilidades.longToHHMMSS(dop.getHoraInicioCargaOfertadaPorPala()); // "hay que abrir DOP para ver info";//(msg.getContent() != null) ? msg.getContent() : "";
        // 2. Loggear en CSV como "SEND"
        CsvLogger.logMessage(
        		this.getLocalName(),
                "SEND",
                sender,
                receivers,
                performative,
                conversationId,
                content
        );

        // 3. Llamar al método original para enviar de verdad
        super.send(msg);
    }

    

    /**
     * Convierte la lista de receptores de un ACLMessage a un string "AgenteA|AgenteB|..."
     */
    private String getReceiverString(ACLMessage msg) {
        String receiverList="";
        jade.util.leap.Iterator it = msg.getAllReceiver();
        while (it.hasNext()) {
            AID r = (AID) it.next();
            receiverList=receiverList+" "+r.getLocalName();
        }
        return receiverList;
    }


    /**
     * Inicia la negociación enviando un CFP a los Trucks.
     * Crea también un WakerBehaviour para controlar el deadline.
     */
    private void startNegotiation() {
    	deadline = new Date(System.currentTimeMillis() + ParametrosConfiguracion.DEADLINE_MS);
        isNegotiating = true;
        String conversationId = this.getLocalName()+" CNP-" + System.currentTimeMillis();
        NegotiationData data = new NegotiationData(conversationId, ParametrosConfiguracion.NUMERODETRUCKS);  
        negotiations.put(conversationId, data);
        CsvLogger.logMessage(
        		getLocalName(),
                " Iniciando negociacion",
                "sin sender",
                "sin receptores",
                "sin performative",
                conversationId,
                " deadline: " + deadline+" ("+ParametrosConfiguracion.DEADLINE_MS + " ms)"
        );
 
        // Enviamos CFP a los trucks
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.setConversationId(conversationId);
        cfp.setReplyByDate(deadline);

        //cfp.setContent("AvailableTime=" + hour + ":" + String.format("%02d", minute));
        DocumentoOfertaPropuesta dop = generaContenidoCFP();
        System.out.println(getLocalName()+" hora de inicio ofertada por pala "
				+Utilidades.longToHHMMSS(dop.getHoraInicioCargaOfertadaPorPala())); 
        data.setDop(dop);
 		try {
			cfp.setContentObject(dop);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
        for (int i = 0; i < ParametrosConfiguracion.NUMERODETRUCKS; i++) {
        	String truckName = "Truck"+i;
        	
        	cfp.addReceiver(new AID(truckName, AID.ISLOCALNAME));
        }
        data.inicioDeNegociacion = System.currentTimeMillis();
        
        //remuevo a aquellos que disconfirmaron esta negociacion
        for (int i=0;i<noEnviarCFP.size();i++) {
        	cfp.removeReceiver(noEnviarCFP.get(i));
        	CsvLogger.logMessage(this.getLocalName(), "remover agente del CFP", "no sender", "no receiver", "no performative", conversationId, "removi a "+noEnviarCFP.get(i).getLocalName()+" por que antes envio disconfirm");
        }
       
        	
        mySend(cfp);

        // *** WakerBehaviour *** para manejar el deadline de 4000 ms
        addBehaviour(new WakerBehaviour(this, ParametrosConfiguracion.DEADLINE_MS) {
            @Override
            protected void onWake() {
                // Se invoca automáticamente tras 4000 ms
                NegotiationData nd = negotiations.get(conversationId);
                if (nd != null && !nd.isDecisionMade) {
                    // Si la negociación sigue sin decidir, la cerramos con la info disponible
                    /*
                	System.out.println(getLocalName() + " - Deadline alcanzado para "
                                       + conversationId 
                                       + ". Decidiendo con "
                                       + nd.numResponses + " respuestas recibidas.");
                                       */
                    CsvLogger.logMessage(
                    		getLocalName(),
                            "cumplimiento deadline",
                            "sin sender",
                            "sin receptores",
                            "sin performative",
                            conversationId,
                            "sin contenido"
                    );
                	if (!data.listaPropuestas.isEmpty()) {
                		decideAndSendAcceptReject(nd);
                	} else {
                		data.isDecisionMade=true;
                		isNegotiating=false;
                		esperandoConfirmacion=false;
                		noEnviarCFP.clear();
                		negociacionFinalizadaSinGanadores=true;
                		data.finNegociacion=System.currentTimeMillis();
                		data.motivoFinNegociacion="deadline cumplido. No hubieron propuestas";
                		CsvLogger.logMessage(
                        		getLocalName(),
                                " - Negociacion " + data.conversationId + " finalizada. deadline cumplido No hubieron propuestas.",
                                "sin sender",
                                "sin receptores",
                                "sin performative",
                                data.conversationId,
                                "sin contenido"
                        );
                		CsvNegotiationsLogger.logNegotiation(
                        		getLocalName(), data                                
                        );
                		
                	}
                    // Revisamos si eso la deja finalizada
                    if (nd.isFinished()) {
                        negotiations.remove(conversationId);
                        isNegotiating = false;   
                        esperandoConfirmacion=false;
                		noEnviarCFP.clear();
                        System.out.println(getLocalName()
                            + " - Negociación " + conversationId + " finalizada (deadline).");
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------------------------
    // Métodos de manejo de mensajes
    // ------------------------------------------------------------------------------------
    private void handlePropose(ACLMessage propose, NegotiationData data) {
    	if (data.conversationId==propose.getConversationId()) {
    		data.numResponses++;
            data.numProposes++;
           	
    	}
        // int arrival = parseArrivalTime(propose.getContent());
       // data.proposals.put(propose.getSender(), arrival);
        DocumentoOfertaPropuesta dop;
		try {
			dop = (DocumentoOfertaPropuesta) propose.getContentObject();
			if (dop.getHoraInicioCargaPropuestoPorCamion()>=dop.getHoraInicioCargaOfertadaPorPala()){			
		        data.listaPropuestas.add(propose);
		        //no agrego propuestas antes de la hora ofertada por pala. Quizas sirvan pero de
				}
		} catch (UnreadableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        /*
        System.out.println(getLocalName() + " - Recibido PROPOSE de "
                           + propose.getSender().getLocalName()
                           + ": " + propose.getContent() 
                           + ", convID=" + propose.getConversationId());
         */
    }

    private void handleRefuse(ACLMessage refuse, NegotiationData data) {
    	if (data.conversationId==refuse.getConversationId()) {
    		data.numResponses++;
            data.numRefuses++;
    	}
    	if (esperandoConfirmacion) {
    		this.isNegotiating=false;
    		esperandoConfirmacion=false;
    		//negociacionFinalizadaSinGanadores=true; //esto no esta bien. 
    		recibiRefuseDeUnRequest=true;
    		noEnviarCFP.clear();
    		data.isDecisionMade = true;
    		data.finNegociacion=System.currentTimeMillis();
    		data.motivoFinNegociacion="recibi refuse despues de request";
    		CsvNegotiationsLogger.logNegotiation(
            		getLocalName(), data                                
            );
    	}
    	
    
    }

    private void handleConfirm(ACLMessage confirm, NegotiationData data) {
        data.numInforms++;
        DocumentoOfertaPropuesta dop;
		try {
			dop = (DocumentoOfertaPropuesta) confirm.getContentObject();
	        AgregaGanador(dop);
			this.isNegotiating=false;
    		negociacionFinalizadaSinGanadores=false;
    		noEnviarCFP.clear();
    		data.isDecisionMade = true;
    		data.finNegociacion=System.currentTimeMillis();
    		data.motivoFinNegociacion="recibi inform. ganador acepto";
    		CsvNegotiationsLogger.logNegotiation(
            		getLocalName(), data                                
            );
		} catch (UnreadableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
   }
    
    private void handleDisconfirm(ACLMessage msg, ShovelAgent.NegotiationData data) {
		this.isNegotiating=false;
		esperandoConfirmacion=false;
		data.isDecisionMade=true;
		data.motivoFinNegociacion="recibi Disconfirm";
		noEnviarCFP.add(msg.getSender());
		data.finNegociacion=System.currentTimeMillis();
		data.numCancels++;
		recibiDisconfirm=true;
		CsvNegotiationsLogger.logNegotiation(
        		getLocalName(), data                                
        );
		CsvLogger.logMessage(
        		getLocalName(),
                " - Negociacion " + "convId" + " finalizada. Recibi Disconfirm. Debo reiniciar negociacion",
                "sin sender",
                "sin receptores",
                "sin performative",
                "convId",
                "sin contenido"
        );
		
	}

    /**
     * Decide y envía ACCEPT al Truck con llegada más temprana,
     * REJECT a los demás.
     */
    private void decideAndSendAcceptReject(NegotiationData data) {
       // data.isDecisionMade = true;

       // AID bestTruck = null;
		double menorValor = Double.POSITIVE_INFINITY;
		ACLMessage propuestaGanadora = data.listaPropuestas.get(0);

        for (ACLMessage proposal : data.listaPropuestas) {
			try {
				DocumentoOfertaPropuesta dop = (DocumentoOfertaPropuesta) proposal.getContentObject();
				double valorPropuesta = dop.getHoraLlegadaApalaPropuestoPorCamion();
				if (dop.getHoraInicioCargaPropuestoPorCamion()>=dop.getHoraInicioCargaOfertadaPorPala()){
					if (valorPropuesta < menorValor) {
						menorValor = valorPropuesta;
						propuestaGanadora = proposal;
					}
				} //no considera propuestas con horas inicio de carga antes de lo ofertado.             

			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
        
     // Mandamos ACCEPT a bestTruck,
		data.listaPropuestas.remove(propuestaGanadora);
		data.ganador=propuestaGanadora.getSender().getLocalName();
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.setConversationId(data.conversationId);
        request.addReceiver(propuestaGanadora.getSender());
        try {
        	request.setContentObject(propuestaGanadora.getContentObject());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnreadableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        mySend(request);
        esperandoConfirmacion=true;


       //  REJECT a los demás //porque si utilizo el reply anterior, le enviaba mensaje al best proposal
        ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        reject.setConversationId(data.conversationId);
        reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
        for (ACLMessage proposal : data.listaPropuestas) {
        	reject.addReceiver(proposal.getSender());
        }
        mySend(reject);
        
    }

    

    @Override
    protected void takeDown() {
        System.out.println(getLocalName() + " finalizando.");
    }
    
    public DocumentoOfertaPropuesta generaContenidoCFP() {
		DocumentoOfertaPropuesta dop = new DocumentoOfertaPropuesta();		
		dop.setIdPala(this.getLocalName());
		dop.setUbicacionPala(this.ubicacionInicial);
		dop.setDestinoMateriales(destinoMateriales);
		dop.setCapacidadPala(capacidad);
		dop.setDuracionPalada(duracionPalada);
		
		if (recibiDisconfirm || recibiRefuseDeUnRequest) {
			dop.setHoraInicioCargaOfertadaPorPala(ultima_HoraInicioCargaOfertadaPorPalaNegociada);//
			recibiDisconfirm=false;
			recibiRefuseDeUnRequest=false;
		} else {
			if (negociacionFinalizadaSinGanadores) {
				dop.setHoraInicioCargaOfertadaPorPala(ultima_HoraInicioCargaOfertadaPorPalaNegociada+60000);//60 segundos mas  mas
				negociacionFinalizadaSinGanadores=false;
			} else {
				dop.setHoraInicioCargaOfertadaPorPala(schedule.getHoraFinUltimoSlot(this.getLocalName()));

			}
		}
		
        Slot previo = this.schedule.buscarSlotAnterior(dop.getHoraInicioCargaOfertadaPorPala());
        if (previo==null) {
			dop.setTiempoInactivoDesdeActividadAnterior(0);
		} else {
			dop.setTiempoInactivoDesdeActividadAnterior(dop.getHoraInicioCargaOfertadaPorPala()-previo.getHoraFin());
		}
		
		
		
		
		
		
		ultima_HoraInicioCargaOfertadaPorPalaNegociada=dop.getHoraInicioCargaOfertadaPorPala();
		//System.out.println(this.getLocalName()+" ofertando "+dop.getHoraInicioCargaOfertadaPorPala()+" - "+Utilidades.longToHHMMSS(dop.getHoraInicioCargaOfertadaPorPala()));
		
		return dop;
		
		
	}
    
    public boolean HayTareaPorAsignar() {
		// TODO Auto-generated method stub
    	if (this.ultima_HoraInicioCargaOfertadaPorPalaNegociada<=ParametrosConfiguracion.SEGUNDO_FINAL_HORARIO) {
    		return true;
    	} else {
    		System.out.println(this.getLocalName()+" mi horario supera el turno");
			this.doSuspend();
    		return false;
    	}
    		
    	
    	/* esta es la version anterior
		if (!schedule.horarioSuperaTurno()) {
			return true;
		} else {
			System.out.println(this.getLocalName()+" mi horario supera el turno");
			this.doSuspend();
			return false;
		}
		*/
		
	}
    
    public void AgregaGanador(DocumentoOfertaPropuesta dop) {
		// TODO Auto-generated method stub
		schedule.agregarSlot(this.getLocalName(), dop.getIdCamion(), dop.getHoraInicioCargaPropuestoPorCamion(), dop.getHoraInicioViajeCargadoPropuestoPorCamion(), ubicacionInicial, Slot.enum_operacion.CARGA, dop.getCantidadEstimadaDeMaterialATransportar());
		//schedule.imprimirSchedule(this.getLocalName());
		//CsvSchedules.logSchedule(destinoMateriales, destinoMateriales, destinoMateriales, destinoMateriales, destinoMateriales, ubicacionInicial, destinoMateriales, ubicacionInicial, destinoMateriales);
	}

    // ------------------------------------------------------------------------------------
    // Clase interna para info de la negociación en curso
    // ------------------------------------------------------------------------------------
    public static class NegotiationData {
		protected String motivoFinNegociacion;
		protected long finNegociacion;
		public long inicioDeNegociacion;
		String conversationId;
        int numReceivers;      // cuántos TRUCKS invitamos
        int numResponses = 0;  // cuántos PROPOSE/REFUSE recibimos
        boolean isDecisionMade = false;

        int numInforms = 0; // cuántos INFORM tras un ACCEPT
      //  int hour, minute;
        private DocumentoOfertaPropuesta dop;

       // Map<AID, Integer> proposals = new HashMap<>();
        private ArrayList<ACLMessage> listaPropuestas;
		public String ganador;
		public int numProposes;
		public int numRefuses;
		public int numCancels;
        NegotiationData(String convId, int numReceivers) {
            this.conversationId = convId;
            this.numReceivers = numReceivers;
            listaPropuestas = new ArrayList<ACLMessage>();
        }

        boolean isFinished() {
            // Concluimos la negociación si se tomó la decisión y ya recibimos al menos
            // 1 INFORM (si hubo un ACCEPT). Si nadie propuso, se finaliza al decidir.
            if (!isDecisionMade) {
                return false;
            }
            if (listaPropuestas.isEmpty()) {
                // Nadie mandó propose
                return true;
            } else {
                // Esperamos 1 INFORM de la parte aceptada
                return numInforms >= 1;
            }
        }

		public DocumentoOfertaPropuesta getDop() {
			return dop;
		}

		public void setDop(DocumentoOfertaPropuesta dop) {
			this.dop = dop;
		}
    }
}
