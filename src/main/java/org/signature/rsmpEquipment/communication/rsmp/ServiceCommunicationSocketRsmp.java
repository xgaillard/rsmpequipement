package org.signature.rsmpEquipment.communication.rsmp;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.FabriqueRsmpEquipment;
import org.signature.rsmpEquipment.communication.tcp.CommunicationSocketTCP;
import org.signature.rsmpEquipment.communication.tcp.CommunicationTcpEncapsulationRsmp;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;
import org.signature.rsmpEquipment.resources.ConfigurationRsmpEquipment;

import com.google.inject.Inject;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * CLasse traitant de l'ouverture et fermeture d'une connexion TCP avec un
 * serveur RSMP. Cette classe fait le lien entre les messages reçus et le
 * traitement RSMP à faire
 * 
 * @author SDARIZCUREN
 *
 */
public class ServiceCommunicationSocketRsmp implements Observer<MessageAvecOrigine> {
	private final ConfigurationRsmpEquipment _configuration;
	private final LoggerRsmpEquipment _logger;

	private PublishSubject<String> _subject;

	private List<Disposable> disposable = new ArrayList<>();
	private Observer<String> _observer;

	private TraitementProtocoleRsmp _serviceRsmp;
	private CommunicationSocketTCP _communicationTcp;

	private static final String[] protocols = new String[] { "TLSv1.2" };
	private static final String[] cipher_suites = new String[] { "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", };

	/**
	 * Construction de l'objet depuis l'injection de dépendances
	 */
	@Inject
	private ServiceCommunicationSocketRsmp(ConfigurationRsmpEquipment pConfiguration, LoggerRsmpEquipment pLogger) {
		_configuration = pConfiguration;
		_logger = pLogger;
	}

	/**
	 * Lancement d'une connexion TCP avec le serveur RSMP, et mise en place de la
	 * messagerie RSMP
	 * 
	 * @param pObserver l'observateur à prévenir des commandes reçues
	 */
	public void lancementCommunicationRsmp(Observer<String> pObserver) {
		_observer = pObserver;

		// Enregistrement de l'observateur
		_subject = PublishSubject.create();
		_subject.subscribe(_observer);

		// Un thread infini pour se connecter au serveur
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

			// Création d'un service de traitement du protocole RSMP
			_serviceRsmp = FabriqueRsmpEquipment.INSTANCE.getTraitementProtocoleRsmp();

			_communicationTcp = new CommunicationTcpEncapsulationRsmp(socketTravail, this, _logger);
			// Lancement de la communication
			_communicationTcp.start();

			// Petit temps d'attente pour laisser démarrer le thread TCP
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			_serviceRsmp.setObserver(this);

		}).start();
	}

	// Création d'une nouvelle socket de travail
	private Socket creationNouvelleSocket() {
		String ip = _configuration.getString("ipServeurRsmp");
		int tcp = Integer.valueOf(_configuration.getString("tcpServeurRsmp"));

		// Avec sécurisation TLS
		if (_configuration.getString("avecSecurisationTLS").equals("OUI")) {
			return creationNouvelleSocketTls(ip, tcp);
		}

		// Connexion sans sécurisation TLS
		try {
			return new Socket(ip, tcp);
		} catch (IOException e) {
			_logger.getLoggerEvenements(ServiceCommunicationSocketRsmp.class).log(Level.ERROR,
					"Erreur connexion au serveur", e);

			return null;
		}
	}

	// Création d'une nouvelle socket de travail avec TLS
	private Socket creationNouvelleSocketTls(String pIp, int pTcp) {
		String nomFichier = _configuration.getString("nomFichierClePublique");
		String motPasseFichier = _configuration.getString("motPasseFichierClePublique");

		String trustStore = System.getProperty("user.dir") + File.separator + nomFichier;

		System.setProperty("javax.net.ssl.trustStore", trustStore);
		System.setProperty("javax.net.ssl.trustStorePassword", motPasseFichier);
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");

		SocketFactory sslsocketfactory = SSLSocketFactory.getDefault();
		SSLSocket sslsocket;
		try {
			sslsocket = (SSLSocket) sslsocketfactory.createSocket(pIp, pTcp);
		} catch (IOException e) {
			_logger.getLoggerEvenements(ServiceCommunicationSocketRsmp.class).log(Level.ERROR,
					"Erreur connexion TLS au serveur", e);

			return null;
		}

		String [] lesProtocoles=sslsocket.getSupportedProtocols();
		for(String protocole:lesProtocoles) {
			System.out.println("Protocole : "+protocole);
		}
		String [] lesCipher=sslsocket.getSupportedCipherSuites();
		for(String protocole:lesCipher) {
			System.out.println("Cipher : "+protocole);
		}
		sslsocket.setEnabledProtocols(protocols);
		sslsocket.setEnabledCipherSuites(cipher_suites);

		return sslsocket;
	}

	/**
	 * Réception d'une commande extérieure à traduire en RSMP et à émettre à la
	 * supervision
	 * 
	 * @param pCommande la commande à émettre
	 */
	public void commandePourSupervision(String pCommande) {
		if (pCommande == null || pCommande.length() == 0) {
			return;
		}

		_serviceRsmp.commandePourSupervision(pCommande);
	}

	/**
	 * Indique si la socket TCP est déconnecté
	 * 
	 * @return true si déconnecté
	 */
	public boolean estDeconnecte() {
		return _communicationTcp.estDeconnecte();
	}

	/**
	 * Connection à un observable
	 * 
	 * @param d un objet permettant de mettre fin à la connexion
	 */
	@Override
	public void onSubscribe(@NonNull Disposable d) {
		// J'enregistre l'observable pour mettre fin plus tard à la connexion si besoin
		disposable.add(d);
	}

	/**
	 * Reception d'un message posté par un observable
	 */
	@Override
	public void onNext(@NonNull MessageAvecOrigine msg) {
		switch (msg.origine) {
		case SOCKET_TCP:
			// Nouveau message à analyser par le protocole RSMP
			_serviceRsmp.receptionNouveauMessage(msg.msg);
			break;
		case MESSAGERIE_RSMP:
			// Echange de messages internes par la messagerie RSMP
			_communicationTcp.emissionSocketTcp(msg.msg);
			break;
		case CLIENT_EXTERNE:
			// Message remonté par la messagerie RSMP, qu'il faut passer au client externe
			if (_subject.hasObservers()) {
				_subject.onNext(msg.msg);
			}
			break;
		case INDEFINI:
			// Rien à faire
			break;
		}
	}

	/**
	 * Erreur de communication
	 */
	@Override
	public void onError(@NonNull Throwable e) {
		fermetureToutesCommunications();
	}

	/**
	 * Un observable à mis fin a ses communications
	 */
	@Override
	public void onComplete() {
		fermetureToutesCommunications();
	}

	// Met fin aux observables et coupe les communications
	private void fermetureToutesCommunications() {
		disposable.forEach(d -> d.dispose());

		_communicationTcp.fermetureSocket();
		_serviceRsmp.forceArretCommunicationRsmp();

		disposable = new ArrayList<>();

		// Relance une tentative de connexion au serveur distant
		lancementCommunicationRsmp(_observer);
	}

}
