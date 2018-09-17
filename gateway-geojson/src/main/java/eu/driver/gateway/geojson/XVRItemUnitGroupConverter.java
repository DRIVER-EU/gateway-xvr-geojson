package eu.driver.gateway.geojson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import eu.driver.model.sim.ObjectDeleted;
import eu.driver.model.sim.connection.Unit;
import eu.driver.model.sim.connection.UnitConnection;
import eu.driver.model.sim.entity.Item;
import eu.driver.model.sim.entity.item.ObjectType;
import eu.driver.model.sim.entity.item.PersonType;
import eu.driver.model.sim.entity.item.RescueLabel;
import eu.driver.model.sim.entity.item.VehicleSubType;
import eu.driver.model.sim.entity.item.VehicleType;

public class XVRItemUnitGroupConverter implements IAdaptorCallback {

	private GenericProducer itemProducer;
	private GenericProducer unitProducer;
	private GenericProducer groupProducer;

	private Map<CharSequence, Item> items = new HashMap<>();
	private Map<CharSequence, Item> itemUpdates = new HashMap<>();

	private Map<CharSequence, Unit> units = new HashMap<>();
	private Map<CharSequence, Unit> unitUpdates = new HashMap<>();
	private Map<CharSequence, Unit> unitMainItemMap = new HashMap<>();
	private UnitConnectionGraph unitConnectionGraph = new UnitConnectionGraph();

	private Queue<UnitConnection> connectionUpdates = new LinkedList<>();
	private Map<CharSequence, Unit> groupUpdates = new HashMap<>();
	private Map<CharSequence, CharSequence> groupGuids = new HashMap<>();

	private ScheduledExecutorService reportingScheduler;

	private final Object groupUpdateLock = new Object();

	private static Logger logger = CISLogger.logger(XVRItemUnitGroupConverter.class);

	public XVRItemUnitGroupConverter(GenericProducer itemProducer, GenericProducer unitProducer,
			GenericProducer groupProducer) {
		this.itemProducer = itemProducer;
		this.unitProducer = unitProducer;
		this.groupProducer = groupProducer;

		reportingScheduler = Executors.newScheduledThreadPool(3);
		long freq = Long.parseLong(GatewayProperties.getInstance().getProperty(GatewayProperties.OUTPUT_FREQUENCY));

		reportingScheduler.scheduleAtFixedRate(new ItemReportingTask(), 0, freq, TimeUnit.MILLISECONDS);
		reportingScheduler.scheduleAtFixedRate(new UnitReportingTask(), 0, freq, TimeUnit.MILLISECONDS);
		reportingScheduler.scheduleAtFixedRate(new GroupReportingTask(), 0, freq, TimeUnit.MILLISECONDS);
		reportingScheduler.scheduleAtFixedRate(new UnitConnectionTask(), 0, freq, TimeUnit.MILLISECONDS);

		logger.info(
				"Start Converting XVR Items, Units and Groups to GeoJSON every " + freq + " milliseconds (windowed)");
	}

	public void messageReceived(IndexedRecord key, IndexedRecord message) {
		if (message instanceof Item) {
			processItem((Item) message);
		}
		if (message instanceof Unit) {
			processUnit((Unit) message);
		}
		if (message instanceof UnitConnection) {
			processUnitConnection((UnitConnection) message);
			logger.info("Received Unit Connection: " + message);
		}
		if (message instanceof ObjectDeleted) {
			processObjectDeletion((ObjectDeleted) message);
		}
	}

	private void processItem(Item item) {
		synchronized (items) {
			items.put(item.getGuid(), item);
		}

		synchronized (itemUpdates) {
			if (item.getVisibleForParticipant() && item.getScenarioLabel() instanceof RescueLabel) {
				itemUpdates.put(item.getGuid(), item);
			}
			if (itemUpdates.size() >= 100) {
				reportingScheduler.schedule(new ItemReportingTask(), 0, TimeUnit.MILLISECONDS);
			}
		}

		Unit unit = null;
		synchronized (unitMainItemMap) {
			unit = unitMainItemMap.get(item.getGuid());
		}
		if (unit != null) {
			synchronized (unitUpdates) {
				unitUpdates.put(unit.getGuid(), unit);
			}
			synchronized (groupUpdateLock) {
				synchronized (unitConnectionGraph) {
					UnitConnectionGraph.Unit ucUnit = unitConnectionGraph.getUnit(unit.getGuid());

					// TODO: move this to the reporting task?
					if (!ucUnit.hasParent() && ucUnit.hasChildren()) {
						synchronized (groupUpdates) {
							groupUpdates.put(unit.getGuid(), unit);
						}
					}
				}
			}
		}
	}

