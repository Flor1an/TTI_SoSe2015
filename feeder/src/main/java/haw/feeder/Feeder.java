package haw.feeder;

import haw.common.CarPositions;
import haw.common.MapElement;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceInterruptedException;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A feeder bean starts a scheduled task that writes a new Data objects to the
 * space (in an unprocessed state).
 *
 * <p>
 * The space is injected into this bean using OpenSpaces support for @GigaSpaceContext
 * annotation.
 *
 * <p>
 * The scheduling uses the java.util.concurrent Scheduled Executor Service. It
 * is started and stopped based on Spring life cycle events.
 *
 * @author kimchy
 */
public class Feeder implements InitializingBean, DisposableBean {

	Logger log = Logger.getLogger(this.getClass().getName());

	private ScheduledExecutorService executorService;

	private ScheduledFuture<?> sf;

	private long defaultDelay = 10;

	private FeederTask feederTask;

	@GigaSpaceContext
	private GigaSpace gigaSpace;

	/**
	 * Sets the number of types that will be used to set
	 * {@link org.openspaces.example.data.common.Data#setType(Long)}.
	 *
	 * <p>
	 * The type is used as the routing index for partitioned space. This will
	 * affect the distribution of Data objects over a partitioned space.
	 */

	public void setDefaultDelay(long defaultDelay) {
		this.defaultDelay = defaultDelay;
	}

	public void afterPropertiesSet() throws Exception {
		log.info("--- STARTING FEEDER WITH CYCLE [" + defaultDelay + "]");
		executorService = Executors.newScheduledThreadPool(1);
		feederTask = new FeederTask();
		sf = executorService.scheduleAtFixedRate(feederTask, defaultDelay, defaultDelay, TimeUnit.MILLISECONDS);
	}

	public void destroy() throws Exception {
		sf.cancel(false);
		sf = null;
		executorService.shutdown();
	}

	public long getFeedCount() {
		return feederTask.getCounter();
	}

	public class FeederTask implements Runnable {

		private int counter = 0;
		public static final int HEIGHT = 10;
		public static final int WIDTH = 15;
		private int HorizontalRoadFrequency = 5;
		private int VerticalRoadFrequency = 6;
		public static final int AMOUNTOFCARS = 5;

		public void run() {
			try {

				generatingTheWorld();
				Thread.sleep(2000);
				setCarToRandomPosition();

				destroy(); // stop feeder
				log.fine("completed generating the world \n\n\n\n");

			} catch (SpaceInterruptedException e) {
				// ignore, we are being shutdown
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void generatingTheWorld() {
			log.info("ENTER: generatingTheWorld()");
			int rows = WIDTH;
			int columns = HEIGHT;

			int[][] matrix = new int[rows][columns];

			int id = 0;
			for (int x = 0; x < rows; x++) {
				for (int y = 0; y < columns; y++) {

					matrix[x][y] = id;

					MapElement mapElem;
					if (y % HorizontalRoadFrequency == 1) {
						if (x % VerticalRoadFrequency == 2) {
							// junction
							if (new Random().nextInt(2) == 0) {
								// junction direction south
								mapElem = new MapElement(id, x, y, true, true, true, false);
							} else {
								// junction direction east
								mapElem = new MapElement(id, x, y, true, true, false, true);
							}

						} else {
							// straight road direction east
							mapElem = new MapElement(id, x, y, true, false, true, false);
						}
					} else {
						if (x % VerticalRoadFrequency == 2) {
							// straight road direction south
							mapElem = new MapElement(id, x, y, true, false, false, true);
						} else {
							// no road (green land)
							mapElem = new MapElement(id, x, y, false, false, false, false);
						}

					}
					gigaSpace.write(mapElem);
					log.info(mapElem.toString());
					id++;
				}
			}
			log.info("EXIT: generatingTheWorld()\n\n\n");
		}

		private void setCarToRandomPosition() {
			log.info("ENTER: setCarToRandomPosition()");
			for (int carId = 0; carId < AMOUNTOFCARS; carId++) {

				MapElement roadMapElem = new MapElement();
				roadMapElem.setRoad(true);
				roadMapElem.setJunction(false);
				roadMapElem.setEastAllowed(true);

				int currentXPosition = -1;
				int currentYPosition = -1;

				boolean foundFreeMapElement = false;
				while (!foundFreeMapElement) {
					MapElement randomRoadMapElementFromSpace = gigaSpace.read(roadMapElem);
					log.warning("taking: " + randomRoadMapElementFromSpace);
					if (randomRoadMapElementFromSpace != null) {
						if (!randomRoadMapElementFromSpace.hasCar()) {
							randomRoadMapElementFromSpace = gigaSpace.take(randomRoadMapElementFromSpace);
							foundFreeMapElement = true;
							randomRoadMapElementFromSpace.setCurrentCarId(carId);
							currentXPosition = randomRoadMapElementFromSpace.getX();
							currentYPosition = randomRoadMapElementFromSpace.getY();
							gigaSpace.write(randomRoadMapElementFromSpace);
						}
					}
				}
				log.info("Assigning of Car " + carId + " to [" + currentXPosition + ";" + currentYPosition + "] completed.\n");
			}
				log.info("EXIT: setCarToRandomPosition()\n\n\n");
			
		}

		public long getCounter() {
			return counter;
		}
	}

}
