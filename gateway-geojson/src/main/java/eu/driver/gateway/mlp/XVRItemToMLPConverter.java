package eu.driver.gateway.mlp;

import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;

import eu.driver.adapter.core.producer.GenericProducer;
import eu.driver.adapter.logger.CISLogger;
import eu.driver.api.IAdaptorCallback;
import eu.driver.model.mlp.Alt;
import eu.driver.model.mlp.AttrEnc;
import eu.driver.model.mlp.AttrType;
import eu.driver.model.mlp.Coord;
import eu.driver.model.mlp.Msid;
import eu.driver.model.mlp.Pd;
import eu.driver.model.mlp.Point;
import eu.driver.model.mlp.Pos;
import eu.driver.model.mlp.SlRep;
import eu.driver.model.mlp.Time;
import eu.driver.model.sim.entity.Item;
import eu.driver.position.PositionParser;

public class XVRItemToMLPConverter implements IAdaptorCallback {
	
	private GenericProducer outputProducer;
	private Logger logger = CISLogger.logger(XVRItemToMLPConverter.class);
	
	public XVRItemToMLPConverter(GenericProducer producer) {
		outputProducer = producer;
	}

	private void receiveMessage(Item message) {
		SlRep locationReport = itemToStandardLocationReport(message);
		outputProducer.send(locationReport);
	}
	
	private SlRep itemToStandardLocationReport(Item item) {
		SlRep.Builder builder = SlRep.newBuilder();
		Pos.Builder posBuilder = Pos.newBuilder();
		
		posBuilder.setMsid(new Msid(item.getGuid(), AttrType.OPE_ID, AttrEnc.ASC)); // ASCI encoded operator specific entity
		
		Pd.Builder pdBuilder = Pd.newBuilder();
		
		int altitude = (int) Math.round(item.getLocation().getAltitude());
		double latRads = Math.toRadians(item.getLocation().getLatitude());
		double lonRads = Math.toRadians(item.getLocation().getLongitude());
		String latDMS = PositionParser.convertLatRadToDMS(latRads);
		String lonDMS = PositionParser.convertLonRadToDMS(lonRads);
		pdBuilder.setAlt(new Alt(altitude));
		
		Point p = new Point();
		Coord c = new Coord();
		c.setX(latDMS);
		c.setY(lonDMS);
		p.setCoord(c);
		pdBuilder.setShape(p);
		
		int yaw = (int) Math.round(item.getVelocity().getYaw());
		pdBuilder.setDirection(yaw);
		
		int speed = (int) Math.round(item.getVelocity().getMagnitude());
		pdBuilder.setSpeed(speed);
		
		pdBuilder.setTime(new Time(System.currentTimeMillis(), "0000"));
		
		posBuilder.setPd(pdBuilder.build());
		
		builder.setPos(posBuilder.build());
		return builder.build();
	}

	@Override
	public void messageReceived(IndexedRecord key, IndexedRecord message) {
		if(message instanceof Item) {
			receiveMessage((Item) message);
		} else {
			logger.warn("XVR Item to MLP Converter received a message of which the value was not an XVR Item, but: " + message.getClass());
		}
	}

}
