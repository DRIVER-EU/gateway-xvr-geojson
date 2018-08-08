package eu.driver.gateway.geojson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;

import eu.driver.adapter.core.producer.GenericProducer;
import eu.driver.adapter.logger.CISLogger;
import eu.driver.api.IAdaptorCallback;
import eu.driver.gateway.GatewayProperties;
import eu.driver.model.geojson.sim.Feature;
import eu.driver.model.geojson.sim.FeatureCollection;
import eu.driver.model.geojson.sim.Point;
import eu.driver.model.geojson.sim.PointType;
import eu.driver.model.geojson.sim.SimulatedEntityProperties;
import eu.driver.model.geojson.sim.TypeEnum;
import eu.driver.model.sim.entity.Item;
import eu.driver.model.sim.entity.item.EnvironmentLabel;
import eu.driver.model.sim.entity.item.IncidentLabel;
import eu.driver.model.sim.entity.item.ObjectType;
import eu.driver.model.sim.entity.item.PersonType;
import eu.driver.model.sim.entity.item.RescueLabel;
import eu.driver.model.sim.entity.item.VehicleSubType;
import eu.driver.model.sim.entity.item.VehicleType;

public class XVRItemConverter implements IAdaptorCallback {

	private GenericProducer outputProducer;

	private Map<CharSequence, Item> items;
	private ScheduledExecutorService reportingScheduler;

	private static Logger logger = CISLogger.logger(XVRItemConverter.class);

	public XVRItemConverter(GenericProducer producer) {
		outputProducer = producer;
		items = new HashMap<>();
		reportingScheduler = Executors.newScheduledThreadPool(1);
		long freq = Long.parseLong(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_FREQUENCY));
		reportingScheduler.scheduleAtFixedRate(new ReportingTask(), 0, freq, TimeUnit.MILLISECONDS);
		logger.info("Start Converting XVR Items to GeoJSON every " + freq + " milliseconds (windowed)");
	}

	public void messageReceived(IndexedRecord key, IndexedRecord message) {
		if (message instanceof Item) {
			synchronized (items) {
				Item item = (Item) message;
				if (item.getVisibleForParticipant() && item.getScenarioLabel() instanceof RescueLabel) {
					items.put(item.getGuid(), item);
				}
				if (items.size() >= 100) {
					reportingScheduler.schedule(new ReportingTask(), 0, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	private class ReportingTask implements Runnable {
		@Override
		public void run() {
			synchronized (items) {
				if (items.size() > 0) {
					FeatureCollection.Builder builder = FeatureCollection.newBuilder();
					List<Feature> features = new ArrayList<>();

					for (Item item : items.values()) {

						Feature.Builder featureBuilder = Feature.newBuilder();

						List<Double> lonLatAlt = new ArrayList<>(3);
						lonLatAlt.add(item.getLocation().getLongitude());
						lonLatAlt.add(item.getLocation().getLatitude());
						lonLatAlt.add(item.getLocation().getAltitude());

						featureBuilder.setGeometry(new Point(PointType.Point, lonLatAlt));

						SimulatedEntityProperties.Builder entityProperties = SimulatedEntityProperties.newBuilder();
						entityProperties.setGuid(item.getGuid());
						entityProperties.setName(item.getName());
						entityProperties.setType(getItemType(item));
						RescueLabel rescueLabel = (RescueLabel) item.getScenarioLabel();
						entityProperties.setLabel(rescueLabel.getSubLabel().name());
						entityProperties.setSpeed(item.getVelocity().getMagnitude());

						featureBuilder.setProperties(entityProperties.build());

						featureBuilder.build();

						features.add(featureBuilder.build());
					}

					builder.setFeatures(features);
					FeatureCollection fc = builder.build();
					outputProducer.send(fc);
					logger.info("Reported " + features.size() + " XVR Items as GeoJSON Features");
					items.clear();
				}
			}
		}

		private TypeEnum getItemType(Item item) {
			Object type = item.getItemType();
			if (type instanceof ObjectType) {
				return TypeEnum.OBJECT;
			}
			if (type instanceof VehicleType) {
				VehicleType ot = (VehicleType) type;
				VehicleSubType subType = ot.getSubType();
				if(subType == VehicleSubType.BOAT) {
					return TypeEnum.BOAT;
				}
				if(subType == VehicleSubType.CAR) {
					return TypeEnum.CAR;
				}
				if(subType == VehicleSubType.HELICOPTER) {
					return TypeEnum.HELICOPTER;
				}
				if(subType == VehicleSubType.MOTORCYCLE) {
					return TypeEnum.MOTORCYCLE;
				}
				if(subType == VehicleSubType.PLANE) {
					return TypeEnum.PLANE;
				}
				if(subType == VehicleSubType.TRUCK) {
					return TypeEnum.TRUCK;
				}
				if(subType == VehicleSubType.VAN) {
					return TypeEnum.VAN;
				}
			}
			if (type instanceof PersonType) {
				return TypeEnum.PERSON;
			}
			return TypeEnum.UNKNOWN;
		}
	}

}
