package org.signature.rsmpEquipment.communication.udp;

/**
 * Interface pour être prévenu de nouvelles connexions
 * @author SDARIZCUREN
 *
 */
public interface IConnexionClientUdp {
	
	/**
	 * Informe de la réception de données sur le port UDP
	 * @param pDatas les données reçues
	 */
	public void receptionDatas(String pDatas);

}
