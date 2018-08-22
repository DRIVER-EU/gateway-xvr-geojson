package eu.driver.gateway;

import org.slf4j.Logger;

import eu.driver.adapter.core.CISAdapter;
import eu.driver.adapter.core.producer.GenericProducer;
import eu.driver.adapter.logger.CISLogger;
import eu.driver.gateway.geojson.XVRItemUnitGroupConverter;
import eu.driver.gateway.geojson.XVRStationConverter;

public class GatewayConverter {
	
	private CISAdapter adapter;
	
	private static Logger logger = CISLogger.logger(GatewayConverter.class);
	
	public GatewayConverter() {
		adapter = CISAdapter.getInstance();
		
		addItemUnitGroupConverter();
		addStationConverter();
	}
	
	public static void main(String[] args) {
		new GatewayConverter();
		logger.info("GatewayConverter started!");
	}
	
	
	private void addItemUnitGroupConverter() {
		GenericProducer itemProducer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC_ITEM));
		GenericProducer unitProducer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC_UNIT));
		GenericProducer groupProducer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC_UNITGROUP));
		
		XVRItemUnitGroupConverter itemConverter = new XVRItemUnitGroupConverter(itemProducer, unitProducer, groupProducer);
		
		adapter.addCallback(itemConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_ITEM));
		adapter.addCallback(itemConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_UNIT));
		adapter.addCallback(itemConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_UNITGROUP));
		adapter.addCallback(itemConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_DELETIONS));
	}
	
	private void addStationConverter() {
		GenericProducer stationProducer = adapter.createProducer(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_TOPIC_STATION));
		XVRStationConverter stationConverter = new XVRStationConverter(stationProducer);
		adapter.addCallback(stationConverter, GatewayProperties.getInstance().getProperty(GatewayProperties.INPUT_TOPIC_STATION));
	}
}
