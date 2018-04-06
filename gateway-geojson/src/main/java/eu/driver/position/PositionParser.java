package eu.driver.position;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Parser for reading positions.
 */
public class PositionParser {

	public static String convertLonRadToDMS(double longitude_rad) {
		double londeg = Math.toDegrees(longitude_rad);

		char eastOrWest = PositionParser.SYMBOL_EAST;
		if (londeg < 0) {
			eastOrWest = PositionParser.SYMBOL_WEST;
			londeg = -londeg;
		}

		// Calculate degrees, minutes and seconds
		int degrees = (int) Math.floor(londeg);
		int minutes = (int) Math.floor((londeg - degrees) / (1 / 60.0));
		double seconds = ((londeg - degrees - (minutes * (1 / 60.0))) / (1 / 3600.0));
		DecimalFormat df = new DecimalFormat("##.###");
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
		df.setMinimumIntegerDigits(2);
		df.setMaximumIntegerDigits(2);

		DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(symbols);

		if (df.format(seconds).equals(df.format(60.00))) {
			seconds = 0.0;
			minutes = minutes + 1;
		}
		if (minutes == 60) {
			minutes = 0;
			degrees = degrees + 1;
		}

		// Convert values to strings
		String degrees2 = String.format("%02d", degrees);
		String minutes2 = String.format("%02d", minutes);
		String seconds2 = df.format(seconds);

		return degrees2 + ' ' + minutes2 + ' ' + seconds2
				+ eastOrWest;
	}

	public static String convertLatRadToDMS(double latitude_rad) {
		double latdeg = Math.toDegrees(latitude_rad);

		char northOrSouth = PositionParser.SYMBOL_NORTH;
		if (latdeg < 0) {
			northOrSouth = PositionParser.SYMBOL_SOUTH;
			latdeg = -latdeg;
		}

		// Calculate degrees, minutes and seconds
		int degrees = (int) Math.floor(latdeg);
		int minutes = (int) Math.floor((latdeg - degrees) / (1 / 60.0));
		double seconds = ((latdeg - degrees - (minutes * (1 / 60.0))) / (1 / 3600.0));
		DecimalFormat df = new DecimalFormat("##.###");
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
		df.setMinimumIntegerDigits(2);
		df.setMaximumIntegerDigits(2);

		DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(symbols);

		if (df.format(seconds).equals(df.format(60.00))) {
			seconds = 0.0;
			minutes = minutes + 1;
		}
		if (minutes == 60) {
			minutes = 0;
			degrees = degrees + 1;
		}

		// Convert values to strings
		String degrees2 = String.format("%02d", degrees);
		String minutes2 = String.format("%02d", minutes);
		String seconds2 = df.format(seconds);

		return degrees2 + ' ' + minutes2 + ' ' + seconds2
				+ northOrSouth;
	}

	/** DMS South notation */
	public static final char SYMBOL_SOUTH = 'S';

	/** DMS North notation */
	public static final char SYMBOL_NORTH = 'N';

	/** DMS West notation */
	public static final char SYMBOL_WEST = 'W';

	/** DMS East notation */
	public static final char SYMBOL_EAST = 'E';

	/** DMS degrees notation */
	public static final char SYMBOL_DEGREES = '°';

	/**
	 * alternative degrees notation (Non unicode)
	 */
	private static final char SYMBOL_DEGREES_ALT = (char) 65533;
	/** DMS minutes notation */
	public static final char SYMBOL_MINUTES = '\'';

	/** DMS seconds notation */
	public static final char SYMBOL_SECONDS = '\"';

	/** DMS separator */
	public static final char SYMBOL_COLON = ':';

	private PositionParser() {
		super();
	}

	private static class StringWrapper {

		private String mString;

		public StringWrapper(String pContents) {
			mString = pContents;
		}

		public int length() {
			return mString.length();
		}

		@Override
		public String toString() {
			return mString;
		}

		public boolean endsWith(char pCharacter) {
			if (mString.length() == 0) {
				return false;
			}
			return getLastChar() == pCharacter;
		}

