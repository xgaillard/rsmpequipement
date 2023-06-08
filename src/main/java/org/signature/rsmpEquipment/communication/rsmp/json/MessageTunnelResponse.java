package org.signature.rsmpEquipment.communication.rsmp.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe MessageTunnelResponse à mapper en JSON
 * @author SDARIZCUREN
 *
 */
public class MessageTunnelResponse extends MessageRsmp implements IdentifiantMessage {
	
	public String mType = "rSMsg";
	public String mId = "";
	public String ntsOId;
	public String xNId = "";
	public String cId;
	public String cTS;
	public List<ChampTunnelResponse> rvs;
	
	public final static String TYPE_MESSAGE = "CommandResponse";
	
	public MessageTunnelResponse(String pMsgId, String pTxt, String pIdClient, String pTimeStamp) {
		mId = pMsgId;
		ntsOId = pIdClient;
		cId = pIdClient;
		cTS = pTimeStamp;
		
		rvs = new ArrayList<>();
		rvs.add(new ChampTunnelResponse(pTxt));
		
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
