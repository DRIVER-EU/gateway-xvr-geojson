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
import eu.driver.model.sim.entity.Station;

public class XVRStationConverter implements IAdaptorCallback {

	private GenericProducer outputProducer;

	private Map<CharSequence, Station> stations;
	private ScheduledExecutorService reportingScheduler;

	private static Logger logger = CISLogger.logger(XVRStationConverter.class);

	public XVRStationConverter(GenericProducer producer) {
		outputProducer = producer;
		stations = new HashMap<>();
		reportingScheduler = Executors.newScheduledThreadPool(1);
		long freq = Long.parseLong(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_FREQUENCY));
		reportingScheduler.scheduleAtFixedRate(new ReportingTask(), 0, freq, TimeUnit.MILLISECONDS);
		logger.info("Start Converting Simulated Stations to GeoJSON every " + freq + " milliseconds (windowed)");
	}

	public void messageReceived(IndexedRecord key, IndexedRecord message) {
		if (message instanceof Station) {
			synchronized (stations) {
				Station station = (Station) message;
				if(station.getVisibleForParticipant()) {
					stations.put(station.getGuid(), station);
				}
				if (stations.size() >= 100) {
					reportingScheduler.schedule(new ReportingTask(), 0, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	private class ReportingTask implements Runnable {
		@Override
		public void run() {
			synchronized (stations) {
				if (stations.size() > 0) {
					FeatureCollection.Builder builder = FeatureCollection.newBuilder();
					List<Feature> features = new ArrayList<>();

					for (Station station : stations.values()) {

						Feature.Builder featureBuilder = Feature.newBuilder();

						List<Double> lonLatAlt = new ArrayList<>(3);
						lonLatAlt.add(station.getLocation().getLongitude());
						lonLatAlt.add(station.getLocation().getLatitude());
						lonLatAlt.add(station.getLocation().getAltitude());

						featureBuilder.setGeometry(new Point(PointType.Point, lonLatAlt));

						SimulatedEntityProperties.Builder propertiesBuilder = SimulatedEntityProperties.newBuilder();
						propertiesBuilder.setGuid(station.getGuid());
						propertiesBuilder.setName(station.getName());
						propertiesBuilder.setLabel(station.getScenarioLabel().toString());
						propertiesBuilder.setType(TypeEnum.STATION);
						propertiesBuilder.setSubEntities(station.getItems());

						featureBuilder.setProperties(propertiesBuilder.build());

						featureBuilder.build();

						features.add(featureBuilder.build());
					}

					builder.setFeatures(features);
					FeatureCollection fc = builder.build();
					outputProducer.send(fc);
					logger.info("Reported " + features.size() + " XVR Stations as GeoJSON Features");
					stations.clear();
				}
			}
		}
	}

}
