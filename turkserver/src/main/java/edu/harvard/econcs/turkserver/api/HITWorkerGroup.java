package edu.harvard.econcs.turkserver.api;

import java.util.Collection;
import java.util.List;

public interface HITWorkerGroup {
	
	int groupSize();
		
	boolean contains(HITWorker hitWorker);

	Collection<? extends HITWorker> getHITWorkers();
	
	/**
	 * Convenience method to get the HIT IDs for this group of workers.
	 * @return
	 */
	Collection<String> getHITIds();

	/**
	 * Convenience method to get the Worker IDs for this group of workers.
	 * @return
	 */
	List<String> getWorkerIds();	
	
	/**
	 * Finds a HITWorker in this group with the corresponding HITId, or null otherwise
	 * @param hitId
	 * @return
	 */
	HITWorker findByHITId(String hitId);	
	
}
