package org.signature.rsmpEquipment.communication.rsmp.json;

/**
 * Le champ tunnel d'un message TunnelRequest
 * @author SDARIZCUREN
 *
 */
public class ChampTunnelRequest {
	public String cCI = "M0001";
	public String n = "tunnel";
	public String cO = "sendData";
	public String v = "";
	
	public ChampTunnelRequest(String val) {
		v = val;
	}

}
