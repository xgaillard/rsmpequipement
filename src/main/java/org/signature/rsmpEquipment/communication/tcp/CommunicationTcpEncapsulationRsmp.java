package org.signature.rsmpEquipment.communication.tcp;

import java.net.Socket;

import org.signature.rsmpEquipment.communication.rsmp.MessageAvecOrigine;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;

import io.reactivex.rxjava3.core.Observer;

/**
 * Classe contrôlant la communication sur la socket TCP, selon les besoins de
 * l'encapsulation RSMP
 * 
 * @author SDARIZCUREN
 *
 */
public class CommunicationTcpEncapsulationRsmp extends CommunicationSocketTCP {
	private final static byte OCTET_FIN = 0x0c;
	private final static String ECHAPPEMENT = new String(new byte[] { 0x7F, 0x00, 0x7F });

	public CommunicationTcpEncapsulationRsmp(Socket pSocket, Observer<MessageAvecOrigine> pObserver,
			LoggerRsmpEquipment pLogger) {
		super(pSocket, pObserver, pLogger);
	}
	
	/**
	 * Applique les caractères d'échappement à la trame avant émission sur la socket
	 * 
	 * @param pOrigine la trame origine
	 * @return la trame modifiée
	 */
	protected String applicationCaracteresEchappement(String pOrigine) {
		return pOrigine.replaceAll(new String(new byte[] { OCTET_FIN }), ECHAPPEMENT);
	}

	/**
	 * Supprime les caractères d'échappement dans la trame reçue
	 * 
	 * @param pOrigine la trame origine
	 * @return la trame modifiée
	 */
	protected String suppressionCaracteresEchappement(String pOrigine) {
		return pOrigine.replaceAll(ECHAPPEMENT, new String(new byte[] { OCTET_FIN }));
	}
	
	/**
	 * Indique si l'octet reçu est le dernier octet attendu
	 * 
	 * @param pOctet l'octet à tester
	 * @return true si c'est le dernier octet
	 */
	protected boolean estOctetFinTrame(int pOctet) {
		return pOctet == OCTET_FIN;
	}
	
	/**
	 * Indique si l'octet fin de trame est à inclure dans la trame à retourner
	 * 
	 * @return true s'il faut le conserver
	 */
	protected boolean inclureOctetFinTrame() {
		return false;
	}
	
	/**
	 * Indique si un flag terminaison est à emmètre
	 * @return true si besoin d'ememtre un flag terminaison de trame
	 */
	protected boolean avecFlagTerminaison() {
		return true;
	}
	
	/**
	 * Donne le flag terminaison de trame
	 * @return true le flag terminaison de trame
	 */
	protected byte getFlagTerminaison() {
		return OCTET_FIN;
	}

}
