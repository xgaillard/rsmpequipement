package org.signature.rsmpEquipment.communication.rsmp;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.communication.rsmp.MessageAvecOrigine.Origine;
import org.signature.rsmpEquipment.communication.rsmp.json.IdentifiantMessage;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageAcknowledgement;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageNotAcknowledgement;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageRsmp;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageTunnelRequest;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageTunnelResponse;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageVersion;
import org.signature.rsmpEquipment.communication.rsmp.json.MessageWatchdog;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;
import org.signature.rsmpEquipment.resources.ConfigurationRsmpEquipment;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Traitement du protocole RSMP
 * 
 * @author SDARIZCUREN
 *
 */
public class TraitementProtocoleRsmp {
	private final ConfigurationRsmpEquipment _configuration;
	private final LoggerRsmpEquipment _logger;

	private PublishSubject<MessageAvecOrigine> _subject;

	private boolean _attenteAcquittementMsgVersion = true;
	private List<HorodateMessageRsmp> _messagesEnvoyes;

	private boolean _attenteMsgVersionSupervision = true;
	private boolean _arretCommunication = false;

	private final int FREQUENCE_EMISSION_WATCHDOG;

	private final int ANCIENNETE_MAX_MESSAGES;

	private final static int FREQUENCE_CONTROLE_RECEPTION_ACK = 1000;

	/**
	 * Construction de l'objet depuis l'injection de dépendances
	 */
	@Inject
	private TraitementProtocoleRsmp(ConfigurationRsmpEquipment pConfiguration, LoggerRsmpEquipment pLogger) {
		_configuration = pConfiguration;
		_logger = pLogger;

		_messagesEnvoyes = new CopyOnWriteArrayList<>();

		FREQUENCE_EMISSION_WATCHDOG = Integer.valueOf(_configuration.getString("frequenceMessageWatchdog"));
		ANCIENNETE_MAX_MESSAGES = Integer.valueOf(_configuration.getString("attenteMaxMessageAcquittement"));
	}

	/**
	 * Initialise l'observer en attente des messages à émettre ou des problèmes dans
	 * la messagerie RSMP
	 * 
	 * @param pObserver l'observateur à l'écoute des évènements RSMP
	 */
	protected void setObserver(Observer<MessageAvecOrigine> pObserver) {
		// Enregistrement de l'observateur
		_subject = PublishSubject.create();
		_subject.subscribe(pObserver);

		// En premier il faut émettre un message Version
		Gson gson = new Gson();

		String idClient = _configuration.getString("idClientRsmp");
		List<String> versionsRsmp = Arrays.stream(_configuration.getString("listeVersionsRsmp").split(";"))
				.collect(Collectors.toList());

		String versionSxl = _configuration.getString("versionSxlSupporte");

		MessageVersion msg = new MessageVersion(UUID.randomUUID().toString(), idClient, versionsRsmp, versionSxl);
		// Ajout dans la liste des messages en attente d'acquittement
		_messagesEnvoyes.add(new HorodateMessageRsmp(msg, LocalDateTime.now()));

		String json = gson.toJson(msg);
		if (_subject.hasObservers()) {
			_subject.onNext(new MessageAvecOrigine(json, Origine.MESSAGERIE_RSMP));
		} else {
			// Plus personne à l'écoute, je sort
			forceArretCommunicationRsmp();
			return;
		}

		// Log de la trame émise
		_logger.getLoggerJsonEmis().log(Level.DEBUG, json);

		// Lancement d'un service de surveillance des messages en attente
		// d'acquittement pour arrêter les services si un acquittement n'arrive pas dans
		// les temps
		demarreSurveillanceMessagesEnAttente();
	}

