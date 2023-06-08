package org.signature.rsmpEquipment.communication.rsmp;

/**
 * Association d'un message texte avec une origine socket TCP, messagerie RSMP
 * (les messages internes RSMP) ou client externe pour un message transporté par
 * la messagerie RSMP, mais provenant d'un client externe
 * 
 * @author SDARIZCUREN
 *
 */
public class MessageAvecOrigine {
	public static enum Origine {
		SOCKET_TCP, MESSAGERIE_RSMP, CLIENT_EXTERNE, INDEFINI
	}

	public final String msg;
	public final Origine origine;

	/**
	 * Construction du message
	 * 
	 * @param pMsg  le message à transmettre
	 * @param pDest l'origine du message
	 */
	public MessageAvecOrigine(String pMsg, Origine pOrigine) {
		msg = pMsg;
		origine = pOrigine;
	}
}
