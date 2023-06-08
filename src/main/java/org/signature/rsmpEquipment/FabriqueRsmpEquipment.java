package org.signature.rsmpEquipment;

import org.signature.rsmpEquipment.communication.GestionnaireCommunication;
import org.signature.rsmpEquipment.communication.rsmp.TraitementProtocoleRsmp;
import org.signature.rsmpEquipment.log.LoggerRsmpEquipment;
import org.signature.rsmpEquipment.resources.ConfigurationRsmpEquipment;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;

/**
 * Une fabrique d'objets pour injecter
 * 
 * @author SDARIZCUREN
 *
 */
public enum FabriqueRsmpEquipment {

	INSTANCE;

	private Injector _injector = Guice.createInjector(new AbstractModule() {
		@Override
		protected void configure() {
			// Mode Singleton. @Singleton sur les méthodes provides ne marche pas (?)
			bind(LoggerRsmpEquipment.class).in(Scopes.SINGLETON);
			bind(ConfigurationRsmpEquipment.class).in(Scopes.SINGLETON);
			bind(GestionnaireCommunication.class).in(Scopes.SINGLETON);
			
		}
	});

	/**
	 * Construction d'un objet LoggerRsmpEquipment
	 * 
	 * @return l'objet construit avec l'injection de dépendances
	 */
	@Provides
	public LoggerRsmpEquipment getLoggerRsmpEquipment() {
		return _injector.getInstance(LoggerRsmpEquipment.class);
	}
	
	/**
	 * Construction d'un objet ConfigurationRsmpEquipment
	 * 
	 * @return l'objet construit avec l'injection de dépendances
	 */
	@Provides
	public ConfigurationRsmpEquipment getConfigurationRsmpEquipment() {
		return _injector.getInstance(ConfigurationRsmpEquipment.class);
	}
	
	/**
	 * Construction d'un objet GestionnaireCommunication
	 * 
	 * @return l'objet construit avec l'injection de dépendances
	 */
	@Provides
	public GestionnaireCommunication getGestionnaireCommunication() {
		return _injector.getInstance(GestionnaireCommunication.class);
	}
	
	/**
	 * Construction d'un objet TraitementProtocoleRsmp
	 * 
	 * @return l'objet construit avec l'injection de dépendances
	 */
	@Provides
	public TraitementProtocoleRsmp getTraitementProtocoleRsmp() {
		return _injector.getInstance(TraitementProtocoleRsmp.class);
	}
}