	private void processUnit(Unit unit) {
		synchronized (units) {
			units.put(unit.getGuid(), unit);
		}
		synchronized (unitMainItemMap) {
			unitMainItemMap.put(unit.getMainItem(), unit);
		}
		synchronized (unitConnectionGraph) {
			unitConnectionGraph.addUnit(unit.getGuid());
		}
		synchronized (unitUpdates) {
			unitUpdates.put(unit.getGuid(), unit);
		}
	}

	private void processUnitConnection(UnitConnection unitConnection) {
		synchronized (connectionUpdates) {
			connectionUpdates.add(unitConnection);
		}
	}

	private void processObjectDeletion(ObjectDeleted deletion) {
		// for now only attempt to delete connections
		synchronized (unitConnectionGraph) {
			if (unitConnectionGraph.hasConnection(deletion.getGuid())) {
				unitConnectionGraph.removeConnection(deletion.getGuid());
			}

			// TODO: check parent and child if valid group -> if so report
		}
		// entities that are removed should stop being reported automatically
	}

	private class UnitConnectionTask implements Runnable {
		@Override
		public void run() {
			synchronized (connectionUpdates) {
				List<UnitConnection> failedUpdates = new LinkedList<>();
				while (connectionUpdates.size() > 0) {
					UnitConnection connection = connectionUpdates.poll();
					// attempt to create connection
					synchronized (groupUpdateLock) {
						synchronized (unitConnectionGraph) {
							if (unitConnectionGraph.hasUnit(connection.getMainUnit())
									&& unitConnectionGraph.hasUnit(connection.getSubUnit())) {
								unitConnectionGraph.addConnection(connection.getGuid(), connection.getMainUnit(),
										connection.getSubUnit());

								UnitConnectionGraph.Unit ucUnit = unitConnectionGraph.getUnit(connection.getMainUnit());
								if (!ucUnit.hasParent() && ucUnit.hasChildren()) {
									synchronized (groupUpdates) {
										groupUpdates.put(connection.getMainUnit(), units.get(connection.getMainUnit()));
									}
								}
							} else {
								failedUpdates.add(connection); // if unable, add to failed updates
								logger.info("Unable to create connection: " + connection.getGuid()
										+ " because parent or child unit not known. Trying again later!");
							}
						}
					}
				}
				connectionUpdates.addAll(failedUpdates); // reschedule failed connection attempts
			}
		}
	}

	private class UnitReportingTask implements Runnable {
		@Override
		public void run() {
			List<Unit> failedUpdates = new LinkedList<>();
			synchronized (unitUpdates) {
				if (unitUpdates.size() > 0) {
					FeatureCollection.Builder builder = FeatureCollection.newBuilder();
					List<Feature> features = new ArrayList<>();

					for (Unit unit : unitUpdates.values()) {

						Feature.Builder featureBuilder = Feature.newBuilder();

						Item mainItem = null;
						synchronized (items) {
							mainItem = items.get(unit.getMainItem());
						}
						if (mainItem != null) {
							List<Double> lonLatAlt = new ArrayList<>(3);
							lonLatAlt.add(mainItem.getLocation().getLongitude());
							lonLatAlt.add(mainItem.getLocation().getLatitude());
							lonLatAlt.add(mainItem.getLocation().getAltitude());

							featureBuilder.setGeometry(new Point(PointType.Point, lonLatAlt));

							SimulatedEntityProperties.Builder entityProperties = SimulatedEntityProperties.newBuilder();
							entityProperties.setGuid(unit.getGuid());
							entityProperties.setName(unit.getName());
							entityProperties.setType(TypeEnum.UNIT);
							
							// set main item guid first, then the sub entities
							List<CharSequence> subEntities = new LinkedList<>();
							subEntities.add(mainItem.getGuid());
							subEntities.addAll(unit.getSubItems());
							entityProperties.setSubEntities(subEntities);
							
							Object scenLabel = mainItem.getScenarioLabel();
							if (!(scenLabel instanceof RescueLabel)) {
								entityProperties.setLabel("UNKNOWN");
								logger.warn("Main Item of the Unit with guid " + unit.getGuid()
										+ " is not of type Rescue!");
							} else {
								RescueLabel rescueLabel = (RescueLabel) scenLabel;
								entityProperties.setLabel(rescueLabel.getSubLabel().name());
							}

							featureBuilder.setProperties(entityProperties.build());

							featureBuilder.build();

							features.add(featureBuilder.build());
						} else {
							logger.info("Unable to report Unit: " + unit.getGuid()
									+ " because main item not known. Trying again later!");
							failedUpdates.add(unit);
						}
					}

					builder.setFeatures(features);
					FeatureCollection fc = builder.build();
					unitProducer.send(fc);
					logger.info("Reported " + features.size() + " XVR Units as GeoJSON Features");
					unitUpdates.clear();
					for (Unit u : failedUpdates) {
						unitUpdates.put(u.getGuid(), u);
					}
				}
			}
		}
	}

