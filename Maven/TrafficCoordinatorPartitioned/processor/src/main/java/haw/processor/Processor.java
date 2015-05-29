package haw.processor;

import haw.common.MapElement;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.openspaces.events.adapter.SpaceDataEvent;

import java.util.logging.Logger;

/**
 * The processor simulates work done no un-processed Data object. The
 * processData accepts a Data object, simulate work by sleeping, and then sets
 * the processed flag to true and returns the processed Data.
 */
public class Processor {

	Logger log = Logger.getLogger(this.getClass().getName());

	private long workDuration = 100;

	@GigaSpaceContext
	private GigaSpace gigaSpace;

	/**
	 * Sets the simulated work duration (in milliseconds). Default to 100.
	 */
	public void setWorkDuration(long workDuration) {
		this.workDuration = workDuration;
	}

	/**
	 * Process the given Data object and returning the processed Data.
	 *
	 * Can be invoked using OpenSpaces Events when a matching event occurs.
	 */

	@SpaceDataEvent
	public MapElement processData(MapElement current) {
		// sleep to simulate some work
		try {
			Thread.sleep(workDuration);
		} catch (InterruptedException e) {
			// do nothing
		}

		//Calculate Successor
		Integer successorX = 0;
		if (current.getX() < 10) {
			successorX = current.getX() + 1;
		}
		MapElement successorTemplate = new MapElement();
		successorTemplate.setX(successorX);
		
		//Get Successor
		MapElement successor = gigaSpace.read(successorTemplate);
		
		//Add carId to successor
		successor.setCurrentCarId(current.getCurrentCarId());
		
		//Write successor to space (with the setted carId)
		gigaSpace.write(successor);
		
		//Remove carId from current element
		current.removeCar();
		
		log.info(" >> OLD : " + current);
		log.info(" >> NEW : " + successor + "\n");
		return current;
	}

}