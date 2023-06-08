package org.signature.rsmpEquipment;

import org.apache.logging.log4j.Level;

/**
 * Classe principale de l'application
 * 
 * @author SDARIZCUREN
 *
 */
public class RsmpEquipment {
	public static void main(String[] args) {
		new RsmpEquipment();
	}

	public RsmpEquipment() {
		// Lancement des services de communication
		FabriqueRsmpEquipment.INSTANCE.getGestionnaireCommunication().lancementServices();

		FabriqueRsmpEquipment.INSTANCE.getLoggerRsmpEquipment().getLoggerEvenements(RsmpEquipment.class).log(Level.INFO,
				"Démarrage du service RSMP Equipment");
	}
}
