package edu.harvard.econcs.turkserver.server;

import static com.google.common.base.Preconditions.*;

import org.apache.commons.configuration.Configuration;

import com.amazonaws.mturk.requester.QualificationRequirement;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.TSBaseModule.*;

/**
 * The main TurkServer class.
 * 
 * Contains static methods for starting experiments.
 * 
 * @author mao
 *
 */
public class TurkServer {
	
	/*
	 * Last check of sanity before we launch a server
	 */
	private static void checkConfiguration(Injector injector, Configuration conf) {
		
		checkNotNull(injector.getBinding(Key.get(String.class, Names.named(TSConfig.EXP_SETID))),
				"set not specified");
		checkNotNull(injector.getBinding(Key.get(Configurator.class, Names.named(TSConfig.EXP_CONFIGURATOR))),
				"experiment configurator not specified");
		checkNotNull(injector.getBinding(Key.get(int.class, Names.named(TSConfig.EXP_REPEAT_LIMIT))),
				"set limit not specified");
		
		boolean debugMode = conf.getBoolean(TSConfig.SERVER_DEBUGMODE);
		
		if( !debugMode ) { // Ignore these settings for local test
			checkNotNull(conf.getDouble(TSConfig.MTURK_HIT_BASE_REWARD, null),
					"reward not specified");
			
			checkNotNull(injector.getBinding(Key.get(QualificationRequirement[].class)),
					"No qualifications set!");
			
			checkNotNull(conf.getInteger(TSConfig.MTURK_HIT_FRAME_HEIGHT, null),
					"frame height not set");
			checkNotNull(conf.getString(TSConfig.MTURK_HIT_EXTERNAL_URL, null),
					"external url not set");
	
			// TODO update these when more flexible config is created 
			checkNotNull( conf.getInteger(TSConfig.HITS_INITIAL, null), 
					"initial HITs not specified ");
			checkNotNull( conf.getInteger(TSConfig.HITS_DELAY, null), 
					"delay not specified" );
			checkNotNull( conf.getInteger(TSConfig.SERVER_HITGOAL, null), 
					"goal amount not specified");
			checkNotNull( conf.getInteger(TSConfig.HITS_TOTAL, null),
					"total HITs not specified");
		}
		
	}

	public static void testExperiment(TSTestModule testModule) {		
		
		Injector injector = Guice.createInjector(testModule);		
		Configuration conf = testModule.getConfiguration();				
		checkConfiguration(injector, conf);
		
		HITController thm = injector.getInstance(HITController.class);
				
		SessionServer ss = getSessionServer(injector);
		
		// TODO this may not be in conf, but in injector
		thm.setHITType(
				conf.getString(TSConfig.MTURK_HIT_TITLE),
				conf.getString(TSConfig.MTURK_HIT_DESCRIPTION),
				conf.getString(TSConfig.MTURK_HIT_KEYWORDS),
				1.00, 
				conf.getInt(TSConfig.MTURK_ASSIGNMENT_DURATION),
				conf.getInt(TSConfig.MTURK_AUTO_APPROVAL_DELAY),
				null);
		
		thm.setExternalParams("http://localhost:9294/", 1500, 604800);
		
		new Thread(ss).start();		
		
		thm.postBatchHITs(1, 5000, 10);
	}

	public static void runExperiment(TSBaseModule module) {
		
		
		
	}

	static SessionServer getSessionServer(Injector injector) {
		if( injector.getExistingBinding(new Key<SimpleExperimentServer>() {}) != null ) {
			return injector.getInstance(SimpleExperimentServer.class);
		}
		else if( injector.getExistingBinding(new Key<GroupServer>() {}) != null ) {
			return injector.getInstance(GroupServer.class);
		}
		else {
			throw new RuntimeException("No binding found for session server. " +
					"Try bindSingleExperiments() or bindGroupExperiments() in your module.");
		}		
	}
	
}
