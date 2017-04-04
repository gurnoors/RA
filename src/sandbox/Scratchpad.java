package sandbox;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class Scratchpad {

	public static void main(String[] args) {
		ZonedDateTime now = ZonedDateTime.now().minusDays(1);
		parseTime("20:54:51", now);
	}

	private static void parseTime(String input, ZonedDateTime dateRef) {
		LocalTime time = LocalTime.parse(input);
		ZonedDateTime parsed = ZonedDateTime.of(dateRef.toLocalDate(), time, ZoneId.systemDefault());
		System.out.println(parsed);
	}



}
