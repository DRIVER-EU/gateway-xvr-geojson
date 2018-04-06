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
	 * Input topic name
	 */
	public static final String INPUT_TOPIC = "input.topic";

	/**
	 * Output topic name
	 */
	public static final String OUTPUT_TOPIC = "output.topic";
	
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
		setProperty(INPUT_TOPIC, "");
		setProperty(OUTPUT_TOPIC, "");
		setProperty(OUTPUT_FREQUENCY, "1000");
	}

}