	/**
	 * Prévient de la réception d'un nouveau message sur le canal de communication
	 * 
	 * @param pMsg le message RSMP à traiter
	 */
	protected synchronized void receptionNouveauMessage(String pMsg) {
		if (pMsg == null || pMsg.length() == 0) {
			return;
		}

		// Log de la trame reçue
		_logger.getLoggerJsonRecus().log(Level.DEBUG, pMsg);

		// Test reception message Version
		Gson gson = new Gson();
		MessageRsmp typeMessageRsmp;

		try {
			typeMessageRsmp = gson.fromJson(pMsg, MessageRsmp.class);
		} catch (JsonSyntaxException e) {
			_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.DEBUG,
					"Erreur decodage JSON du message Type RSMP", e);
			return;
		}

		if (typeMessageRsmp == null || typeMessageRsmp.type == null) {
			return;
		}

		// Cas attente acquittement du message version
		if (_attenteAcquittementMsgVersion) {
			if (MessageAcknowledgement.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
				traitementAcquittementMessageVersion(pMsg);
			} else if (MessageNotAcknowledgement.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
				traitementNonAcquittementMessageVersion(pMsg);
			}

			return;
		}

		// Cas reception message Version Supervision
		if (_attenteMsgVersionSupervision) {
			if (MessageVersion.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
				traitementReceptionMessageVersionSupervision(pMsg);
			}

			return;
		}

		// Cas reception d'un message watchdog
		if (MessageWatchdog.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
			traitementReceptionMessageWatchdog(pMsg);

			return;
		}

		// Cas reception d'un acquittement ou non acquittement
		if (MessageAcknowledgement.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
			traitementAcquittementMessage(pMsg);

			return;
		} else if (MessageNotAcknowledgement.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
			traitementNonAcquittementMessage(pMsg);

			return;
		}

		// AUTRES TYPES DE MESSAGES A FAIRE