		public StringWrapper substring(int pBeginIndex, int pEndIndex) {
			return new StringWrapper(mString.substring(pBeginIndex, pEndIndex));
		}

		public StringWrapper substring(int pBeginIndex) {
			return new StringWrapper(mString.substring(pBeginIndex));
		}

		public char getLastChar() {
			if (mString.length() == 0) {
				return (char) 0;
			}
			return mString.charAt(mString.length() - 1);
		}

		public boolean startsWith(char pCharacter) {
			if (mString.length() == 0) {
				return false;
			}
			return (mString.charAt(0) == pCharacter);
		}

		public void moveFirstCharacterToEnd() {
			if (mString.length() > 0) {
				char first = mString.charAt(0);
				mString = mString.substring(1) + first;
			}
		}

		public int indexOf(char pCharacter) {
			int result = mString.indexOf(pCharacter);
			if (pCharacter == SYMBOL_DEGREES) {
				if (result < 0) {
					return mString.indexOf(SYMBOL_DEGREES_ALT);
				}
			}
			return result;
		}

		public StringWrapper replace(String pString, String pString2) {
			return new StringWrapper(mString.replace(pString, pString2));
		}

	}

	/**
	 * Valid latitude values are: Northern Hemisphere: Latitude_DMS: "51°41'20.00"
	 * Latitude_DMS: "51°41'20.00N" Latitude_DMS: "+51°41'20.00" Southern
	 * Hemisphere: Latitude_DMS: "51°41'20.00S" Latitude_DMS: "-51°41'20.00"
	 * 
	 * @param pLatitude_DMS
	 *            string representation
	 * @return The latitude in degrees
	 */
	public static double parseLatDMS(String pLatitude_DMS) {
		StringWrapper latitudeDMS = new StringWrapper(pLatitude_DMS.replace(" ", ""));
		if (latitudeDMS.startsWith(SYMBOL_NORTH) || latitudeDMS.startsWith(SYMBOL_SOUTH)) {
			latitudeDMS.moveFirstCharacterToEnd();
		}
		boolean north = true;
		if (latitudeDMS.startsWith('+')) {
			latitudeDMS = latitudeDMS.substring(1);
		} else if (latitudeDMS.startsWith('-')) {
			latitudeDMS = latitudeDMS.substring(1);
			north = false;
		}
		int degreesIndex = latitudeDMS.indexOf(SYMBOL_DEGREES);
		if (degreesIndex == -1) {
			throw new RuntimeException("The latitude " + pLatitude_DMS + " should be of the format DD" + SYMBOL_DEGREES
					+ "MM" + SYMBOL_MINUTES + "SS.SS");
		}
		double deg = Double.parseDouble(latitudeDMS.substring(0, degreesIndex).toString());

		int minutesIndex = latitudeDMS.indexOf(SYMBOL_MINUTES);
		if (minutesIndex == -1) {
			throw new RuntimeException("The latitude " + pLatitude_DMS + " should be of the format DD" + SYMBOL_DEGREES
					+ "MM" + SYMBOL_MINUTES + "SS.SS");
		}
		StringWrapper minStr = latitudeDMS.substring(degreesIndex + 1, minutesIndex);
		minStr = minStr.replace(",", ".");
		double min = Double.parseDouble(minStr.toString());

		double sec = 0.0;
		char lastChar = latitudeDMS.getLastChar();
		if (lastChar == SYMBOL_SOUTH || lastChar == SYMBOL_NORTH) {
			if (minutesIndex + 1 < latitudeDMS.length() - 1) {
				StringWrapper secStr = latitudeDMS.substring(minutesIndex + 1, latitudeDMS.length() - 1);
				if (secStr.endsWith(SYMBOL_SECONDS)) {
					secStr = secStr.substring(0, secStr.length() - 1);
				}

				sec = Double.parseDouble(secStr.toString());
			}
			if (lastChar == SYMBOL_SOUTH) {
				if (!north) {
					throw new RuntimeException(
							"The latitude " + pLatitude_DMS + " cannot have both a - sign and " + SYMBOL_SOUTH);
				}
				north = false;
			}
		} else {
			if (minutesIndex + 1 < latitudeDMS.length()) {
				sec = Double.parseDouble(latitudeDMS.toString().substring(minutesIndex + 1, latitudeDMS.length()));
			}
		}
		if (north) {
			return (deg + min / 60.0 + sec / 3600.0);
		}
		return (-deg - min / 60.0 - sec / 3600.0);
	}

