package org.signature.rsmpEquipment.communication.rsmp.json;

/**
 * Classe Watchdog à mapper en JSON
 * @author SDARIZCUREN
 *
 */
public class MessageWatchdog extends MessageRsmp implements IdentifiantMessage {
	public String mType = "rSMsg";
	public String mId = "";
	public String wTs = "";
	
	public final static String TYPE_MESSAGE = "Watchdog";
	
	public MessageWatchdog(String pMsgId, String pTimeStamp) {
		mId = pMsgId;
		wTs = pTimeStamp;
		
		super.type = TYPE_MESSAGE;
	}
	
	/**
	 * Donne l'UUID du message
	 * 
	 * @return son identifiant
	 */
	public String getIdentifiantMessage() {
		return mId;
	}
	

}
