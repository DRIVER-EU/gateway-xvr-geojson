package eu.driver.gateway;

import org.slf4j.Logger;

import eu.driver.adapter.constants.TopicConstants;
import eu.driver.adapter.core.CISAdapter;
import eu.driver.adapter.core.producer.GenericProducer;
import eu.driver.adapter.logger.CISLogger;
import eu.driver.gateway.geojson.XVRItemToGeoJSONConverter;

public class GatewayConverter {
	
	private CISAdapter adapter;
	
	private static Logger logger = CISLogger.logger(GatewayConverter.class);
	
	public static void main(String[] args) {
		new GatewayConverter();
		logger.info("GatewayConverter started!");
	}
	
	public GatewayConverter() {
		adapter = CISAdapter.getInstance();
		GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC);
		GenericProducer producer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC));
		XVRItemToGeoJSONConverter xvrToGeoJsonConverter = new XVRItemToGeoJSONConverter(producer);
		adapter.addCallback(xvrToGeoJsonConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC));
	}
}
