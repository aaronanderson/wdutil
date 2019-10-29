package @@PACKAGE@@;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

//java.time adapters not available yet https://github.com/eclipse-ee4j/jaxb-ri/issues/1174
public class XmlAdapters {

	public static class DateAdapter extends XmlAdapter<String, LocalDate> {

		@Override
		public LocalDate unmarshal(String dateTime) throws Exception {
			if (dateTime != null) {
				return LocalDate.parse(dateTime, DateTimeFormatter.ISO_DATE);
			}
			return null;
		}

		@Override
		public String marshal(LocalDate dateTime) throws Exception {
			if (dateTime != null) {
				return dateTime.format(DateTimeFormatter.ISO_DATE);
			}
			return null;
		}

	}

	public static class DateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

		@Override
		public OffsetDateTime unmarshal(String dateTime) throws Exception {
			if (dateTime != null) {
				return OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			}
			return null;
		}

		@Override
		public String marshal(OffsetDateTime dateTime) throws Exception {
			if (dateTime != null) {
				return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			}
			return null;
		}

	}

	public static class TimeAdapter extends XmlAdapter<String, OffsetTime> {

		public OffsetTime unmarshal(String s) {
			if (s != null) {
				//return Date.from(LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC));
				return OffsetTime.parse(s, DateTimeFormatter.ISO_OFFSET_TIME);
			}
			return null;
		}

		public String marshal(OffsetTime dt) {
			if (dt != null) {
				//return  LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME);
				return dt.format(DateTimeFormatter.ISO_OFFSET_TIME);
			}
			return null;
		}
	}

}