	private class ItemReportingTask implements Runnable {
		@Override
		public void run() {
			synchronized (itemUpdates) {
				if (itemUpdates.size() > 0) {
					FeatureCollection.Builder builder = FeatureCollection.newBuilder();
					List<Feature> features = new ArrayList<>();

					for (Item item : itemUpdates.values()) {

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
					itemProducer.send(fc);
					logger.info("Reported " + features.size() + " XVR Items as GeoJSON Features");
					itemUpdates.clear();
				}
			}
		}
	}

	private class GroupReportingTask implements Runnable {
		@Override
		public void run() {
			List<Unit> failedUpdates = new LinkedList<>();
			synchronized (groupUpdateLock) {
				synchronized (groupUpdates) {
					if (groupUpdates.size() > 0) {
						FeatureCollection.Builder builder = FeatureCollection.newBuilder();
						List<Feature> features = new ArrayList<>();

						for (Unit mainUnit : groupUpdates.values()) {

							Feature.Builder featureBuilder = Feature.newBuilder();

							Item mainItem = items.get(mainUnit.getMainItem());
							if (mainItem != null) {
								List<Double> lonLatAlt = new ArrayList<>(3);
								lonLatAlt.add(mainItem.getLocation().getLongitude());
								lonLatAlt.add(mainItem.getLocation().getLatitude());
								lonLatAlt.add(mainItem.getLocation().getAltitude());

								featureBuilder.setGeometry(new Point(PointType.Point, lonLatAlt));

								SimulatedEntityProperties.Builder entityProperties = SimulatedEntityProperties
										.newBuilder();

								CharSequence groupGuid = groupGuids.get(mainUnit.getGuid());
								if (groupGuid == null) {
									groupGuid = java.util.UUID.randomUUID().toString();
									groupGuids.put(mainUnit.getGuid(), groupGuid);
								}

								entityProperties.setGuid(groupGuid);
								entityProperties.setName(mainUnit.getName() + "-group");
								entityProperties.setType(TypeEnum.UNITGROUP);

								List<CharSequence> groupMembers = new LinkedList<>();
								groupMembers.add(mainUnit.getGuid());
								synchronized (unitConnectionGraph) {
									groupMembers.addAll(unitConnectionGraph.getDescendantsForUnit(mainUnit.getGuid()));
								}
								entityProperties.setSubEntities(groupMembers);

								Object scenLabel = mainItem.getScenarioLabel();
								if (!(scenLabel instanceof RescueLabel)) {
									entityProperties.setLabel("UNKNOWN");
									logger.warn("Main Item of the Group with guid " + groupGuid
											+ " is not of type Rescue!");
								} else {
									RescueLabel rescueLabel = (RescueLabel) scenLabel;
									entityProperties.setLabel(rescueLabel.getSubLabel().name());
								}

								featureBuilder.setProperties(entityProperties.build());

								featureBuilder.build();

								features.add(featureBuilder.build());
							} else {
								logger.info("Unable to report Group because main unit not known. Trying again later!");
								failedUpdates.add(mainUnit);
							}
						}

						builder.setFeatures(features);
						FeatureCollection fc = builder.build();
						groupProducer.send(fc);
						logger.info("Reported " + features.size() + " XVR Unit Groups as GeoJSON Features");
						groupUpdates.clear();
						for (Unit u : failedUpdates) {
							groupUpdates.put(u.getGuid(), u);
						}
					}
				}
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
			if (subType == VehicleSubType.BOAT) {
				return TypeEnum.BOAT;
			}
			if (subType == VehicleSubType.CAR) {
				return TypeEnum.CAR;
			}
			if (subType == VehicleSubType.HELICOPTER) {
				return TypeEnum.HELICOPTER;
			}
			if (subType == VehicleSubType.MOTORCYCLE) {
				return TypeEnum.MOTORCYCLE;
			}
			if (subType == VehicleSubType.PLANE) {
				return TypeEnum.PLANE;
			}
			if (subType == VehicleSubType.TRUCK) {
				return TypeEnum.TRUCK;
			}
			if (subType == VehicleSubType.VAN) {
				return TypeEnum.VAN;
			}
		}
		if (type instanceof PersonType) {
			return TypeEnum.PERSON;
		}
		return TypeEnum.UNKNOWN;
	}

}
