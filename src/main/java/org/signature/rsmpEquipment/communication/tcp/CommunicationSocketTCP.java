package org.signature.rsmpEquipment.communication.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.communication.rsmp.MessageAvecOrigine;
import org.signature.rsmpEquipment.communication.rsmp.MessageAvecOrigine.Origine;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Traitement d'une connexion client TCP
 * 
 * @author SDARIZCUREN
 *
 */
public abstract class CommunicationSocketTCP extends Thread {
	private final LoggerRsmpEquipment _logger;
	private final PublishSubject<MessageAvecOrigine> _subject;

	private final Socket _clientSocket;
	private BufferedReader _in;
	private BufferedWriter _out;

	public CommunicationSocketTCP(Socket pSocket, Observer<MessageAvecOrigine> pObserver, LoggerRsmpEquipment pLogger) {
		this._clientSocket = pSocket;
		_logger = pLogger;

		// Enregistrement de l'observateur
		_subject = PublishSubject.create();
		_subject.subscribe(pObserver);
	}

	public void run() {
		try {
			_out = new BufferedWriter(new OutputStreamWriter(_clientSocket.getOutputStream()));
		} catch (IOException e) {
			_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR,
					"Erreur création buffer d'écriture", e);

			closeSocketTravail();
			return;
		}

		try {
			_in = new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()));
		} catch (IOException e) {
			_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR,
					"Erreur création buffer de lecture", e);

			closeSocketTravail();
			return;
		}

		// boucle infini en lecture
		lectureSocket();
	}

	// Lecture infini sur le buffer d'entrée
	private void lectureSocket() {
		// Boucle infini en lecture.
		// Sortie si erreur
		StringBuffer sb = new StringBuffer();
		while (true) {
			try {
				if (_in == null) {
					closeSocketTravail();
					return;
				}

				int octet = _in.read();
				// Fin de flux
				if (octet == -1) {
					closeSocketTravail();
					return;
				}

				if (estOctetFinTrame(octet)) {
					if (inclureOctetFinTrame()) {
						sb.append((char) octet);
					}

					if (sb.length() > 0) {

						// Emission des données reçues après suppression des éventuels caractères
						// d'échappement
						String msg = suppressionCaracteresEchappement(sb.toString());

						if (_subject.hasObservers()) {
							_subject.onNext(new MessageAvecOrigine(msg, Origine.SOCKET_TCP));
						} else {
							// Plus personne à l'écoute, je sort

							closeSocketTravail();
							return;
						}
					}

					// Relance une reception de nouveau message
					sb = new StringBuffer();
				} else {
					sb.append((char) octet);
				}
			} catch (Exception e) {
				_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR, "Erreur lecture socket", e);

				closeSocketTravail();
				return;
			}
		}
	}

	/**
	 * Ecriture d'un message sur la socket TCP
	 * 
	 * @param pMsg le message à émettre
	 */
	public void emissionSocketTcp(String pMsg) {
		// Remplacement par les caractères d'echappement si nécessaire
		pMsg = applicationCaracteresEchappement(pMsg);

		// Ajout du caractère séparateur de message
		if (_out != null) {
			try {
				_out.write(pMsg);

				// Si nécessité d'emmetre un flag de fin
				if (avecFlagTerminaison()) {
					_out.write(getFlagTerminaison());
				}

				_out.flush();
			} catch (IOException e) {
				_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR, "Erreur ecriture socket", e);
			}

		}
	}

	// Sur erreur ferme la socket et prévient le service de communication
	private void closeSocketTravail() {
		fermetureSocket();

		if (_subject.hasObservers()) {
			_subject.onComplete();
		}
	}

	/**
	 * Applique les caractères d'échappement à la trame avant émission sur la socket
	 * 
	 * @param pOrigine la trame origine
	 * @return la trame modifiée
	 */
	protected abstract String applicationCaracteresEchappement(String pOrigine);

	/**
	 * Supprime les caractères d'échappement dans la trame reçue
	 * 
	 * @param pOrigine la trame origine
	 * @return la trame modifiée
	 */
	protected abstract String suppressionCaracteresEchappement(String pOrigine);

	/**
	 * Indique si l'octet reçu est le dernier octet attendu
	 * 
	 * @param pOctet l'octet à tester
	 * @return true si c'est le dernier octet
	 */
	protected abstract boolean estOctetFinTrame(int pOctet);

	/**
	 * Indique si l'octet fin de trame est à inclure dans la trame à retourner
	 * 
	 * @return true s'il faut le conserver
	 */
	protected abstract boolean inclureOctetFinTrame();

	/**
	 * Indique si un flag terminaison est à emmètre
	 * 
	 * @return true si besoin d'ememtre un flag terminaison de trame
	 */
	protected abstract boolean avecFlagTerminaison();

	/**
	 * Donne le flag terminaison de trame
	 * 
	 * @return true le flag terminaison de trame
	 */
	protected abstract byte getFlagTerminaison();

	/**
	 * Demande de fermeture de la socket
	 */
	public void fermetureSocket() {
		if (_in != null) {
			try {
				_in.close();
			} catch (IOException e) {
				_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR,
						"Erreur fermeture buffer lecture", e);
			}

			_in = null;
		}

		if (_out != null) {
			try {
				_out.close();
			} catch (IOException e) {
				_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR,
						"Erreur fermeture buffer écriture", e);
			}

			_out = null;
		}

		if (_clientSocket != null) {
			try {
				_clientSocket.close();
			} catch (IOException e) {
				_logger.getLoggerEvenements(CommunicationSocketTCP.class).log(Level.ERROR, "Erreur fermeture socket",
						e);
			}
		}
	}

	/**
	 * Indique si la socket TCP est déconnecté
	 * 
	 * @return true si déconnecté
	 */
	public boolean estDeconnecte() {
		return _clientSocket.isClosed();
	}

}
