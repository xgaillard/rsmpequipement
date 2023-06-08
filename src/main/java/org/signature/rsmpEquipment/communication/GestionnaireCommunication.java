package org.signature.rsmpEquipment.communication;

import org.signature.rsmpEquipment.communication.diaser.ServiceCommunicationDiaser;
import org.signature.rsmpEquipment.communication.lcr.ServiceCommunicationLcr;
import org.signature.rsmpEquipment.communication.rsmp.ServiceCommunicationSocketRsmp;
import org.signature.rsmpEquipment.resources.ConfigurationRsmpEquipment;

import com.google.inject.Inject;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Gestion des communications avec la Supervision et le client RSMP
 * 
 * @author SDARIZCUREN
 *
 */
public class GestionnaireCommunication implements Observer<String> {
	private final ServiceCommunicationSocketRsmp _serviceCommunicationSocketRsmp;
	private final ServiceCommunicationLcr _serviceCommunicationLcr;
	private final ServiceCommunicationDiaser _serviceCommunicationDiaser;

	// True pour LCR, false pour Diaser
	private final LCR_DIASER _communicationLcrDiaser;

	private enum LCR_DIASER {
		LCR, DIASER
	}

	/**
	 * Construction de l'objet depuis l'injection de dépendances
	 */
	@Inject
	private GestionnaireCommunication(ServiceCommunicationSocketRsmp pServiceCommunicationSocketRsmp,
			ServiceCommunicationLcr pServiceLcr, ServiceCommunicationDiaser pServiceCommunicationDiaser,
			ConfigurationRsmpEquipment pConfiguration) {
		_serviceCommunicationSocketRsmp = pServiceCommunicationSocketRsmp;
		_serviceCommunicationLcr = pServiceLcr;
		_serviceCommunicationDiaser = pServiceCommunicationDiaser;

		_communicationLcrDiaser = pConfiguration.getString("communicationLCR_DIASER").equals("LCR") ? LCR_DIASER.LCR
				: LCR_DIASER.DIASER;
	}

	/**
	 * Démarrage des services de communication
	 */
	public void lancementServices() {
		_serviceCommunicationSocketRsmp.lancementCommunicationRsmp(this);
		
		if (_communicationLcrDiaser == LCR_DIASER.LCR) {
			_serviceCommunicationLcr.lancementServices(this);
		} else {
			_serviceCommunicationDiaser.lancementServices(this);
		}
	}

	/**
	 * Reception d'une réponse à la commande émise par le système externe. La
	 * réponse est à transmettre à la couche RSMP
	 * 
	 * @param pReponse la réponse reçue
	 */
	public void receptionReponseDepuisSystemeExterne(String pReponse) {
		if (pReponse == null) {
			return;
		}

		_serviceCommunicationSocketRsmp.commandePourSupervision(pReponse);
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
	public void onNext(@NonNull String msg) {
		// Emission en LCR ou Diaser vers l'équipement externe
		if (_communicationLcrDiaser == LCR_DIASER.LCR) {
			_serviceCommunicationLcr.receptionCommandeLcr(msg);
		} else {
			_serviceCommunicationDiaser.receptionCommandeDiaser(msg);
		}

//		receptionReponseDepuisSystemeExterne(
//				new String(new byte[] { 0x02 }) + "123021/06/21 18:58:13" + new String(new byte[] { 0x03 }) + "#");
	}

	/**
	 * Erreur de communication
	 */
	@Override
	public void onError(@NonNull Throwable e) {
	}

	/**
	 * Un observable à mis fin a ses communications
	 */
	@Override
	public void onComplete() {
	}
}
