package vn.ssdc.vnpt.utils;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StringUtils {

    public static String convertDate(String date, String fromFormat, String toFormat) {
        String result = null;
        try {

            // Declare date
            DateFormat dateFormat = new SimpleDateFormat(fromFormat);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(toFormat);

            // Convert date time
            Date dateDf = dateFormat.parse(date);
            result = simpleDateFormat.format(dateDf);

        } catch (ParseException e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public static String convertDateWithTimeZone(String date, String fromFormat, TimeZone fromTimeZone, String toFormat, TimeZone toTimeZone) {
        String result = null;
        try {

            // Declare date
            DateFormat dateFormat = new SimpleDateFormat(fromFormat);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(toFormat);

            // Set time zone
            dateFormat.setTimeZone(fromTimeZone);
            simpleDateFormat.setTimeZone(toTimeZone);

            // Convert date time
            Date dateDf = dateFormat.parse(date);
            result = simpleDateFormat.format(dateDf);

        } catch (ParseException e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    // Convert from zone +0 to zone default
    public static String convertDateFromElk(String date, String fromFormat, String toFormat) {
        return convertDateWithTimeZone(date, fromFormat, TimeZone.getTimeZone("GMT+0"), toFormat, TimeZone.getDefault());
    }

    // Convert from zone default to zone +0
    public static String convertDateToElk(String date, String fromFormat, String toFormat) {
        return convertDateWithTimeZone(date, fromFormat, TimeZone.getDefault(), toFormat, TimeZone.getTimeZone("GMT+0"));
    }

    public static String convertDateToElk1(String date, String fromFormat, String toFormat) {
        return convertDateWithTimeZone(date, fromFormat, TimeZone.getDefault(), toFormat, TimeZone.getTimeZone("GMT+7"));
    }

    public static Long convertDatetimeToTimestamp(String dateTime, String fromFormat) {
        Long result = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(fromFormat);
            Date date = dateFormat.parse(dateTime);
            Timestamp timestamp = new Timestamp(date.getTime());
            result = timestamp.getTime();
        } catch (ParseException e) {
            result = null;
            e.printStackTrace();
        }
        return result;
    }

    public static String convertDateToString(int interval) {
        Calendar cal = Calendar.getInstance();
        if (interval > 0) {
            cal.add(Calendar.MINUTE, -interval);
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
    }

    public static long convertDateToLong(String date, String format) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        // Convert date time
        Date dateDf = simpleDateFormat.parse(date);
        return dateDf.getTime();
    }

    public static String convertToUtf8(String str) {
        byte[] byteText = str.getBytes(Charset.forName("UTF-8"));
        String originalString = "";
        try {
            originalString = new String(byteText, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return originalString;
    }

    public static String toZoneDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(value, formatter);

        // Add zone time
        ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime.toLocalDate(), dateTime.toLocalTime(), ZoneId.systemDefault());
        return zonedDateTime.toOffsetDateTime().toString();
    }
}
