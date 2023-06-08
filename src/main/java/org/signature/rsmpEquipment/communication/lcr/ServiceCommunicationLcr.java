package org.signature.rsmpEquipment.communication.lcr;

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.communication.GestionnaireCommunication;
import org.signature.rsmpEquipment.communication.rsmp.MessageAvecOrigine;
import org.signature.rsmpEquipment.communication.tcp.CommunicationTcpEncapsulationTedi;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;
import org.signature.rsmpEquipment.resources.ConfigurationRsmpEquipment;

import com.google.inject.Inject;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Un service chargé de la communication en LCR avec un équipement externe
 * 
 * @author SDARIZCUREN
 *
 */
public class ServiceCommunicationLcr implements Observer<MessageAvecOrigine> {
	private final LoggerRsmpEquipment _logger;
	private final String _ipEquipement;
	private final int _tcpEquipement;

	private GestionnaireCommunication _gestionnairePrincipal;
	private CommunicationTcpEncapsulationTedi _comTedi;

	/**
	 * Construction de l'objet depuis l'injection de dépendances
	 */
	@Inject
	private ServiceCommunicationLcr(ConfigurationRsmpEquipment pConfiguration, LoggerRsmpEquipment pLogger) {
		_logger = pLogger;

		_ipEquipement = pConfiguration.getString("ipEquipementLcr");
		_tcpEquipement = Integer.valueOf(pConfiguration.getString("tcpEquipementLcr"));
	}

	/**
	 * Démarrage des services de communication
	 */
	public void lancementServices(GestionnaireCommunication pGestionnairePrincipal) {
		_gestionnairePrincipal = pGestionnairePrincipal;

		// Un thread infini pour se connecter à l'équipement
		new Thread(() -> {
			// Boucle infini
			Socket socketTravail = null;
			while (socketTravail == null) {
				socketTravail = creationNouvelleSocket();
				// Pose 1s entre deux tentatives de création
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

			// Communication TEDI-LCR. Je traite une seule connexion à la fois
			_comTedi = new CommunicationTcpEncapsulationTedi(socketTravail, this, _logger);
			_comTedi.start();

		}).start();

	}

	// Création d'une nouvelle socket de travail
	private Socket creationNouvelleSocket() {
		try {
			return new Socket(_ipEquipement, _tcpEquipement);
		} catch (IOException e) {
			_logger.getLoggerEvenements(ServiceCommunicationLcr.class).log(Level.ERROR,
					"Erreur connexion à l'équipement", e);

			return null;
		}
	}

	/**
	 * Réception d'une commande à transmettre en LCR à l'équipement externe
	 * 
	 * @param pCommande la commande à transmettre
	 */
	public void receptionCommandeLcr(String pCommande) {
		if(_comTedi != null && !_comTedi.estDeconnecte()) {
			// Emission de la commande
			_comTedi.emissionSocketTcp(pCommande);

			// Log de la trame
			_logger.getLoggerLcrEmis().log(Level.DEBUG, "LCR --> " + pCommande);
		}
	}

	/**
	 * Connection à un observable
	 * 
	 * @param d un objet permettant de mettre fin à la connexion
	 */
	@Override
	public void onSubscribe(@NonNull Disposable d) {

	}

	/**
	 * Reception d'un message posté par un observable
	 */
	@Override
	public void onNext(@NonNull MessageAvecOrigine msg) {
		// Je transmet la commande au gestionnaire principal de communication qui va la
		// transmettre à la couche RSMP
		_gestionnairePrincipal.receptionReponseDepuisSystemeExterne(msg.msg);

		// Log de la trame
		_logger.getLoggerLcrRecus().log(Level.DEBUG, "LCR --> " + msg.msg);
	}

	/**
	 * Erreur de communication
	 */
	@Override
	public void onError(@NonNull Throwable e) {
		_comTedi.fermetureSocket();
		_comTedi = null;
		
		// Je lance une nouvelle connexion
		lancementServices(_gestionnairePrincipal);
	}

	/**
	 * Un observable à mis fin a ses communications
	 */
	@Override
	public void onComplete() {
		_comTedi.fermetureSocket();
		_comTedi = null;
		
		// Je lance une nouvelle connexion
		lancementServices(_gestionnairePrincipal);
	}

}