	/**
	 * Convert the DMS
	 * 
	 * @param pLongitude_DMS
	 *            New longitude in DMS format Valid longitude formats are Eastern of
	 *            Greenwich Longitude_DMS : "003°39'43.00" Longitude_DMS :
	 *            "003°39'43.00E" Longitude_DMS : "+003°39'43.00" Western of
	 *            Greenwich Longitude_DMS : "003°39'43.00W" Longitude_DMS :
	 *            "-003°39'43.00"
	 * @return The longitude in degrees
	 */
	public static double parseLonDMS(String pLongitude_DMS) {
		StringWrapper longitudeDMS = new StringWrapper(pLongitude_DMS.replace(" ", ""));
		if (longitudeDMS.startsWith(SYMBOL_EAST) || longitudeDMS.startsWith(SYMBOL_WEST)) {
			longitudeDMS.moveFirstCharacterToEnd();
		}
		boolean east = true;
		if (longitudeDMS.endsWith('+')) {
			longitudeDMS = longitudeDMS.substring(1);
		} else if (longitudeDMS.startsWith('-')) {
			longitudeDMS = longitudeDMS.substring(1);
			east = false;
		}
		int degreesIndex = longitudeDMS.indexOf(SYMBOL_DEGREES);
		if (degreesIndex == -1) {
			throw new RuntimeException("The longitude " + pLongitude_DMS + " should be of the format DD"
					+ SYMBOL_DEGREES + "MM" + SYMBOL_MINUTES + "SS.SS");
		}
		double deg = Double.parseDouble(longitudeDMS.toString().substring(0, degreesIndex));

		int minutesIndex = longitudeDMS.indexOf(SYMBOL_MINUTES);
		if (minutesIndex == -1) {
			throw new RuntimeException("The longitude " + pLongitude_DMS + " should be of the format DD"
					+ SYMBOL_DEGREES + "MM" + SYMBOL_MINUTES + "SS.SS");
		}
		StringWrapper minStr = longitudeDMS.substring(degreesIndex + 1, minutesIndex);
		minStr = minStr.replace(",", ".");
		double min = Double.parseDouble(minStr.toString());

		double sec = 0.0;
		char lastChar = longitudeDMS.getLastChar();
		if (lastChar == SYMBOL_WEST || lastChar == SYMBOL_EAST) {
			if (minutesIndex + 1 < longitudeDMS.length() - 1) {
				StringWrapper secStr = longitudeDMS.substring(minutesIndex + 1, longitudeDMS.length() - 1);
				if (secStr.endsWith(SYMBOL_SECONDS)) {
					secStr = secStr.substring(0, secStr.length() - 1);
				}

				sec = Double.parseDouble(secStr.toString());
			}
			if (lastChar == SYMBOL_WEST) {
				if (!east) {
					throw new RuntimeException(
							"The longitude " + pLongitude_DMS + " cannot have both a - sign and " + SYMBOL_WEST);
				}
				east = false;
			}
		} else {
			if (minutesIndex + 1 < longitudeDMS.length()) {
				sec = Double.parseDouble(longitudeDMS.toString().substring(minutesIndex + 1, longitudeDMS.length()));
			}
		}

		if (east) {
			return (deg + min / 60.0 + sec / 3600.0);
		}
		return (-deg - min / 60.0 - sec / 3600.0);
	}

