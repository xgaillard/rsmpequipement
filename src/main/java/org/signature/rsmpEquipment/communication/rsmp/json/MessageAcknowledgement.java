package org.signature.rsmpEquipment.communication.rsmp.json;

/**
 * Classe  Acknowledgement à mapper en JSON
 * @author SDARIZCUREN
 *
 */
public class MessageAcknowledgement extends MessageRsmp {
	public String mType = "rSMsg";
	public String oMId = "";
	
	public final static String TYPE_MESSAGE = "MessageAck";
	
	public MessageAcknowledgement(String pMsgId) {
		oMId = pMsgId;
		
		super.type = TYPE_MESSAGE;
	}
	

}
