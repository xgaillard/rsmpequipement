package org.signature.rsmpEquipment.communication.tcp;

import java.net.Socket;

import org.signature.rsmpEquipment.communication.rsmp.MessageAvecOrigine;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;

import io.reactivex.rxjava3.core.Observer;

/**
 * Classe contrôlant la communication sur la socket TCP, selon les besoins de
 * l'encapsulation TEDI
 * 
 * @author SDARIZCUREN
 *
 */
public class CommunicationTcpEncapsulationTedi extends CommunicationSocketTCP {
	private final static byte ETX = 0x03;
	private final static byte ETB = 0x17;
	private final static byte ACK = 0x06;
	private final static byte NACK = 0x15;

	private boolean _attenteCksNumeroBloc = false;

	public CommunicationTcpEncapsulationTedi(Socket pSocket, Observer<MessageAvecOrigine> pObserver,
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
		// Rien à faire
		return pOrigine;
	}

	/**
	 * Supprime les caractères d'échappement dans la trame reçue
	 * 
	 * @param pOrigine la trame origine
	 * @return la trame modifiée
	 */
	protected String suppressionCaracteresEchappement(String pOrigine) {
		// Rien à faire
		return pOrigine;
	}

	/**
	 * Indique si l'octet reçu est le dernier octet attendu
	 * 
	 * @param pOctet l'octet à tester
	 * @return true si c'est le dernier octet
	 */
	protected boolean estOctetFinTrame(int pOctet) {
		// Réception du dernier octet de la trame : CKS ou numéro de bloc
		if (_attenteCksNumeroBloc) {
			// Reset pour trame suivant
			_attenteCksNumeroBloc = false;

			return true;
		}

		// Cas mode terminal : Reception ! ou ?
		// TODO PB SI CES CARACTERES APPRAISSENT DANS LE CORPS DE LA TRAME
		if (!_attenteCksNumeroBloc && (pOctet == '!' || pOctet == '?')) {
			return true;
		}

		// Octet fin de trame ou ack/nak, il reste le CKS ou le numéro de bloc à
		// recevoir
		if (pOctet == ETX || pOctet == ETB || pOctet == ACK || pOctet == NACK) {
			_attenteCksNumeroBloc = true;
		}

		return false;
	}

	/**
	 * Indique si l'octet fin de trame est à inclure dans la trame à retourner
	 * 
	 * @return true s'il faut le conserver
	 */
	protected boolean inclureOctetFinTrame() {
		return true;
	}

	/**
	 * Indique si un flag terminaison est à emmètre
	 * 
	 * @return true si besoin d'ememtre un flag terminaison de trame
	 */
	protected boolean avecFlagTerminaison() {
		return false;
	}

	/**
	 * Donne le flag terminaison de trame
	 * 
	 * @return true le flag terminaison de trame
	 */
	protected byte getFlagTerminaison() {
		return 0x00;
	}

}
