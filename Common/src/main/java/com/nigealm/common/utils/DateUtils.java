package com.nigealm.common.utils;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gil on 03/05/2016.
 */
public class DateUtils {
    private static final Tracer tracer = new Tracer(DateUtils.class);

    public static Date parseDate(String dateStr) {
        Date date;
        try {
            date = org.apache.commons.lang3.time.DateUtils.parseDate(dateStr);
        } catch (ParseException e) {
            date = formatStringToDate(dateStr);
            if (date == null) {
                tracer.exception("parseDate", e);
            }
        }
        return date;
    }

    private static Date formatStringToDate(String date) {
        if (date == null)
            return null;
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        Date res = null;
        try {
            res = f.parse(date);
        } catch (ParseException e) {
            //do nothing
        }
        if (res != null)
            return res;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        try {
            res = dateFormat.parse(date);
        } catch (ParseException e) {
            //do nothing
        }
        if (res != null)
            return res;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d = sdf.parse(date);
            String formattedTime = output.format(d);
            res = output.parse(formattedTime);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return res;

        try {
            Date d = sdf.parse(date);
            String formattedTime = output.format(d);
            res = output.parse(formattedTime);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return res;

        try {
            dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            res = dateFormat.parse(date);
        } catch (ParseException e) {
            // do nothing
        }
        try {
            dateFormat = new SimpleDateFormat("dd/MMM/yy h:mm a", Locale.ENGLISH);
            res = dateFormat.parse(date);
        } catch (ParseException e) {
            // do nothing
        }
        return res;
    }

    public static Date parseDateToLocalTime(String date) {
        Date dateObj = parseDate(date);
        if (dateObj == null) {
            return null;
        }

        // Get TimeZone of user
        TimeZone currentTimeZone = Calendar.getInstance().getTimeZone();
        Calendar currentDt = new GregorianCalendar(currentTimeZone, Locale.ENGLISH);
        // Get the Offset from GMT taking DST into account
        int gmtOffset = currentTimeZone.getOffset(
                currentDt.get(Calendar.ERA),
                currentDt.get(Calendar.YEAR),
                currentDt.get(Calendar.MONTH),
                currentDt.get(Calendar.DAY_OF_MONTH),
                currentDt.get(Calendar.DAY_OF_WEEK),
                currentDt.get(Calendar.MILLISECOND));
        // convert to hours
        gmtOffset = gmtOffset / (60 * 60 * 1000);
        Calendar requestedDate = Calendar.getInstance();
        requestedDate.setTime(dateObj);

        // Adjust for GMT (note the offset negation)
        requestedDate.add(Calendar.HOUR_OF_DAY, gmtOffset);
        return requestedDate.getTime();
    }

    public static List<Date> getAllDatesBetweenDates(Date startDate, Date endDate, boolean ascending) {
        if (ascending)
            return getAllDatesBetweenDatesAscending(startDate, endDate);
        else
            return getAllDatesBetweenDatesDescending(startDate, endDate);
    }

    private static List<Date> getAllDatesBetweenDatesAscending(Date startDate, Date endDate) {
        List<Date> res = new ArrayList<Date>();

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startDate);

        while (calendar.getTime().before(endDate) || calendar.getTime().equals(endDate)) {
            Date result = calendar.getTime();
            result = getDateInFormat(result, null, false);
            res.add(result);
            res.add(result);
            calendar.add(Calendar.DATE, 1);
        }
        return res;
    }

    private static List<Date> getAllDatesBetweenDatesDescending(Date startDate, Date endDate) {
        List<Date> res = new ArrayList<Date>();

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(endDate);

        while (calendar.getTime().after(startDate) || calendar.getTime().equals(startDate)) {
            Date result = calendar.getTime();
            result = getDateInFormat(result, null, false);
            res.add(result);
            calendar.add(Calendar.DATE, -1);
        }
        return res;
    }

    public static Date formatDateStringToDate(String date) {
        if (date == null)
            return null;
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        Date res = null;
        try {
            res = f.parse(date);
        } catch (ParseException e) {
            //do nothing
        }
        if (res != null)
            return res;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        try {
            res = dateFormat.parse(date);
        } catch (ParseException e) {
            //do nothing
        }
        if (res != null)
            return res;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d = sdf.parse(date);
            String formattedTime = output.format(d);
            res = output.parse(formattedTime);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return res;

        try {
            Date d = sdf.parse(date);
            String formattedTime = output.format(d);
            res = output.parse(formattedTime);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return res;

        try {
            dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            res = dateFormat.parse(date);
        } catch (ParseException e) {
            // do nothing
        }
        return res;
    }

    public static Date getDateInFormat(Date date, String format, boolean includeTime) {
        // default: "yyyy-MM-dd"
        if (format == null)
            format = "yyyy-MM-dd";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date formatted = sdf.parse(sdf.format(date));

            if (!includeTime) {
                formatted.setHours(0);
                formatted.setMinutes(0);
                formatted.setSeconds(0);
            }

            return formatted;
        } catch (ParseException e) {
            return date;
        }

    }

    public static Date getDateInUTCMidnight(Date date) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar origDate = Calendar.getInstance();
        origDate.setTime(date);

        c.set(Calendar.YEAR, origDate.get(Calendar.YEAR));
        c.set(Calendar.MONTH, origDate.get(Calendar.MONTH));
        c.set(Calendar.DATE, origDate.get(Calendar.DATE));
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date res = c.getTime();
        return res;
    }

    public static Date getSameDateInMidnight(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date res = c.getTime();
        return res;
    }

    public static Date getSameDateEndOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date res = c.getTime();
        return res;
    }

    public static Date getDateInUTC(Date date) {
        Calendar origDate = Calendar.getInstance();
        origDate.setTime(date);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, origDate.get(Calendar.YEAR));
        c.set(Calendar.MONTH, origDate.get(Calendar.MONTH));
        c.set(Calendar.DATE, origDate.get(Calendar.DATE));
        c.set(Calendar.HOUR, origDate.get(Calendar.HOUR));
        c.set(Calendar.HOUR_OF_DAY, origDate.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, origDate.get(Calendar.MINUTE));
        c.set(Calendar.SECOND, origDate.get(Calendar.SECOND));
        c.set(Calendar.MILLISECOND, origDate.get(Calendar.MILLISECOND));
        Date res = c.getTime();
        return res;
    }

    public static Date getDateInUTCEndOfDay(Date date) {
        Calendar origDate = Calendar.getInstance();
        origDate.setTime(date);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, origDate.get(Calendar.YEAR));
        c.set(Calendar.MONTH, origDate.get(Calendar.MONTH));
        c.set(Calendar.DATE, origDate.get(Calendar.DATE));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        Date res = c.getTime();
        return res;
    }

    public static String getExecutionDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String executionDate = calendar.get(Calendar.YEAR) + "-";
        executionDate += getNumberInTwoDigits(calendar.get(Calendar.MONTH) + 1) + "-";
        executionDate += getNumberInTwoDigits(calendar.get(Calendar.DAY_OF_MONTH));
        return executionDate;
    }

    private static String getNumberInTwoDigits(int number){
        if (number > 9){
            return number + "";
        } else{
            return "0" + number;
        }

    }

    public static boolean isFirstDataCollectionDate(Date date){
        if (date == null){
            return true;
        }
        return date.equals(new Date(0));
    }



}
