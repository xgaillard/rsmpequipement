package org.signature.rsmpEquipment.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;

import com.google.inject.Inject;

/**
 * Gestionnaire de configuration de l'application
 * 
 * @author SDARIZCUREN
 *
 */
public class ConfigurationRsmpEquipment {
	private Properties _prop = new Properties();

	private final static String PATH_FILE_CONFIG = "resources" + File.separator + "Configuration.properties";

	/**
	 * Construction du gestionnaire depuis sl'injection de dépendances
	 */
	@Inject
	private ConfigurationRsmpEquipment(LoggerRsmpEquipment pLogger) {
		InputStream input = null;

		try {
			input = new FileInputStream(PATH_FILE_CONFIG);

			// Chargement des propriétés
			_prop.load(input);

		} catch (Exception e) {
			pLogger.getLoggerEvenements(ConfigurationRsmpEquipment.class).log(Level.ERROR,
					"Probleme chargement du fichier de ressources", e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
					pLogger.getLoggerEvenements(ConfigurationRsmpEquipment.class).log(Level.ERROR,
							"Probleme fermeture input", e);
				}
			}
		}
	}

	/**
	 * Demande la valeur de la clé
	 * 
	 * @param pKey la clé à rechercher
	 * @return la valeur associée
	 */
	public String getString(String pKey) {
		return _prop.getProperty(pKey);
	}

	/**
	 * Demande la valeur de la clé
	 * 
	 * @param pKey          la clé à rechercher
	 * @param pValeurDefaut la valeur par défaut à utiliser si la clé n'existe pas
	 * @return la valeur associée
	 */
	public String getString(String pKey, String pValeurDefaut) {
		return _prop.getProperty(pKey, pValeurDefaut);
	}

}
