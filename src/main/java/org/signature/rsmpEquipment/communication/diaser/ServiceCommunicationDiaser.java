package org.signature.rsmpEquipment.communication.diaser;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.communication.GestionnaireCommunication;
import org.signature.rsmpEquipment.communication.udp.ClientServeurUdp;
import org.signature.rsmpEquipment.communication.udp.IConnexionClientUdp;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;
import org.signature.rsmpEquipment.resources.ConfigurationRsmpEquipment;

import com.google.inject.Inject;

/**
 * Un service chargé de la communication en Diaser avec un équipement externe
 * 
 * @author SDARIZCUREN
 *
 */
public class ServiceCommunicationDiaser implements IConnexionClientUdp {
	private final ClientServeurUdp _clientServeurUdp;
	private final LoggerRsmpEquipment _logger;

	private final String _ipEquipement;
	private final int _udpEquipement;
	
	private GestionnaireCommunication _gestionnairePrincipal;

	/**
	 * Construction de l'objet depuis l'injection de dépendances
	 */
	@Inject
	private ServiceCommunicationDiaser(ClientServeurUdp pClientServeurUdp, ConfigurationRsmpEquipment pConfiguration,
			LoggerRsmpEquipment pLogger) {
		_logger = pLogger;
		_clientServeurUdp = pClientServeurUdp;

		_ipEquipement = pConfiguration.getString("ipEquipementDiaser");
		_udpEquipement = Integer.valueOf(pConfiguration.getString("udpEquipementDiaser"));
	}
	
	/**
	 * Démarrage des services de communication
	 */
	public void lancementServices(GestionnaireCommunication pGestionnairePrincipal) {
		_gestionnairePrincipal = pGestionnairePrincipal;
	}

	/**
	 * Réception d'une commande à transmettre en Diaser à l'équipement externe
	 * 
	 * @param pCommande la commande à transmettre
	 */
	public void receptionCommandeDiaser(String pCommande) {
		_clientServeurUdp.emissionTrame(pCommande, _ipEquipement, _udpEquipement, this);

		// Log de la trame
		_logger.getLoggerDiaserEmis().log(Level.DEBUG, "DIASER --> " + pCommande);
	}

	/**
	 * Informe de la réception de données sur le port UDP
	 * 
	 * @param pDatas les données reçues
	 */
	public void receptionDatas(String pDatas) {
		// Je transmet la commande au gestionnaire principal de communication qui va la
		// transmettre à la couche RSMP
		_gestionnairePrincipal.receptionReponseDepuisSystemeExterne(pDatas);

		// Log de la trame
		_logger.getLoggerDiaserRecus().log(Level.DEBUG, "DIASER --> " + pDatas);
	}

}
