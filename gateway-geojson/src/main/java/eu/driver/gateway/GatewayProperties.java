package eu.driver.gateway;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties object that contains properties for a gateway converter.
 * 
 * @author hameetepa
 *
 */
public class GatewayProperties extends Properties {

	private static final long serialVersionUID = -5082046368427979491L;

	/**
	 * Input topic name for simulated items
	 */
	public static final String INPUT_TOPIC_ITEM = "input.topic.item";
	
	/**
	 * Input topic name for simulated units
	 */
	public static final String INPUT_TOPIC_UNIT = "input.topic.unit";
	/**
	 * Input topic name for simulated unit groups
	 */
	public static final String INPUT_TOPIC_UNITGROUP = "input.topic.unitgroup";
	/**
	 * Input topic name for simulated stations
	 */
	public static final String INPUT_TOPIC_STATION = "input.topic.station";
	
	/**
	 * Input topic name for simulated deletions
	 */
	public static final String INPUT_TOPIC_DELETIONS = "input.topic.deletions";
	
	/**
	 * Output topic name for converted items
	 */
	public static final String OUTPUT_TOPIC_ITEM = "output.topic.item";
	
	/**
	 * Output topic name for converted units
	 */
	public static final String OUTPUT_TOPIC_UNIT = "output.topic.unit";
	
	/**
	 * Output topic name for converted unit groups
	 */
	public static final String OUTPUT_TOPIC_UNITGROUP = "output.topic.unitgroup";
	
	/**
	 * Output topic name for converted stations
	 */
	public static final String OUTPUT_TOPIC_STATION = "output.topic.station";
	
	public static final String OUTPUT_FREQUENCY = "output.frequency";
	
	private static final Logger logger = LoggerFactory.getLogger(GatewayProperties.class);

	private static GatewayProperties instance;

	/**
	 * @return The Singleton ClientProperties object containing all client
	 *         configuration.
	 */
	public static GatewayProperties getInstance() {
		if (instance == null) {
			instance = new GatewayProperties();
		}
		return instance;
	}

	private GatewayProperties() {
		super();
		setDefaults();
		loadConfigFile();
	}
	
	private void loadConfigFile() {
		try {
			FileInputStream fis = new FileInputStream("config/gatewayconverter.properties");
			load(fis);
			fis.close();
		} catch (IOException e) {
			logger.error("Could not read Client Properties file client.properties in config folder");
		}
	}

	private void setDefaults() {
		setProperty(OUTPUT_FREQUENCY, "1000");
	}

}
