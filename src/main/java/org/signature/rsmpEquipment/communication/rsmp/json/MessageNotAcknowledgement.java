package org.signature.rsmpEquipment.communication.rsmp.json;

/**
 * Classe NotAcknowledgement à mapper en JSON
 * @author SDARIZCUREN
 *
 */
public class MessageNotAcknowledgement extends MessageRsmp {
	public String mType = "rSMsg";
	public String oMId = "";
	public String rea = "";
	
	public final static String TYPE_MESSAGE = "MessageNotAck";
	
	public MessageNotAcknowledgement(String pMsgId, String pMsgError) {
		oMId = pMsgId;
		rea = pMsgError;
		
		super.type = TYPE_MESSAGE;
	}
	

}
