package rangeTest;

import java.util.List;

import javax.swing.tree.DefaultTreeCellEditor.EditorContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.ArrayList;

import org.apache.commons.lang3.SystemUtils;
import org.gavaghan.geodesy.*;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class Plot {
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;
	private static final long ROUND_TIME = 100000000l;
	private static final String BASE_DIR = "/Users/gurnoorsinghbhatia/Documents/code/sem2/ra/rangeTest/concLogs/fiddle";
	private static final String rssiFile = BASE_DIR + "/rssi_time.csv";
	private static final String locFile = BASE_DIR + "/location.csv";
	private static final String opFile = BASE_DIR + "/opFile.csv";

	private class Loc {
		public Loc(double lat, double lon, double elev) {
			super();
			this.lat = lat;
			this.lon = lon;
			this.elev = elev;
		}

		double lat;
		double lon;
		double elev;
	}

	public static void main(String[] args) throws IOException {
		
		new File(opFile).createNewFile();
		Plot plotObj = new Plot();
		
		plotObj.plot(rssiFile, locFile, opFile);
	}

	/**
	 * Assumes location readings started before RSSI readings
	 * 
	 * @param rssiFile
	 * @param locFile
	 * @throws IOException
	 */
	private void plot(String rssiFile, String locFile, String opFile) throws IOException {
		boolean eof = false;
		Loc concentratorLoc = this.new Loc(0,0,0);
		Loc nodeLoc = this.new Loc(0, 0, 0);

		CSVReader rssiReader = new CSVReader(new FileReader(rssiFile));
		// time,lat,lon,elevation
		CSVReader locReader = new CSVReader(new FileReader(locFile));

		String[] locLine = locReader.readNext();
		String[] rssiLine = rssiReader.readNext();

		System.out.println(locLine[0] + ", " + locLine[1] + ", " + locLine[2] + ", " + locLine[3]);
		// NOTE: calling callibrate in loop will skip alternate lines unless u
		// return the lines just read.
		//alternatively, just read all initially, then pass around the read lists.
		
		callibrate(rssiReader, locReader);
		locLine = locReader.readNext();
		rssiLine = rssiReader.readNext();

		// first reading is concentrator
		concentratorLoc.lat = Double.valueOf(locLine[1]);
		concentratorLoc.lon = Double.valueOf(locLine[2]);
		concentratorLoc.elev = Double.valueOf(locLine[3]);

		List<Double> xData = new ArrayList<>();
		List<Double> yData = new ArrayList<>();
		
		nodeLoc.elev = 0;
		double prevElev;
		while (locLine != null && rssiLine != null) {
			prevElev = nodeLoc.elev;
			yData.add(Double.valueOf(rssiLine[5]));
			double dist;
			nodeLoc.lat = Double.valueOf(locLine[1]);
			nodeLoc.lon = Double.valueOf(locLine[2]);
			try {
				nodeLoc.elev = Double.valueOf(locLine[3]);
			} catch (java.lang.NumberFormatException e) {
				nodeLoc.elev =  prevElev;
				e.printStackTrace();
			}
			dist = calculateDistance(concentratorLoc, nodeLoc);
			xData.add(Double.valueOf(dist));
			
			System.out.println("RSSI: " + rssiLine[5]);
//			rssiLine = rssiReader.readNext();
//
//			// debug
//			System.out.println("distance: " + String.valueOf(dist));
//			if(locLine != null && rssiLine !=null){
//				LocalDateTime locTime = parseTimeUTC(locLine[0]);
//				try{
//					LocalDateTime rssiTime = parseTime(rssiLine[5], locTime);
//					System.out.println("RSSI time: "+rssiTime);
//				}catch (java.time.format.DateTimeParseException e) {
//				e.printStackTrace();
//				}
//				
//				System.out.println("locationTime: "+locTime);
//				
//			}
//			System.out.println("-------------\n");

			// TODO: verify callibration
			
			locLine = locReader.readNext();
			

//			if(!isApproxEqual(locTime, rssiTime))

			callibrate(rssiReader, locReader);
			
//			rssiReader.readNext();
			rssiLine = rssiReader.readNext();
			
			
//			LocalDateTime locTime = parseTimeUTC(locLine[0]);
//			LocalDateTime rssiTime = parseTime(rssiLine[5], locTime);
			
			
			
		}

		XYChart chart = QuickChart.getChart("RSSI Vs distance", "distance (in metres)", "RSSI (in dBm)", "rssi", xData, yData);
		new SwingWrapper(chart).displayChart();

	}

	/**
	 * Move both reader pointers to a point where they the time is same. i.e.
	 * Skip ahead until time in locLine != (firstLine time in rssiLine)
	 * 
	 * @param rssiReader
	 * @param locReader
	 * @throws IOException
	 */
	private void callibrate(CSVReader rssiReader, CSVReader locReader) throws IOException {
		System.out.println("Callibrating------");
		String[] locLine = locReader.readNext();
		String[] rssiLine = rssiReader.readNext();
		LocalDateTime locTime = parseTimeUTC(locLine[0]);
		LocalDateTime rssiTime = parseTime(rssiLine[5], locTime);

		// if(isApproxEqual(locTime, rssiTime))
		// return;
		//
		boolean isLocStartTimeEarlier = false;
		if (locTime.isBefore(rssiTime)) {
			isLocStartTimeEarlier = true;
		}
		while (locLine != null && rssiLine != null) {
			rssiTime = parseTime(rssiLine[5], locTime);
			locTime = parseTimeUTC(locLine[0]);
			System.out.println("RSSI time: "+ rssiTime);
			System.out.println("Loc Time: "+locTime);

			if (isApproxEqual(locTime, rssiTime)) {
				System.out.println("End Callibration------");
				return;
				
			} else if (isLocStartTimeEarlier) {
				if (locTime.isAfter(rssiTime.plusNanos(ROUND_TIME))){
					System.out.println("End Callibration------");
					return;
				}
				System.out.println("incrementing locReader");
				locLine = locReader.readNext();
			} else {
				if (rssiTime.isAfter(locTime.plusNanos(ROUND_TIME))){
					System.out.println("End Callibration------");
					return;
				}
				System.out.println("Incrementing rssiReader");
				rssiReader.readNext();// 1 reading is on 2 lines
				rssiLine = rssiReader.readNext();
			}
			
			
		}
		
		System.out.println("End Callibration------");
	}

	private double calculateDistance(Loc concentratorLoc, Loc nodeLoc) {
		GeodeticCalculator geoCalc = new GeodeticCalculator();

		Ellipsoid reference = Ellipsoid.WGS84;
		GlobalPosition home = new GlobalPosition(concentratorLoc.lat, concentratorLoc.lon, concentratorLoc.elev);
		GlobalPosition node = new GlobalPosition(nodeLoc.lat, nodeLoc.lon, nodeLoc.elev);

		return geoCalc.calculateGeodeticCurve(reference, node, home).getEllipsoidalDistance();
	}

	/**
	 * 
	 * @param isoTimeString
	 *            sample input: 2017-04-03T04:25:58.000Z (ISO-8601 fromt, UTC)
	 * @return
	 */
	private static LocalDateTime parseTimeUTC(String isoTimeString) {
		TemporalAccessor temporal = DATE_TIME_FORMATTER.parse(isoTimeString);
		Instant instant = Instant.from(temporal);
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		return localDateTime;
	}

	private static boolean isApproxEqual(LocalDateTime time1, LocalDateTime time2) {
		if (time1.equals(time2)) {
			return true;
		} else if (time1.isAfter(time2.minusNanos(ROUND_TIME)) && time1.isBefore(time2.plusNanos(ROUND_TIME))) {
			return true;
		} else {
			return false;
		}
	}

	private static LocalDateTime parseTime(String input, LocalDateTime locTime) {
		LocalTime time = LocalTime.parse(input);
		ZonedDateTime parsed = ZonedDateTime.of(locTime.toLocalDate(), time, ZoneId.systemDefault());
		return parsed.toLocalDateTime();
	}

}