		// Cas reception d'un message transporté par un tunnel RSMP
		if (MessageTunnelRequest.TYPE_MESSAGE.equalsIgnoreCase(typeMessageRsmp.type)) {
			traitementReceptionMessageTunnelRequest(pMsg);

			return;
		}
	}

	// Traitement reception de l'acquittement d'un message version
	private void traitementAcquittementMessageVersion(String pMsg) {
		// Si mon message version est acquitté
		if (traitementAcquittementMessage(pMsg)) {
			_attenteAcquittementMsgVersion = false;
		}
	}

	// Traitement reception non acquittement d'un message version
	private void traitementNonAcquittementMessageVersion(String pMsg) {
		traitementNonAcquittementMessage(pMsg);
	}

	// Traitement réception du message version de la supervision
	private void traitementReceptionMessageVersionSupervision(String pMsg) {
		Gson gson = new Gson();
		MessageVersion msgVer;

		try {
			msgVer = gson.fromJson(pMsg, MessageVersion.class);
		} catch (JsonSyntaxException e) {
			_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.DEBUG,
					"Erreur decodage JSON du message version SUpervision", e);
			return;
		}

		if (msgVer == null) {
			return;
		}

		// TODO traitement des informations de version à faire. Emission ack ou nack

		// Envoi du message d'acquittement à la supervision
		emissionMessageAcquittement(msgVer.mId);

		_attenteMsgVersionSupervision = false;

		// Je peux mettre en place l'émission watchdog vers la supervision
		demarreEmissionWatchdog();
	}

	// Traitement réception du message watchog de la supervision
	private void traitementReceptionMessageWatchdog(String pMsg) {
		Gson gson = new Gson();
		MessageWatchdog msgWdog;

		try {
			msgWdog = gson.fromJson(pMsg, MessageWatchdog.class);
		} catch (JsonSyntaxException e) {
			_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.DEBUG,
					"Erreur decodage JSON du message watchdog SUpervision", e);
			return;
		}

		if (msgWdog == null) {
			return;
		}

		// Envoi du message d'acquittement à la supervision
		emissionMessageAcquittement(msgWdog.mId);
	}

	// Traitement reception de l'acquittement d'un message
	// Supprime le message en attente de cet acquittement
	// Retourne true si OK, sinon false
	private boolean traitementAcquittementMessage(String pMsg) {
		Gson gson = new Gson();
		MessageAcknowledgement msgAck;

		try {
			msgAck = gson.fromJson(pMsg, MessageAcknowledgement.class);
		} catch (JsonSyntaxException e) {
			_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.DEBUG,
					"Erreur decodage JSON du message d'acquittment", e);
			return false;
		}

		if (msgAck == null) {
			return false;
		}

		// Récupération du message en attente
		IdentifiantMessage msg = getMessageAvecUUID(msgAck.oMId);

		// Cas message reçu n'est pas l'acquittement du message en attente
		if (msg == null) {
			return false;
		}

		// Mon message est acquitté
		supprimeMessageEnAttente(msg);

		return true;
	}

	// Traitement reception non acquittement d'un message
	private void traitementNonAcquittementMessage(String pMsg) {
		Gson gson = new Gson();
		MessageNotAcknowledgement msgNotAck;

		try {
			msgNotAck = gson.fromJson(pMsg, MessageNotAcknowledgement.class);
		} catch (JsonSyntaxException e) {
			_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.DEBUG,
					"Erreur decodage JSON du message non acquittment", e);
			return;
		}

		if (msgNotAck == null) {
			return;
		}

		// Récupération du message en attente
		IdentifiantMessage msg = getMessageAvecUUID(msgNotAck.oMId);

		// Cas message reçu n'est pas l'acquittement d'un message en attente
		if (msg == null) {
			return;
		}

		// Un de mes messages est refusé, je coupe la communication RSMP
		// TODO A reflechir à un autre traitement ...
		forceArretCommunicationRsmp();
	}

	// Reception d'un message dans un tunnel RSMP
	private void traitementReceptionMessageTunnelRequest(String pMsg) {
		Gson gson = new Gson();
		MessageTunnelRequest msgTxt;

		try {
			msgTxt = gson.fromJson(pMsg, MessageTunnelRequest.class);
		} catch (JsonSyntaxException e) {
			_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.DEBUG,
					"Erreur decodage JSON du message MessageTunnelRequest", e);
			return;
		}

		if (msgTxt == null) {
			return;
		}

		// Si la commande est vide
		if (msgTxt.arg == null || msgTxt.arg.size() == 0 || msgTxt.arg.get(0).v == null) {
			return;
		}

		// Passe la réponse à la couche de gestion des communications externes
		if (_subject.hasObservers()) {
			_subject.onNext(new MessageAvecOrigine(msgTxt.arg.get(0).v, Origine.CLIENT_EXTERNE));
		} else {
			// Plus personne à l'écoute, je sort
			forceArretCommunicationRsmp();
			return;
		}

		// Envoi du message d'acquittement à la supervision
		emissionMessageAcquittement(msgTxt.mId);
	}

	// Acquittement du message reçu
	private void emissionMessageAcquittement(String pIdMsgAAcquitter) {
		// Rien à acquitter si chaîne ID vide
		if (pIdMsgAAcquitter == null || pIdMsgAAcquitter.trim().length() == 0) {
			return;
		}

		Gson gson = new Gson();

		MessageAcknowledgement msgAck = new MessageAcknowledgement(pIdMsgAAcquitter);
		String json = gson.toJson(msgAck);
		if (_subject.hasObservers()) {
			_subject.onNext(new MessageAvecOrigine(json, Origine.MESSAGERIE_RSMP));
		} else {
			// Plus personne à l'écoute, je sort
			forceArretCommunicationRsmp();
			return;
		}

		// Log de la trame émise
		_logger.getLoggerJsonEmis().log(Level.DEBUG, json);
	}

	// Retourne le message avec cet UUID ou null si non trouvé
	private IdentifiantMessage getMessageAvecUUID(String pUuid) {
		if (pUuid == null || pUuid.length() == 0) {
			return null;
		}

		HorodateMessageRsmp horoMsg = _messagesEnvoyes.stream()
				.filter(m -> pUuid.equals(m._message.getIdentifiantMessage())).findFirst().orElse(null);

		return horoMsg != null ? horoMsg._message : null;
	}

	// Supprime ce message dans la liste
	private void supprimeMessageEnAttente(IdentifiantMessage pMsg) {
		HorodateMessageRsmp horoMsg = _messagesEnvoyes.stream().filter(m -> m._message.equals(pMsg)).findFirst()
				.orElse(null);

		if (horoMsg != null) {
			_messagesEnvoyes.remove(horoMsg);
		}
	}

	/**
	 * Réception d'une commande extérieure à traduire en RSMP et à émettre à la
	 * supervision
	 * 
	 * @param pCommande la commande à émettre
	 */
	protected void commandePourSupervision(String pCommande) {
		Gson gson = new Gson();

		String idClient = _configuration.getString("idClientRsmp");

		MessageTunnelResponse msgTunnelResponse = new MessageTunnelResponse(UUID.randomUUID().toString(), pCommande,
				idClient, LocalDateTime.now().toString());

		String json = gson.toJson(msgTunnelResponse);

		// Ajout dans la liste des messages en attente d'acquittement
		_messagesEnvoyes.add(new HorodateMessageRsmp(msgTunnelResponse, LocalDateTime.now()));

		if (_subject.hasObservers()) {
			_subject.onNext(new MessageAvecOrigine(json, Origine.MESSAGERIE_RSMP));
		} else {
			// Plus personne à l'écoute, je sort
			forceArretCommunicationRsmp();
			return;
		}

		// Log de la trame émise
		_logger.getLoggerJsonEmis().log(Level.DEBUG, json);
	}

	/**
	 * Demande à arrêter la communication RSMP
	 */
	protected void forceArretCommunicationRsmp() {
		_arretCommunication = true;

		if (_subject.hasObservers()) {
			_subject.onComplete();
		}

		_logger.getLoggerEvenements(TraitementProtocoleRsmp.class).log(Level.ERROR,
				"Problème messagerie RSMP ou demande arrêt extérieur -> Arrêt communication");
	}

	// Emission watchdog à fréquence régulière
	private void demarreEmissionWatchdog() {
		new Thread(() -> {
			while (!_arretCommunication) {
				Gson gson = new Gson();

				MessageWatchdog msgWDog = new MessageWatchdog(UUID.randomUUID().toString(),
						LocalDateTime.now().toString());
				String json = gson.toJson(msgWDog);

				// Ajout dans la liste des messages en attente d'acquittement
				_messagesEnvoyes.add(new HorodateMessageRsmp(msgWDog, LocalDateTime.now()));

				if (_subject.hasObservers()) {
					_subject.onNext(new MessageAvecOrigine(json, Origine.MESSAGERIE_RSMP));
				} else {
					// Plus personne à l'écoute, je sort
					forceArretCommunicationRsmp();
					return;
				}

				// Log de la trame émise
				_logger.getLoggerJsonEmis().log(Level.DEBUG, json);

				try {
					Thread.sleep(FREQUENCE_EMISSION_WATCHDOG);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

	}

	// Contrôle des message en attente d'acuqittement, à fréquence régulière
	private void demarreSurveillanceMessagesEnAttente() {
		new Thread(() -> {
			while (!_arretCommunication) {
				LocalDateTime now = LocalDateTime.now();

				// Contrôle de l'horodate sur chaque message en attente
				for (HorodateMessageRsmp msgEnAttente : _messagesEnvoyes) {
					if (now.minus(ANCIENNETE_MAX_MESSAGES, ChronoUnit.MILLIS).isAfter(msgEnAttente._horodate)) {
						// Acquittement non reçu, je sort
						forceArretCommunicationRsmp();

						break;
					}
				}

				try {
					Thread.sleep(FREQUENCE_CONTROLE_RECEPTION_ACK);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

	}

}
