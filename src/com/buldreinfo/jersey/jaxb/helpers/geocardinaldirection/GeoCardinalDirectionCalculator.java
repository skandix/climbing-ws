package com.buldreinfo.jersey.jaxb.helpers.geocardinaldirection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonReader;

public class GeoCardinalDirectionCalculator {
	private static Logger logger = LogManager.getLogger();
	private final List<GeoPoint> geoPoints = new ArrayList<>();
	private GeoPoint firstPointLow;
	private GeoPoint firstPointHigh;
	private GeoPoint secondPointLow;
	private GeoPoint secondPointHigh;
	private double bearing;
	private long wallDirection;
	private String cardinalDirection;

	public GeoCardinalDirectionCalculator(String outline, boolean verbose) throws IOException {
		parseOutline(outline);
		calculateDistanceToCenter();
		calculateBoundingBox();
		this.bearing = getBearing(firstPointLow, secondPointLow);
		// Add or subtract 90 degrees to get perpendicular vector in the walls facing direction
		int degreesDelta = getPerpendicularDegrees();
		if (degreesDelta == -90 || degreesDelta == 90) {
			this.wallDirection = ((Math.round(bearing) + degreesDelta)+360) % 360;
			this.cardinalDirection = convertFromDegreesToCardinalDirection(wallDirection);
		}
	}

	private void calculateBoundingBox() {
		// Find bounding box
		List<GeoPoint> boundingBoxPoints = geoPoints
				.stream()
				.sorted(Comparator.comparingDouble(GeoPoint::getDistanceToCenter).reversed())
				.collect(Collectors.toList())
				.subList(0, 4);
		Preconditions.checkArgument(boundingBoxPoints.size() == 4, "Invalid bounding box");
		// Group the four points in two pairs
		for (int i = 0; i < boundingBoxPoints.size(); i++) {
			GeoPoint a = boundingBoxPoints.get(i);
			for (int j = 0; j < boundingBoxPoints.size(); j++) {
				if (i != j) {
					GeoPoint b = boundingBoxPoints.get(j);
					double distance = getDistance(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
					if (distance < a.getNeighbourDistance()) {
						a.setNeighbour(b, distance);
					}
				}
			}
		}
		// Set variables
		GeoPoint g11 = boundingBoxPoints.get(0);
		GeoPoint g12 = g11.getNeighbourPoint();
		GeoPoint g21 = boundingBoxPoints.stream().filter(x -> x != g11 && x != g12).findAny().get();
		GeoPoint g22 = g21.getNeighbourPoint();
		if (g11.getElevation() < g12.getElevation()) {
			firstPointLow = g11;
			firstPointHigh = g12;
		}
		else {
			firstPointLow = g12;
			firstPointHigh = g11;
		}
		if (g21.getElevation() < g22.getElevation()) {
			secondPointLow = g21;
			secondPointHigh = g22;
		}
		else {
			secondPointLow = g22;
			secondPointHigh = g21;
		}
	}

	private void calculateDistanceToCenter() {
		// TODO: This fails when polygon crosses 180th meridian!
		double minLatitude = geoPoints.stream().mapToDouble(GeoPoint::getLatitude).min().getAsDouble();
		double maxLatitude = geoPoints.stream().mapToDouble(GeoPoint::getLatitude).max().getAsDouble();
		double minLongitude = geoPoints.stream().mapToDouble(GeoPoint::getLongitude).min().getAsDouble();
		double maxLongitude = geoPoints.stream().mapToDouble(GeoPoint::getLongitude).max().getAsDouble();
		double centerLatitude = (minLatitude + maxLatitude) / 2.0;
		double centerLongitude = (minLongitude + maxLongitude) / 2.0;
		for (GeoPoint p : geoPoints) {
			p.setDistanceToCenter(getDistance(centerLatitude, centerLongitude, p.getLatitude(), p.getLongitude()));
		}
	}

	public String getCardinalDirection() {
		return cardinalDirection;
	}

	private String convertFromDegreesToCardinalDirection(long bearing) {
		if (bearing < 0 && bearing > -180) {
			// Normalize to [0,360]
			bearing = 360 + bearing;
		}
		if (bearing > 360 || bearing < -180) {
			throw new RuntimeException("Invalid bearing: " + bearing);
		}
		String directions[] = {
				"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
				"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N"};
		String cardinal = directions[(int) Math.floor(((bearing + 11.25) % 360) / 22.5)];
		return cardinal;
	}

	private long getBearing(GeoPoint g1, GeoPoint g2) {
		double longitude1 = g1.getLongitude();
		double longitude2 = g2.getLongitude();
		double latitude1 = Math.toRadians(g1.getLatitude());
		double latitude2 = Math.toRadians(g2.getLatitude());
		double longDiff= Math.toRadians(longitude2-longitude1);
		double y= Math.sin(longDiff)*Math.cos(latitude2);
		double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);
		return Math.round(Math.toDegrees(Math.atan2(y, x))+360)%360;
	}

