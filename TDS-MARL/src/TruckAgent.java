import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.FileWriter;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Ejemplo de Agente Truck que actúa como 'Contractor' en un Contract Net Protocol
 * sin usar ContractNetResponder de JADE.
 * En su lugar, implementa un comportamiento propio que procesa:
 *  - CFP (Call for Proposal)
 *  - ACCEPT_PROPOSAL
 *  - REJECT_PROPOSAL
 */
public class TruckAgent extends Agent {
	//horario
    private Schedule schedule;

    private Random random;

    // ------------------------------------------------------------------
    // (Opcional) Bandera para habilitar o deshabilitar aprendizaje
    // ------------------------------------------------------------------
    private boolean learningEnabled = false;

    // ------------------------------------------------------------------
    // Q-LEARNING (opcional): Para cada Shovel (estado), Q-values para 2 acciones:
    // action=0 -> REFUSE, action=1 -> PROPOSE
    // Usamos un Map: shovelName -> double[2]
    // ------------------------------------------------------------------
    private Map<String, double[]> qTable;  

    // Parámetros de Q-learning
    private double alpha = 0.1;      // Tasa de aprendizaje
    private double gamma = 0.9;      // Factor de descuento
    private double epsilon = 0.1;    // Probabilidad de exploración

    // Guardamos la última acción elegida por conversación ID
    private Map<String, Integer> lastActionChosen;

	private double capacidadCamion;

	private String ubicacionInicial;

	private double velocidadVacio;

	private long duracionDescarga;

	private double velocidadCargado; 
	
	private Graph<Vertice, DefaultWeightedEdge> grafoMina;
	
	private List<Double> episodeRewards; // Lista para almacenar la recompensa acumulada por episodio
    private int episodeCount = 0; // Contador de episodios
    private static final String LOG_FILE = ParametrosConfiguracion.prefijoArchivos()+"rewards_log.csv"; // Archivo para guardar los datos


    @Override
    protected void setup() {
    	schedule = new Schedule();
        random = new Random();
        episodeRewards = new ArrayList<>();


        // Lectura de argumentos (opcional) para habilitar/deshabilitar aprendizaje
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            try {
                learningEnabled = Boolean.parseBoolean(ParametrosConfiguracion.LEARNING_ENABLED);
                ubicacionInicial = args[0].toString();
                velocidadVacio = Double.parseDouble(args[1].toString());
                capacidadCamion = Double.parseDouble(args[2].toString());
                velocidadCargado = Double.parseDouble(args[3].toString());
                duracionDescarga = Long.parseLong(args[4].toString());
                grafoMina = (Graph<Vertice, DefaultWeightedEdge>) args[5]; 
            } catch (Exception e) {
                // Mantener valor por defecto
            }
        }

        System.out.println(getLocalName() + " listo. Aprendizaje habilitado = " + learningEnabled);

        // Inicializamos estructuras para RL
        qTable = new HashMap<>();
        lastActionChosen = new HashMap<>();

