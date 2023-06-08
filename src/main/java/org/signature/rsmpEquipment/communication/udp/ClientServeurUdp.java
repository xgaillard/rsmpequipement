package org.signature.rsmpEquipment.communication.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;

import com.google.inject.Inject;

/**
 * Implémentation d'un client serveur UDP
 * 
 * @author SDARIZCUREN
 *
 */
public class ClientServeurUdp {
	private final LoggerRsmpEquipment _logger;

	private byte[] _bufferReception = new byte[TAILLE_MAX];

	private final static int TAILLE_MAX = 256;

	/**
	 * Construction de l'objet depuis l'injection de dépendances
	 */
	@Inject
	private ClientServeurUdp(LoggerRsmpEquipment pLogger) {
		_logger = pLogger;
	}

	/**
	 * Emission d'une trame à destination d'un équipement UDP
	 * 
	 * @param pTrame     la trame à émettre
	 * @param pAdresseIp l'adresse IP de l'équipement
	 * @param pPortUdp   le port UDP distant
	 * @param pListener  le listener à prévenir avec la réponse reçue
	 */
	public void emissionTrame(String pTrame, String pAdresseIp, int pPortUdp, IConnexionClientUdp pListener) {
		DatagramSocket socket;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			_logger.getLoggerEvenements(ClientServeurUdp.class).log(Level.ERROR, "Erreur creation socket client UDP",
					e);
			return;
		}

		byte[] buf = pTrame.getBytes(StandardCharsets.ISO_8859_1);

		InetAddress adresse;

		try {
			adresse = InetAddress.getByName(pAdresseIp);
		} catch (UnknownHostException e) {
			_logger.getLoggerEvenements(ClientServeurUdp.class).log(Level.ERROR, "Erreur creation InetAddress", e);

			socket.close();
			return;
		}

		DatagramPacket packet = new DatagramPacket(buf, buf.length, adresse, pPortUdp);
		try {
			socket.send(packet);
		} catch (IOException e) {
			_logger.getLoggerEvenements(ClientServeurUdp.class).log(Level.ERROR,
					"Erreur emission données vers le serveur UDP", e);
		}

		// Je me met à l'écoute de la réponse de l'équipement
		lectureDonnees(pListener, socket);
	}

	// Retournes les données reçues sur le port UDP
	private void lectureDonnees(IConnexionClientUdp pListener, DatagramSocket pSocket) {
		new Thread(() -> {
			if (pSocket != null && !pSocket.isClosed()) {
				DatagramPacket packet = new DatagramPacket(_bufferReception, _bufferReception.length);

				try {
					// J'arme un timeout 10s pour ne pas attendre une réponse infiniment
					pSocket.setSoTimeout(10000);
					
					// Méthode bloquante
					pSocket.receive(packet);

					byte[] recus = packet.getData();
					
					// Je reconstruit un buffer avec la taille reçue
					byte[] retour = new byte[packet.getLength()];
					for(int i=0; i<retour.length; i++) {
						retour[i] = recus[i];
					}

					pListener.receptionDatas(new String(retour));

					// Fermeture de la socket
					fermetureSocket(pSocket);
				} catch (Exception e) {
					_logger.getLoggerEvenements(ClientServeurUdp.class).log(Level.ERROR, "Erreur réception données UDP",
							e);
				}
			}
		}).start();
	}

	// Ferme la socket
	private void fermetureSocket(DatagramSocket pSocket) {
		if (pSocket == null) {
			return;
		}

		try {
			pSocket.disconnect();
		} catch (Exception e) {
			_logger.getLoggerEvenements(ClientServeurUdp.class).log(Level.ERROR,
					"Erreur disconnect de la connexion client UDP", e);
		}

		try {
			pSocket.close();
		} catch (Exception e) {
			_logger.getLoggerEvenements(ClientServeurUdp.class).log(Level.ERROR,
					"Erreur close de la connexion client UDP", e);
		}
	}
}