	/**
	 * Valid latitude values are: Northern Hemisphere: Latitude_DMS: "51:41:20.00"
	 * Latitude_DMS: "51:41:20.00N" Latitude_DMS: "+51:41:20.00" Southern
	 * Hemisphere: Latitude_DMS: "51:41:20.00S" Latitude_DMS: "-51:41:20.00"
	 * 
	 * @param pLatitude_DMS
	 *            string representation
	 * @return The latitude in degrees
	 */
	public static double parseLatDMSColon(String pLatitude_DMS) {
		return parseLatDMSColon(new StringWrapper(pLatitude_DMS));
	}

	private static double parseLatDMSColon(StringWrapper pLatitude_DMS) {
		StringWrapper latitudeDMS = pLatitude_DMS.replace(" ", "");
		if (latitudeDMS.startsWith(SYMBOL_NORTH) || latitudeDMS.startsWith(SYMBOL_SOUTH)) {
			latitudeDMS.moveFirstCharacterToEnd();
		}

		boolean north = true;
		if (latitudeDMS.startsWith('+')) {
			latitudeDMS = latitudeDMS.substring(1);
		} else if (latitudeDMS.startsWith('-')) {
			latitudeDMS = latitudeDMS.substring(1);
			north = false;
		}
		int degreesIndex = latitudeDMS.indexOf(SYMBOL_COLON);
		if (degreesIndex == -1) {
			throw new RuntimeException("The latitude " + pLatitude_DMS + " should be of the format DD" + SYMBOL_COLON
					+ "MM" + SYMBOL_COLON + "SS.SS");
		}
		double deg = Double.parseDouble(latitudeDMS.toString().substring(0, degreesIndex));

		latitudeDMS = latitudeDMS.substring(degreesIndex + 1);
		int minutesIndex = latitudeDMS.indexOf(SYMBOL_COLON);
		if (minutesIndex == -1) {
			throw new RuntimeException("The latitude " + pLatitude_DMS + " should be of the format DD" + SYMBOL_COLON
					+ "MM" + SYMBOL_COLON + "SS.SS");
		}
		StringWrapper minStr = latitudeDMS.substring(0, minutesIndex);
		minStr = minStr.replace(",", ".");
		double min = Double.parseDouble(minStr.toString());

		double sec = 0.0;
		char lastChar = latitudeDMS.getLastChar();
		if (lastChar == SYMBOL_SOUTH || lastChar == SYMBOL_NORTH) {
			if (minutesIndex + 1 < latitudeDMS.length() - 1) {
				StringWrapper secStr = latitudeDMS.substring(minutesIndex + 1, latitudeDMS.length() - 1);
				if (secStr.endsWith(SYMBOL_SECONDS)) {
					secStr = secStr.substring(0, secStr.length() - 1);
				}

				sec = Double.parseDouble(secStr.toString());
			}
			if (lastChar == SYMBOL_SOUTH) {
				if (!north) {
					throw new RuntimeException(
							"The latitude " + pLatitude_DMS + " cannot have both a - sign and " + SYMBOL_SOUTH);
				}
				north = false;
			}
		} else {
			if (minutesIndex + 1 < latitudeDMS.length()) {
				sec = Double.parseDouble(latitudeDMS.substring(minutesIndex + 1, latitudeDMS.length()).toString());
			}
		}
		if (north) {
			return (deg + min / 60.0 + sec / 3600.0);
		}
		return (-deg - min / 60.0 - sec / 3600.0);
	}

	/**
	 * Convert the DMS
	 * 
	 * @param pLongitude_DMS
	 *            New longitude in DMS format Valid longitude formats are Eastern of
	 *            Greenwich Longitude_DMS : "003:39:43.00" Longitude_DMS :
	 *            "003:39:43.00E" Longitude_DMS : "+003:39:43.00" Western of
	 *            Greenwich Longitude_DMS : "003:39:43.00W" Longitude_DMS :
	 *            "-003:39:43.00"
	 * @return The longitude in degrees
	 */
	public static double parseLonDMSColon(String pLongitude_DMS) {
		return parseLonDMSColon(new StringWrapper(pLongitude_DMS));
	}

