package eu.driver.testing;

import org.apache.avro.generic.IndexedRecord;

import eu.driver.adapter.core.CISAdapter;
import eu.driver.adapter.core.producer.GenericProducer;
import eu.driver.api.IAdaptorCallback;
import eu.driver.model.sim.entity.Item;
import eu.driver.model.sim.geo.Location;
import eu.driver.model.sim.geo.Orientation;
import eu.driver.model.sim.geo.Velocity;

public class TestGateway {
	
	public static void main(String[] args) {
		CISAdapter adapter = CISAdapter.getInstance();
		adapter.addCallback(new IAdaptorCallback() {
			
			@Override
			public void messageReceived(IndexedRecord arg0, IndexedRecord arg1) {
				System.out.println(arg1);
			}
		}, "simulation-entity-item-pieter-test");
		
		GenericProducer producer = adapter.createProducer("simulation-entity-item-pieter-test");
		
		Item.Builder xvrEntityBuilder = Item.newBuilder();
		
		xvrEntityBuilder.setGuid("sim_entity");
		xvrEntityBuilder.setOwner("XVR.RM");
		xvrEntityBuilder.setName("Ambulance");
		xvrEntityBuilder.setOrientation(new Orientation(0d, 0d, 0d));
		xvrEntityBuilder.setVelocity(new Velocity(0d, 0d, 1d));
		xvrEntityBuilder.setVisibleForParticipant(true);
		xvrEntityBuilder.setMovable(true);
		Location.Builder xvrEntityLocationBuilder = Location.newBuilder();
		xvrEntityLocationBuilder.setAltitude(0.0);
		
		double latitude = 52.171f;
		double longitude = 21.201f;
		
		while (true) {
			
			xvrEntityLocationBuilder.setLatitude(latitude);
			xvrEntityLocationBuilder.setLongitude(longitude);
			
			xvrEntityBuilder.setLocationBuilder(xvrEntityLocationBuilder);
			
			Item xvrentity = xvrEntityBuilder.build();
			
			producer.send(xvrentity);
			
			latitude -= 0.001f;
			longitude += 0.001f;
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

}
