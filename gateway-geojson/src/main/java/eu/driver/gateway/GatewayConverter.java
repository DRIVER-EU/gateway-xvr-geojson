package eu.driver.gateway;

import org.slf4j.Logger;

import eu.driver.adapter.constants.TopicConstants;
import eu.driver.adapter.core.CISAdapter;
import eu.driver.adapter.core.producer.GenericProducer;
import eu.driver.adapter.logger.CISLogger;
import eu.driver.gateway.geojson.XVRItemConverter;
import eu.driver.gateway.geojson.XVRStationConverter;

public class GatewayConverter {
	
	private CISAdapter adapter;
	
	private static Logger logger = CISLogger.logger(GatewayConverter.class);
	
	public static void main(String[] args) {
		new GatewayConverter();
		logger.info("GatewayConverter started!");
	}
	
	public GatewayConverter() {
		adapter = CISAdapter.getInstance();
		
		addItemConverter();
		addStationConverter();
	}
	
	private void addItemConverter() {
		GenericProducer itemProducer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC_ITEM));
		XVRItemConverter itemConverter = new XVRItemConverter(itemProducer);
		adapter.addCallback(itemConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_ITEM));
	}
	
	private void addStationConverter() {
		GenericProducer stationProducer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC_STATION));
		XVRStationConverter stationConverter = new XVRStationConverter(stationProducer);
		adapter.addCallback(stationConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_STATION));
	}
}