	private double getDistance(double lat1, double lng1, double lat2, double lng2) {
		final int R = 6371; // Radius of the earth
		double latDistance = Math.toRadians(lat2 - lat1);
		double lngDistance = Math.toRadians(lng2 - lng1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters
		return distance;
	}

	private int getPerpendicularDegrees() {
		// Use points with greatest elevation difference
		double direction = (getBearing(firstPointHigh, firstPointLow) + getBearing(secondPointHigh, secondPointLow)) / 2.0;
		logger.debug("bearing={}, direction={}, firstPointLow={}, firstPointHigh={}, secondPointLow={}, secondPointHigh={}, vectorLow={}, vectorHigh={}",
				bearing, direction,
				firstPointLow, firstPointHigh, secondPointLow, secondPointHigh,
				(firstPointLow.getLatitude() + "," + firstPointLow.getLongitude() + ";" + secondPointLow.getLatitude() + "," + secondPointLow.getLongitude()),
				(firstPointHigh.getLatitude() + "," + firstPointHigh.getLongitude() + ";" + secondPointHigh.getLatitude() + "," + secondPointHigh.getLongitude()));
		if (direction > bearing) {
			return 90;
		}
		return -90;
	}
	
	private void parseOutline(String outline) throws IOException {
		String locations = outline.replaceAll(";", "|");
		double latitude = 0, longitude = 0, elevation = 0;
		String apiKey = BuldreinfoConfig.getConfig().getProperty(BuldreinfoConfig.PROPERTY_KEY_GOOGLE_APIKEY);
		URL url = new URL(String.format("https://maps.googleapis.com/maps/api/elevation/json?locations=%s&key=%s", locations, apiKey));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		int responseCode = connection.getResponseCode();
		Preconditions.checkArgument(responseCode == 200, "Invalid responseCode: " + responseCode);
		try (InputStream is = connection.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				JsonReader jsonReader = new JsonReader(isr)) {
			jsonReader.beginObject();
			while (jsonReader.hasNext()) {
				String field = jsonReader.nextName();
				if (field.equals("results")) {
					jsonReader.beginArray();
					while (jsonReader.hasNext()) {
						jsonReader.beginObject();
						while (jsonReader.hasNext()) {
							field = jsonReader.nextName();
							if (field.equals("elevation")) {
								elevation = jsonReader.nextDouble();
							}
							else if (field.equals("location")) {
								jsonReader.beginObject();
								while (jsonReader.hasNext()) {
									field = jsonReader.nextName();
									if (field.equals("lat")) {
										latitude = jsonReader.nextDouble();
									}
									else if (field.equals("lng")) {
										longitude = jsonReader.nextDouble();
									}
									else {
										jsonReader.skipValue();
									}
								}
								jsonReader.endObject();
							}
							else {
								jsonReader.skipValue();
							}
						}
						geoPoints.add(new GeoPoint(latitude, longitude, elevation));
						jsonReader.endObject();						
					}
					jsonReader.endArray();
				} else {
					jsonReader.skipValue();
				}

			}
			jsonReader.endObject();
		}
	}
}
