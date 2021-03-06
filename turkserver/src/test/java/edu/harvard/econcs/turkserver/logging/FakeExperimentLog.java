package edu.harvard.econcs.turkserver.logging;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.server.ExperimentScoped;

@ExperimentScoped
public class FakeExperimentLog implements LogController, ExperimentLog {	
	
	volatile int lastRound;
	
	@Override
	public void initialize(long startTime, String experimentId) {
		
	}

	@Override
	public long print(String msg) {		
		System.out.println(msg);
		return System.currentTimeMillis();
	}

	@Override
	public long printf(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
		return System.currentTimeMillis();
	}

	@Override
	public void startRound(int round) {
		System.out.println("ExpLog: Starting round " + round);	
		lastRound = round;
	}

	@Override
	public void finishRound() {
		System.out.println("ExpLog: Finishing round " + lastRound);
	}

	@Override
	public String getRoundOutput() {
		return "Fake round output; was printed to screen";
	}

	@Override
	public long conclude() {
		System.out.println("Log finished.");
		return System.currentTimeMillis();
	}

	@Override
	public String getOutput() {		
		return "Nothing, output was printed to screen.";
	}

}