        // Añadimos un comportamiento cíclico que atenderá TODOS los mensajes
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myReceive();
                if (msg != null) {
                    // Procesar el mensaje
                    switch (msg.getPerformative()) {
                        case ACLMessage.CFP:
                            handleCfp(msg);
                            break;
                        case ACLMessage.ACCEPT_PROPOSAL:
                            handleAcceptProposal(msg);
                            break;
                        case ACLMessage.REJECT_PROPOSAL:
                            handleRejectProposal(msg);
                            break;
                        default:
                            // Ignoramos otros performatives o los procesamos si es necesario
                            break;
                    }
                } else {
                    block(); // No hay mensajes, bloqueo para liberar CPU
                }
            }
        });
    }
    
    public ACLMessage myReceive() {
        ACLMessage msg = super.receive();
        if (msg != null) {
            // Extraemos datos
            String sender = (msg.getSender() != null) ? msg.getSender().getLocalName() : "unknown";
            String receivers = getReceiverString(msg);
            String performative = ACLMessage.getPerformative(msg.getPerformative());
            String conversationId = (msg.getConversationId() != null) ? msg.getConversationId() : "";
            String content = "hay que abrir DOP para ver info";//(msg.getContent() != null) ? msg.getContent() : "";

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
        String content = "hay que abrir DOP para ver info";//(msg.getContent() != null) ? msg.getContent() : "";

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
        String receiverList = new String();
        jade.util.leap.Iterator it = msg.getAllReceiver();
        while (it.hasNext()) {
            AID r = (AID) it.next();
            receiverList.concat(r.getLocalName()+"/");
        }
        return receiverList;
    }

    // --------------------------------------------------------------------------------------
    // 1. Lógica para procesar CFP (Call for Proposal)
    // --------------------------------------------------------------------------------------
    private void handleCfp(ACLMessage cfp) {
        String shovelName = cfp.getSender().getLocalName();
        /*
        System.out.println(getLocalName() + " - CFP recibido de "
                           + shovelName + ". Contenido: " + cfp.getContent());
		*/
        // Creamos respuesta
        ACLMessage response = cfp.createReply();
		DocumentoOfertaPropuesta dop;
        try {
			dop = (DocumentoOfertaPropuesta) cfp.getContentObject();                
            //System.out.println(controller.getLocalName() + " hora ofertada de inicio de carga"+ccfp.getHoraInicioCarga());
            generaPropuesta(dop);
            
            /*
             * si generaPropuesta==-1
             *    se envia REFUSE
             * de lo contrario
             *    si RL dice que no
             *        se envia REFUSE
             *    de lo contrario
             *        se envia PROPOSE   
             */
            try {
				response.setContentObject(dop);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            if (dop.getHoraLlegadaApalaPropuestoPorCamion()==-1) {
            	response.setPerformative(ACLMessage.REFUSE);
            	mySend(response);
                return;
            } else {
            	if (learningEnabled) {
            		// Con RL: Obtenemos (o creamos) la Q para este shovel
                    double[] qValues = qTable.getOrDefault(shovelName, new double[]{0.0, 0.0});
                    qTable.putIfAbsent(shovelName, qValues);

                    // Elegimos acción (0=REFUSE, 1=PROPOSE) con e-greedy
                    int action = chooseActionEGreedy(qValues);

                    // Guardamos la acción en lastActionChosen (usamos la conversationId)
                    lastActionChosen.put(cfp.getConversationId(), action);

                    if (action == 0 ) {
                        // REFUSE
                        response.setPerformative(ACLMessage.REFUSE);
                        //response.setContent("No me interesa (RL - REFUSE).");
                       // System.out.println(getLocalName()+": envio REFUSE a pesar de que podia enviar Propose. Razon RL");
                    } else {
                        // PROPOSE
                        response.setPerformative(ACLMessage.PROPOSE);
                        //response.setContent(generateArrivalTime(cfp.getContent()));
                    }
            	} else {
            		response.setPerformative(ACLMessage.PROPOSE);
            	}
            	
                mySend(response);
            }
            
            /*
            if (!learningEnabled) {
                // Si NO estamos usando RL, siempre enviamos PROPOSE (por ejemplo)
                response.setPerformative(ACLMessage.PROPOSE);
                response.setContent(generateArrivalTime(cfp.getContent()));
                mySend(response);
                return;
            }
            */

            
            
        } catch (UnreadableException e1) {
            e1.printStackTrace();
        }

        
    }

    // --------------------------------------------------------------------------------------
    // 2. Lógica para procesar ACCEPT_PROPOSAL
    // --------------------------------------------------------------------------------------
    private void handleAcceptProposal(ACLMessage acceptMsg) {
       // System.out.println(getLocalName() + " - ¡Mi propuesta fue ACEPTADA! ");
                         //  + acceptMsg.getContent());

        // Actualizamos la Q-table sólo si el aprendizaje está habilitado
        if (learningEnabled) {
            String shovelName = acceptMsg.getSender().getLocalName();

            // Recuperar la acción que habíamos elegido para este conversationId
            int action = lastActionChosen.getOrDefault(acceptMsg.getConversationId(), 1);

            double reward = +1.0; // recompensa positiva
            updateQValue(shovelName, action, reward);
        }
        // agregamos al schedule
        // Enviamos INFORM indicando que cumpliremos
        
        ACLMessage inform = acceptMsg.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        DocumentoOfertaPropuesta dop=null;
        try {
        	dop=(DocumentoOfertaPropuesta) acceptMsg.getContentObject();
        	if(schedule.estaCamionOcupado(dop)) {
        		System.out.println(this.getLocalName()+": ya se ocupó el slot, debo rechazar");
        		ACLMessage cancel = acceptMsg.createReply();
        		cancel.setPerformative(ACLMessage.CANCEL);
        		//cancel.setContent("ya se ocupó el slot, debo rechazar");
        		mySend(cancel);
        	} else {
        		inform.setContentObject(dop);
        		agregaGanador(dop);
        		 mySend(inform);
        	}
            

			
		} catch (UnreadableException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       // inform.setContent("Camino al Shovel. Gracias por aceptar la propuesta.");
       
    }

    // --------------------------------------------------------------------------------------
    // 3. Lógica para procesar REJECT_PROPOSAL
    // --------------------------------------------------------------------------------------
    private void handleRejectProposal(ACLMessage rejectMsg) {
       /* System.out.println(getLocalName() + " - Mi propuesta fue RECHAZADA. "
                           + rejectMsg.getContent());
        */
        if (learningEnabled) {
            String shovelName = rejectMsg.getSender().getLocalName();

            int action = lastActionChosen.getOrDefault(rejectMsg.getConversationId(), 1);

            double reward = -1.0; // penalización
            updateQValue(shovelName, action, reward);
        }
    }

    

    // --------------------------------------------------------------------------------------
    // 5. Métodos Auxiliares de RL (si learningEnabled=true)
    // --------------------------------------------------------------------------------------

    /**
     * Selecciona acción (0=REFUSE, 1=PROPOSE) usando política e-greedy.
     */
    private int chooseActionEGreedy(double[] qValues) {
        // Exploración
        if (random.nextDouble() < epsilon) {
            return random.nextInt(2); // 0 o 1
        }
        // Explotación
        if (qValues[0] > qValues[1]) {
            return 0; // REFUSE
        } else {
            return 1; // PROPOSE
        }
    }

    /**
     * Actualiza Q(s,a) usando la ecuación:
     * Q(s,a) <- Q(s,a) + alpha * [ r + gamma * max_a' Q(s,a') - Q(s,a) ]
     *
     * Consideramos el "estado" = shovelName, que no cambia.
     * max Q(s,a') => mayor valor entre Q(estado,0) y Q(estado,1).
     */
    private void updateQValue(String shovelName, int action, double reward) {
        double[] qValues = qTable.getOrDefault(shovelName, new double[]{0.0, 0.0});
        double currentQ = qValues[action];

        double maxNextQ = Math.max(qValues[0], qValues[1]);

        double newQ = currentQ + alpha * (reward + gamma * maxNextQ - currentQ);
        qValues[action] = newQ;

        // Almacenar de vuelta
        qTable.put(shovelName, qValues);
        
     // Registrar recompensa acumulada para el episodio actual
        if (episodeRewards.size() <= episodeCount) {
            episodeRewards.add(0.0); // Inicializa episodio si no existe
        }
        episodeRewards.set(episodeCount, episodeRewards.get(episodeCount) + reward);
        endEpisode();
    }
    
    private void endEpisode() {
        episodeCount++;
        logRewards();
    }
    
 // Guardar datos en CSV
    private void logRewards() {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(episodeCount + "," + episodeRewards.get(episodeCount - 1) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(getLocalName() + " finalizando. ¡Adiós!");
    }
    
    public void generaPropuesta(DocumentoOfertaPropuesta dop) {
		// TODO Auto-generated method stub
		dop.setIdCamion(this.getLocalName());
		dop.setCantidadEstimadaDeMaterialATransportar(capacidadCamion);
		
		if (schedule.estaCamionOcupado(dop) ) {
			//System.out.println("camion ocupado, retorno -1");
			dop.setHoraLlegadaApalaPropuestoPorCamion(-1);
			dop.setMotivoRefuse("camion ocupado");
				
		} else {
			calcularTiempos(dop);
			if (!schedule.camionPuedeRealizarCicloPropuesto(dop)) {
				//System.out.println("camion no puede realizar ciclo propuesto, retorno -1");
				dop.setHoraLlegadaApalaPropuestoPorCamion(-1);
				dop.setMotivoRefuse("camion no puede realizar ciclo propuesto");
			} else {
				//System.out.println("camion si puede realizar ciclo propuesto"); 
				//System.out.println(dop.toString()); 
			}
		

		}

	}
    
    private void calcularTiempos(DocumentoOfertaPropuesta dop) {
		// TODO Auto-generated method stub
		//System.out.println(dop.toString());
		/*
		 * hay que calcular duracionCiclo = viajeVacio + carga + viajeCargado
		 * viajeVacio = duracionViaje(sAnterior.getUbicacion(), dop.getUbicacionPala(), velocidadVacio);
		 * carga = (capacidadCamion / capacidadPala)*duracionPalada
		 * viajeCargado = duracionViaje(dop.ubicacionPala, dop.destinoMateriales, velocidadCargado)
		 *   
		 */
		Slot sAnterior = schedule.buscarSlotAnterior(dop.getHoraInicioCargaOfertadaPorPala());		
		//Slot sSiguiente = controllerSchedule.buscarSlotSiguiente(dop.getHoraInicioCargaOfertadaPorPala());
		long duracionViajeVacio=0;
		if (schedule.getSlots().isEmpty()) {
			duracionViajeVacio = duracionViaje(ubicacionInicial, dop.getUbicacionPala(), velocidadVacio);
		} else {			
			duracionViajeVacio = duracionViaje(sAnterior.getUbicacion(), dop.getUbicacionPala(), velocidadVacio);
		}
		//System.out.println("duración viaje vacio "+duracionViajeVacio);
		long duracionCarga = (int)(capacidadCamion / dop.getCapacidadPala())*dop.getDuracionPalada();
		
		long duracionViajeCargado = duracionViaje(dop.getUbicacionPala(), dop.getDestinoMateriales(), velocidadCargado);
		/*
		 * hay que determinar las horas de inicio de las actividades. Conviene que el camion llegue justo a tiempo.
		 * puede suceder lo siguiente:
		 * 1. camion puede llegar antes de la hora ofertada por pala. Aqui conviene que llegue justo a cargar, es decir no espere en pala. mejor espera en su posicion
		 * 2. camion no puede llegar antes. Aqui no hay nada que hacer.
		 */
		if (schedule.getSlots().isEmpty()) {
			dop.setHoraInicioViajeVacioPropuestoPorCamion(0);
			dop.setHoraLlegadaApalaPropuestoPorCamion(duracionViajeVacio);			
		} else {
			if (dop.getHoraInicioCargaOfertadaPorPala()-duracionViajeVacio>=sAnterior.getHoraFin()) {
				dop.setHoraInicioViajeVacioPropuestoPorCamion(dop.getHoraInicioCargaOfertadaPorPala()-duracionViajeVacio);
				dop.setHoraLlegadaApalaPropuestoPorCamion(dop.getHoraInicioCargaOfertadaPorPala());				
			} else {
				dop.setHoraInicioViajeVacioPropuestoPorCamion(sAnterior.getHoraFin());
				dop.setHoraLlegadaApalaPropuestoPorCamion(sAnterior.getHoraFin()+duracionViajeVacio);
			}			
		}
		dop.setHoraInicioCargaPropuestoPorCamion(dop.getHoraLlegadaApalaPropuestoPorCamion());
		dop.setHoraInicioViajeCargadoPropuestoPorCamion(dop.getHoraInicioCargaPropuestoPorCamion()+duracionCarga);
		dop.setHoraLlegadaAlugarDeDescargaPropuestoPorCamion(dop.getHoraInicioViajeCargadoPropuestoPorCamion()+duracionViajeCargado);
		dop.setHoraFinDeDescargaPropuestoPorCamion(dop.getHoraLlegadaAlugarDeDescargaPropuestoPorCamion()+duracionDescarga);
			
	}
    private long duracionViaje(String inicio, String destino, double velocidad) {
		Vertice vInicio=Utilidades.retornaVerticeDesdeGrafoMina(grafoMina, inicio);
		Vertice vDestino=Utilidades.retornaVerticeDesdeGrafoMina(grafoMina, destino);
		/*System.out.println(vInicio.toString());
		System.out.println(vDestino.toString());
		System.out.println(grafoMina.toString());
		*/		
		GraphPath<Vertice, DefaultWeightedEdge> camino = DijkstraShortestPath.findPathBetween(grafoMina, vInicio, vDestino);		
		if (camino==null) {
			//System.out.println("no existe camino entre "+inicio+" y "+destino);
			return -1;
		} else {
			double distancia = camino.getWeight();
			//System.out.println("Distancia "+distancia+" velocidad "+velocidad);
			return (long) ((distancia/velocidad)*60*60*1000);			
		}	
	}
    public void agregaGanador(DocumentoOfertaPropuesta dop) {
		// TODO Auto-generated method stub
		schedule.agregarSlot(this.getLocalName(), "", dop.getHoraInicioViajeVacioPropuestoPorCamion(), dop.getHoraLlegadaApalaPropuestoPorCamion(), dop.getUbicacionPala(), Slot.enum_operacion.VIAJE_VACIO, 0);
		schedule.agregarSlot(this.getLocalName(), dop.getIdPala(), dop.getHoraInicioCargaPropuestoPorCamion(), dop.getHoraInicioViajeCargadoPropuestoPorCamion(), dop.getUbicacionPala(), Slot.enum_operacion.CARGA, dop.getCantidadEstimadaDeMaterialATransportar());
		schedule.agregarSlot(this.getLocalName(), "", dop.getHoraInicioViajeCargadoPropuestoPorCamion(), dop.getHoraLlegadaAlugarDeDescargaPropuestoPorCamion(), dop.getDestinoMateriales(), Slot.enum_operacion.VIAJE_CARGADO, dop.getCantidadEstimadaDeMaterialATransportar());
		schedule.agregarSlot(this.getLocalName(), "", dop.getHoraLlegadaAlugarDeDescargaPropuestoPorCamion(), dop.getHoraFinDeDescargaPropuestoPorCamion(), dop.getDestinoMateriales(), Slot.enum_operacion.DESCARGA, dop.getCantidadEstimadaDeMaterialATransportar());
		//schedule.imprimirSchedule(this.getLocalName());
	}
}
