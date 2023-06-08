package org.signature.rsmpEquipment.communication.rsmp.json;

/**
 * Le champ tunnel d'un message TunnelResponse
 * @author SDARIZCUREN
 *
 */
public class ChampTunnelResponse {
	public String cCI = "M0001";
	public String n = "tunnel";
	public String age = "recent";
	public String v = "";
	
	public ChampTunnelResponse(String val) {
		v = val;
	}

}
