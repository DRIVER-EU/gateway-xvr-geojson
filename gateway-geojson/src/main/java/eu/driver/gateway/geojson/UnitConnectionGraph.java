package eu.driver.gateway.geojson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import eu.driver.adapter.logger.CISLogger;

public class UnitConnectionGraph {

	private static Logger logger = CISLogger.logger(UnitConnectionGraph.class);

	private Map<CharSequence, Unit> unitMap;
	private Map<CharSequence, UnitConnection> connectionMap;

	public UnitConnectionGraph() {
		unitMap = new HashMap<>();
		connectionMap = new HashMap<>();
	}

	public void addUnit(CharSequence guid) {
		if (unitMap.get(guid) == null) {
			Unit u = new Unit();
			unitMap.put(guid, u);
		}
	}

	public Unit getUnit(CharSequence guid) {
		return unitMap.get(guid);
	}
	
	public List<CharSequence> getDescendantsForUnit(CharSequence guid) {
		Unit u = unitMap.get(guid);
		List<CharSequence> descendants = new LinkedList<>();
		descendants.addAll(u.children);
		for(CharSequence child : u.children) {
			descendants.addAll(getDescendantsForUnit(child));
		}
		return descendants;
	}
	
	public boolean hasUnit(CharSequence guid) {
		return unitMap.get(guid) != null;
	}

	public void removeUnit(CharSequence guid) {
		unitMap.remove(guid);
	}

	public void addConnection(CharSequence connectionGuid, CharSequence parentGuid, CharSequence childGuid) {
		UnitConnection connection = new UnitConnection(connectionGuid, parentGuid, childGuid);
		connectionMap.put(connectionGuid, connection);
		Unit parent = unitMap.get(parentGuid);
		Unit child = unitMap.get(childGuid);
		parent.children.add(childGuid);
		child.parent = parentGuid;
	}
	
	public boolean hasConnection(CharSequence connectionGuid) {
		return connectionMap.get(connectionGuid) != null;
	}

	public void removeConnection(CharSequence connectionGuid) {
		UnitConnection connection = connectionMap.get(connectionGuid);
		if (connection == null) {
			logger.error(
					"Cant remove connection with guid: " + connectionGuid + " because that connection is not known!");
			return;
		}
		Unit parent = unitMap.get(connection.parent);
		Unit child = unitMap.get(connection.child);
		if (parent == null) {
			logger.error("Cant remove connection with guid: " + connectionGuid + " because Parent Unit with guid: "
					+ connection.parent + " is not known!");
			return;
		}
		if (child == null) {
			logger.error("Cant remove connection with guid: " + connectionGuid + " because Child Unit with guid: "
					+ connection.child + " is not known!");
			return;
		}
		child.parent = null;
		parent.children.remove(connection.child);
	}

	public class Unit {

		private CharSequence parent;
		private List<CharSequence> children = new ArrayList<>();

		public Unit() {
		}

		public boolean hasParent() {
			return parent != null;
		}

		public boolean hasChildren() {
			return !children.isEmpty();
		}
	}

	public class UnitConnection {

		private CharSequence parent;
		private CharSequence child;

		public UnitConnection(CharSequence guid, CharSequence parent, CharSequence child) {
			this.parent = parent;
			this.child = child;
		}
	}

}