	private static double parseLonDMSColon(StringWrapper pLongitude_DMS) {
		StringWrapper longitudeDMS = pLongitude_DMS.replace(" ", "");
		if (longitudeDMS.startsWith(SYMBOL_EAST) || longitudeDMS.startsWith(SYMBOL_WEST)) {
			longitudeDMS.moveFirstCharacterToEnd();
		}
		boolean east = true;
		if (longitudeDMS.startsWith('+')) {
			longitudeDMS = longitudeDMS.substring(1);
		} else if (longitudeDMS.startsWith('-')) {
			longitudeDMS = longitudeDMS.substring(1);
			east = false;
		}
		int degreesIndex = longitudeDMS.indexOf(SYMBOL_COLON);
		if (degreesIndex == -1) {
			throw new RuntimeException("The longitude " + pLongitude_DMS + " should be of the format DD" + SYMBOL_COLON
					+ "MM" + SYMBOL_COLON + "SS.SS");
		}
		double deg = Double.parseDouble(longitudeDMS.toString().substring(0, degreesIndex));

		longitudeDMS = longitudeDMS.substring(degreesIndex + 1);
		int minutesIndex = longitudeDMS.indexOf(SYMBOL_COLON);
		if (minutesIndex == -1) {
			throw new RuntimeException("The longitude " + pLongitude_DMS + " should be of the format DD" + SYMBOL_COLON
					+ "MM" + SYMBOL_COLON + "SS.SS");
		}
		StringWrapper minStr = longitudeDMS.substring(0, minutesIndex);
		minStr = minStr.replace(",", ".");
		double min = Double.parseDouble(minStr.toString());

		double sec = 0.0;
		char lastChar = longitudeDMS.getLastChar();
		if (lastChar == SYMBOL_WEST || lastChar == SYMBOL_EAST) {
			if (minutesIndex + 1 < longitudeDMS.length() - 1) {
				StringWrapper secStr = longitudeDMS.substring(minutesIndex + 1, longitudeDMS.length() - 1);
				if (secStr.endsWith(SYMBOL_SECONDS)) {
					secStr = secStr.substring(0, secStr.length() - 1);
				}

				sec = Double.parseDouble(secStr.toString());
			}
			if (lastChar == SYMBOL_WEST) {
				if (!east) {
					throw new RuntimeException(
							"The longitude " + pLongitude_DMS + " cannot have both a - sign and " + SYMBOL_WEST);
				}
				east = false;
			}
		} else {
			if (minutesIndex + 1 < longitudeDMS.length()) {
				sec = Double.parseDouble(longitudeDMS.toString().substring(minutesIndex + 1, longitudeDMS.length()));
			}
		}

		if (east) {
			return (deg + min / 60.0 + sec / 3600.0);
		}
		return (-deg - min / 60.0 - sec / 3600.0);
	}

	/**
	 * Convert the DMS, but with a check on [-90,+90] Valid latitude values are:
	 * Northern Hemisphere: Latitude_DMS: "51°41'20.00" Latitude_DMS: "51°41'20.00N"
	 * Latitude_DMS: "+51°41'20.00" Southern Hemisphere: Latitude_DMS:
	 * "51°41'20.00S" Latitude_DMS: "-51°41'20.00"
	 * 
	 * @param pLatitude_DMS
	 *            string representation
	 * @return The latitude in degrees
	 */
	public static double parseLatDMSBounded(String pLatitude_DMS) {
		double result = parseLatDMS(pLatitude_DMS);
		if ((result < -90) || (result > 90)) {
			throw new RuntimeException("the value should be in the range of -90.0 to +90.0" + SYMBOL_DEGREES);
		}
		return result;
	}

