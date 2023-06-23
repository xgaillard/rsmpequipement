package org.signature.rsmpEquipment.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

/**
 * Gestionnaire de log de l'application
 * 
 * @author SDARIZCUREN
 *
 */
public class LoggerRsmpEquipment {
	private final Logger _loggerRsmpEquipmentJsonRecus;
	private final Logger _loggerRsmpEquipmentJsonEmis;
	
	private final Logger _loggerRsmpEquipmentLcrRecus;
	private final Logger _loggerRsmpEquipmentLcrEmis;
	
	private final Logger _loggerRsmpEquipmentDiaserRecus;
	private final Logger _loggerRsmpEquipmentDiaserEmis;

	/**
	 * Construction du gestionnaire depuis sl'injection de dépendances
	 */
	@Inject
	private LoggerRsmpEquipment() {
		_loggerRsmpEquipmentJsonRecus = LogManager.getLogger("LogRsmpEquipmentJsonRecus");
		_loggerRsmpEquipmentJsonEmis = LogManager.getLogger("LogRsmpEquipmentJsonEmis");
		
		_loggerRsmpEquipmentLcrRecus = LogManager.getLogger("LogRsmpEquipmentLcrRecus");
		_loggerRsmpEquipmentLcrEmis = LogManager.getLogger("LogRsmpEquipmentLcrEmis");
		
		_loggerRsmpEquipmentDiaserRecus = LogManager.getLogger("LogRsmpEquipmentDiaserRecus");
		_loggerRsmpEquipmentDiaserEmis = LogManager.getLogger("LogRsmpEquipmentDiaserEmis");
	}

	/**
	 * Demande le logger à utiliser
	 * 
	 * @param pClass la classe à utiliser
	 * 
	 * @return le logger à utiliser pour les évènements et défauts
	 */
	public synchronized Logger getLoggerEvenements(Class<?> pClass) {
		return LogManager.getLogger(pClass);
	}
	
	/**
	 * Demande le logger à utiliser pour les données json reçus
	 * 
	 * @return le logger à utiliser pour les sauvegardes des données json reçus
	 */
	public Logger getLoggerJsonRecus() {
		return _loggerRsmpEquipmentJsonRecus;
	}
	
	/**
	 * Demande le logger à utiliser pour les données json émis
	 * 
	 * @return le logger à utiliser pour les sauvegardes des données json émis
	 */
	public Logger getLoggerJsonEmis() {
		return _loggerRsmpEquipmentJsonEmis;
	}
	
	/**
	 * Demande le logger à utiliser pour les données LCR reçus
	 * 
	 * @return le logger à utiliser pour les sauvegardes des données LCR reçus
	 */
	public Logger getLoggerLcrRecus() {
		return _loggerRsmpEquipmentLcrRecus;
	}

	/**
	 * Demande le logger à utiliser pour les données LCR émis
	 * 
	 * @return le logger à utiliser pour les sauvegardes des données LCR émis
	 */
	public Logger getLoggerLcrEmis() {
		return _loggerRsmpEquipmentLcrEmis;
	}
	
	/**
	 * Demande le logger à utiliser pour les données DIASER reçus
	 * 
	 * @return le logger à utiliser pour les sauvegardes des données DIASER reçus
	 */
	public Logger getLoggerDiaserRecus() {
		return _loggerRsmpEquipmentDiaserRecus;
	}

	/**
	 * Demande le logger à utiliser pour les données DIASER émis
	 * 
	 * @return le logger à utiliser pour les sauvegardes des données DIASER émis
	 */
	public Logger getLoggerDiaserEmis() {
		return _loggerRsmpEquipmentDiaserEmis;
	}
}
