package quitersstorssguard.utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import quitersstorssguard.Main;


public class Miscellaneous {


	// **************************************************************************************************************************************************
	// ****************************************************************** Declarations ******************************************************************
	// **************************************************************************************************************************************************
	final static int BUFFER_SIZE = 8192;



	// **************************************************************************************************************************************************
	// ******************************************************************** Methods *********************************************************************
	// **************************************************************************************************************************************************
	public static String readFileFromResources(final String resourcePath) {

		try (InputStream inputStream = Main.class.getResourceAsStream(resourcePath);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE)) {

			final List<String> lines = new ArrayList<>();

			String str;
			while ((str = bufferedReader.readLine()) != null)
				lines.add(str);

			return String.join(System.lineSeparator(), lines);

		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}


	public static long convertToTimestamp(String dateTime) {
		ZoneOffset zoneOffset = ZoneOffset.UTC; // Convert to UTC-based timestamps

		if (dateTime.endsWith("Z")) { // Handle ISO-8601 format with 'Z' (UTC)
			return Instant.parse(dateTime).toEpochMilli();
		} else { // Handle LocalDateTime without explicit timezone
			LocalDateTime localDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return localDateTime.toInstant(zoneOffset).toEpochMilli();
		}
	}


	/**
	 * Formats a Duration into a human-readable string (e.g., "2h 30m 15s 200ms").
	 * Omits zero-value units unless they are the only component.
	 *
	 * @param duration The duration to format.
	 * @return Formatted string (e.g., "30m 5s", "500ms", "1h").
	 */
	public static String durationToHumanReadable(Duration duration) {

		if (duration.isZero())
			return "0 ms"; // Handle zero duration

		boolean isNegative = duration.isNegative();
		duration = duration.abs();

		long hours = duration.toHours();
		long minutes = duration.toMinutesPart();
		long seconds = duration.toSecondsPart();
		long millis = duration.toMillisPart();

		StringBuilder formatted = new StringBuilder();

		if (hours != 0)
			formatted.append(hours).append("h ");

		// Show minutes if hours exist (even if 0)
		if (minutes != 0 || hours != 0)
			formatted.append(minutes).append("m ");

		// Show seconds if higher units exist
		if (seconds != 0 || minutes != 0 || hours != 0)
			formatted.append(seconds).append("s ");

		// Show ms if nothing else or if non-zero
		if (millis != 0 || formatted.isEmpty())
			formatted.append(millis).append("ms");


        return isNegative ? "-" + formatted.toString().strip() : formatted.toString().strip();
	}


}