	/**
	 * Convert the DMS, but with a check on [-180,+180]
	 * 
	 * @param pLongitude_DMS
	 *            New longitude in DMS format Valid longitude formats are Eastern of
	 *            Greenwich Longitude_DMS : "003°39'43.00" Longitude_DMS :
	 *            "003°39'43.00E" Longitude_DMS : "+003°39'43.00" Western of
	 *            Greenwich Longitude_DMS : "003°39'43.00W" Longitude_DMS :
	 *            "-003°39'43.00"
	 * @return The longitude in degrees
	 */
	public static double parseLonDMSBounded(String pLongitude_DMS) {
		double result = parseLonDMS(pLongitude_DMS);
		if ((result < -180) || (result > 180)) {
			throw new RuntimeException("the value should be in the range of -180.0 to +180.0" + SYMBOL_DEGREES);
		}
		return result;
	}

	/**
	 * Set latitude and longitude, according to the ddMMssNdddMMssE format. N and E
	 * can also be S and W.
	 * 
	 * @param pLatLonDMS
	 *            Text value
	 * @return Lat lon array [°]
	 */
	public static double[] parseLatLonDMS(String pLatLonDMS) {
		double[] result = new double[2];

		int latIndex = pLatLonDMS.indexOf(SYMBOL_NORTH);
		if (latIndex == -1) {
			latIndex = pLatLonDMS.indexOf(SYMBOL_SOUTH);
		}
		String latitudeDMS = pLatLonDMS.substring(0, latIndex + 1);
		boolean north = true;
		if (latitudeDMS.charAt(latIndex) == SYMBOL_SOUTH) {
			north = false;
		}
		double deg = Double.parseDouble(latitudeDMS.substring(0, 2));
		double min = Double.parseDouble(latitudeDMS.substring(2, 4));
		double sec = Double.parseDouble(latitudeDMS.substring(4, 6));
		int lastNumberIndex = latIndex - 1;
		if (lastNumberIndex > 5) {
			int numberOfDecimals = lastNumberIndex - 5;
			sec = sec + (Double.parseDouble(latitudeDMS.substring(6, lastNumberIndex + 1))
					/ Math.pow(10, numberOfDecimals));
		}
		if (north) {
			result[0] = deg + min / 60.0 + sec / 3600.0;
		} else {
			result[0] = -deg - min / 60.0 - sec / 3600.0;
		}

		int lonIndex = pLatLonDMS.indexOf(SYMBOL_EAST);
		if (lonIndex == -1) {
			lonIndex = pLatLonDMS.indexOf(SYMBOL_WEST);
		}
		String longitudeDMS = pLatLonDMS.substring(latIndex + 1, lonIndex + 1);
		lonIndex = lonIndex - latIndex;
		boolean east = true;
		if (longitudeDMS.charAt(lonIndex - 1) == SYMBOL_WEST) {
			east = false;
		}
		deg = Double.parseDouble(longitudeDMS.substring(0, 3));
		min = Double.parseDouble(longitudeDMS.substring(3, 5));
		sec = Double.parseDouble(longitudeDMS.substring(5, 7));
		lastNumberIndex = lonIndex - 1;
		if (lastNumberIndex > 7) {
			int numberOfDecimals = lastNumberIndex - 7;
			sec = sec
					+ (Double.parseDouble(longitudeDMS.substring(7, lastNumberIndex)) / Math.pow(10, numberOfDecimals));
		}
		if (east) {
			result[1] = deg + min / 60.0 + sec / 3600.0;
		} else {
			result[1] = -deg - min / 60.0 - sec / 3600.0;
		}
		return result;
	}

	/**
	 * Set latitude and longitude, according to the ddMMssNdddMMssE format. N and E
	 * can also be S and W. Check min and max values for the values.
	 * 
	 * @param pLatLonDMS
	 *            Text value
	 * @return Lat lon array [°]
	 */
	public static double[] parseLatLonDMSBounded(String pLatLonDMS) {
		double[] result = parseLatLonDMS(pLatLonDMS);
		if ((result[0] < -90) || (result[0] > 90)) {
			throw new RuntimeException("The latitude value should be in the range of -90.0 to +90.0" + SYMBOL_DEGREES);
		}
		if ((result[1] < -180) || (result[1] > 180)) {
			throw new RuntimeException(
					"The longitude value should be in the range of -180.0 to +180.0" + SYMBOL_DEGREES);
		}
		return result;
	}

}
