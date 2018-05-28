package com.composum.pages.commons.model;

import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.composum.pages.commons.PagesConstants.META_NODE_NAME;
import static com.composum.pages.commons.service.TrackingService.STATS_NODE_NAME;

/**
 * a model implementation to generate a JSON data set of the statistics meta data of a page
 * <p>
 * The model is using the meta data of the containing page of the models resource.
 * The request selectors are scanned for range hints for the data output:
 * <ul>
 * <li>'y-'dddd</li> specifies the year of the range
 * <li>'m-'dd</li> specifies the month of the range
 * <li>'w-'dd</li> specifies the week of the year alternatively to the month
 * <li>'d-'dd</li> specifies the day ot the month (hides a week selector)
 * </ul>
 * Examples:
 * <ul>
 * <li>'/path/to/page/_jcr_content.statistics.m-04.d-01.json' generates the data for the 1st April of the current year</li>
 * <li>'/path/to/page/_jcr_content.statistics.d-01.json' generates the data for the 1st Day of the current month</li>
 * <li>'/path/to/page/_jcr_content.statistics.y-2000.w-02.json' generates the data of the 2nd week of year 2000</li>
 * <li>'/path/to/page/_jcr_content.statistics.json' generates the data of the current month</li>
 * </ul>
 * </p>
 */
public class Statistics extends AbstractModel {

    private static final Logger LOG = LoggerFactory.getLogger(Statistics.class);

    public static final String YEAR_FORMAT = "yyyy";
    public static final String MONTH_FORMAT = YEAR_FORMAT + "-MM";
    public static final String DAY_OF_MONTH_FORMAT = MONTH_FORMAT + "-dd";
    public static final String WEEK_FORMAT = "yyyy-ww";
    public static final String DAY_OF_WEEK_FORMAT = "E," + WEEK_FORMAT + "-dd";

    public static final String DAY_PATH_FORMAT = "'y-'yyyy'/m-'MM'/d-'dd";

    public interface DataSet {

        void toJSON(JsonWriter writer) throws IOException;
    }

    public static final Data NO_DATA = new Data(0, 0);

    public static class Data implements DataSet {

        public int total;
        public int unique;

        public Data(int total, int unique) {
            this.total = total;
            this.unique = unique;
        }

        @Override
        public void toJSON(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("total").value(total);
            writer.name("unique").value(unique);
            writer.endObject();
        }

        @Override
        public String toString() {
            return "[t:" + total + ",u:" + unique + "]";
        }
    }

    public class Referer implements DataSet, Comparable<Referer> {

        public final String url;
        public final Data data;

        public Referer(Resource resource) {
            ValueMap values = resource.getValueMap();
            this.url = values.get("url", "");
            this.data = new Data(values.get("total", 0), values.get("unique", 0));
        }

        public void toJSON(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("url").value(url);
            writer.name("summary");
            data.toJSON(writer);
            writer.endObject();
        }

        @Override
        public int compareTo(@Nonnull Referer other) {
            return data.unique - other.data.unique;
        }
    }

    public class Day implements DataSet {

        public final Calendar date;
        public final Data summary;
        public final List<Data> hours = new ArrayList<>();
        public final List<Referer> referrers = new ArrayList<>();

        public Day(Calendar date) {
            this.date = (Calendar) date.clone();
            for (int index = 0; index < 24; index++) {
                hours.add(NO_DATA);
            }
            summary = NO_DATA;
        }

        public Day(Resource resource, Calendar date) {
            this.date = (Calendar) date.clone();
            int total = 0;
            int unique = 0;
            for (int index = 0; index < 24; index++) {
                hours.add(NO_DATA);
            }
            for (Resource child : resource.getChildren()) {
                String name = child.getName();
                if (name.startsWith("h-")) {
                    ValueMap values = child.getValueMap();
                    Data hour = new Data(values.get("total", 0), values.get("unique", 0));
                    total += hour.total;
                    unique += hour.unique;
                    int index = Integer.valueOf(name.substring(2));
                    hours.set(index, hour);
                } else if ("referer".equals(name)) {
                    for (Resource referer : child.getChildren()) {
                        referrers.add(new Referer(referer));
                    }
                }
            }
            summary = new Data(total, unique);
            Collections.sort(referrers);
        }

        @Override
        public void toJSON(JsonWriter writer) throws IOException {
            toJSON(writer, null, true);
        }

        public void toJSON(JsonWriter writer, DateFormat labelFormat, boolean detail) throws IOException {
            writer.beginObject();
            writer.name("type").value("day");
            writer.name("label").value((labelFormat != null ? labelFormat : new SimpleDateFormat(DAY_OF_MONTH_FORMAT))
                    .format(date.getTime()));
            writer.name("summary");
            summary.toJSON(writer);
            if (detail) {
                writer.name("entries").beginArray();
                for (int i = 0; i < 24; i++) {
                    Data hour = hours.get(i);
                    writer.beginObject();
                    writer.name("type").value("hour");
                    writer.name("label").value(String.format("%02d", i));
                    writer.name("summary");
                    hour.toJSON(writer);
                    writer.endObject();
                }
                writer.endArray();
                writer.name("referrers").beginArray();
                for (Referer referer : referrers) {
                    referer.toJSON(writer);
                }
                writer.endArray();
            }
            writer.endObject();
        }

        @Override
        public String toString() {
            return "{d:" + new SimpleDateFormat(DAY_OF_MONTH_FORMAT).format(date.getTime()) + summary + "}";
        }
    }

    public abstract class Days implements DataSet {

        public final Calendar from;
        public final Calendar to;
        public final Data summary;
        public final List<Day> days = new ArrayList<>();
        public final Map<String, Referer> referrers = new HashMap<>();

        public Days(Resource statistics, Calendar from, Calendar to) {
            SimpleDateFormat pathFormat = new SimpleDateFormat(DAY_PATH_FORMAT);
            this.from = (Calendar) from.clone();
            this.to = (Calendar) to.clone();
            int total = 0;
            int unique = 0;
            int count = 0;
            while (!from.after(to)) {
                Day day = getDay(statistics, from);
                days.add(day);
                for (Referer referer : day.referrers) {
                    Referer refererSummary = referrers.get(referer.url);
                    if (refererSummary == null) {
                        referrers.put(referer.url, referer);
                    } else {
                        refererSummary.data.total += referer.data.total;
                        refererSummary.data.unique += referer.data.unique;
                    }
                }
                total += day.summary.total;
                unique += day.summary.unique;
                from.add(Calendar.DAY_OF_MONTH, 1);
                if (++count > 40) {
                    DateFormat format = new SimpleDateFormat(DAY_OF_MONTH_FORMAT);
                    LOG.error("invalid range '{} - {}'", format.format(from.getTime()), format.format(to.getTime()));
                    break;
                }
            }
            summary = new Data(total, unique);
        }

        protected abstract String getType();

        protected abstract DateFormat getLabelFormat(boolean detail);

        protected abstract DateFormat getDayFormat();

        @Override
        public void toJSON(JsonWriter writer) throws IOException {
            toJSON(writer, true);
        }

        public void toJSON(JsonWriter writer, boolean detail) throws IOException {
            writer.beginObject();
            writer.name("type").value(getType());
            writer.name("label").value(getLabelFormat(detail).format(from.getTime()));
            writer.name("summary");
            summary.toJSON(writer);
            if (detail) {
                DateFormat dayFormat = getDayFormat();
                writer.name("entries").beginArray();
                for (Day day : days) {
                    day.toJSON(writer, dayFormat, false);
                }
                writer.endArray();
                writer.name("referrers").beginArray();
                List<Referer> referrers = new ArrayList<>(this.referrers.values());
                Collections.sort(referrers);
                for (Referer referer : referrers) {
                    referer.toJSON(writer);
                }
                writer.endArray();
            }
            writer.endObject();
        }

        @Override
        public String toString() {
            return "{" + getType().charAt(0) + ":" + getLabelFormat(true).format(from.getTime()) + summary + "}";
        }
    }

    public class Week extends Days {

        public final int year;
        public final int week;

        public Week(Resource statistics, int year, int week) {
            super(statistics, firstDayOfWeek(getLocale(), year, week), lastDayOfWeek(getLocale(), year, week));
            this.year = year;
            this.week = week;
        }

        @Override
        protected String getType() {
            return "week";
        }

        @Override
        protected DateFormat getLabelFormat(boolean detail) {
            return new SimpleDateFormat(detail ? WEEK_FORMAT : "'W'ww");
        }

        @Override
        protected DateFormat getDayFormat() {
            return new SimpleDateFormat("E");
        }
    }

    public class Month extends Days {

        public final int year;
        public final int month;

        public Month(Resource statistics, int year, int month) {
            super(statistics, firstDayOfMonth(getLocale(), year, month), lastDayOfMonth(getLocale(), year, month));
            this.year = year;
            this.month = month;
        }

        @Override
        protected String getType() {
            return "month";
        }

        @Override
        protected DateFormat getLabelFormat(boolean detail) {
            return new SimpleDateFormat(detail ? MONTH_FORMAT : "MMM");
        }

        @Override
        protected DateFormat getDayFormat() {
            return new SimpleDateFormat("dd");
        }
    }

    public class Year implements DataSet {

        public final int year;
        public final Data summary;
        public final List<Month> months = new ArrayList<>();
        public final Map<String, Referer> referrers = new HashMap<>();

        public Year(Resource statistics, int year) {
            this.year = year;
            int total = 0;
            int unique = 0;
            for (int i = 0; i < 12; i++) {
                Month month = new Month(statistics, year, i + 1);
                months.add(month);
                for (String key : month.referrers.keySet()) {
                    Referer entry = month.referrers.get(key);
                    Referer refererSummary = referrers.get(key);
                    if (refererSummary == null) {
                        referrers.put(key, entry);
                    } else {
                        refererSummary.data.total += entry.data.total;
                        refererSummary.data.unique += entry.data.unique;
                    }
                }
                total += month.summary.total;
                unique += month.summary.unique;
            }
            summary = new Data(total, unique);
        }

        @Override
        public void toJSON(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("type").value("year");
            writer.name("label").value("" + year);
            writer.name("summary");
            summary.toJSON(writer);
            writer.name("entries").beginArray();
            for (Month month : months) {
                month.toJSON(writer, false);
            }
            writer.endArray();
            writer.name("referrers").beginArray();
            List<Referer> referrers = new ArrayList<>(this.referrers.values());
            Collections.sort(referrers);
            for (Referer referer : referrers) {
                referer.toJSON(writer);
            }
            writer.endArray();
            writer.endObject();
        }

        @Override
        public String toString() {
            return "{y:" + year + summary + "}";
        }
    }

    private transient Resource statistics;
    private transient DataSet dataSet;

    public Resource getStatistics() {
        if (statistics == null) {
            Resource resource = getResource();
            Resource pageResource = getPageManager().getContainingPageResource(resource);
            if (pageResource != null) {
                Resource metaData = pageResource.getChild(META_NODE_NAME);
                if (metaData != null) {
                    statistics = metaData.getChild(STATS_NODE_NAME);
                }
            }
            if (statistics == null) {
                statistics = new NonExistingResource(getContext().getResolver(),
                        resource.getPath() + "/" + META_NODE_NAME + "/" + STATS_NODE_NAME);
            }
        }
        return statistics;
    }

    public DataSet getDataSet() {
        if (dataSet == null) {
            RequestPathInfo pathInfo = getContext().getRequest().getRequestPathInfo();
            Calendar today = new GregorianCalendar();
            today.setTime(new Date());
            Integer year = today.get(Calendar.YEAR);
            Integer month = today.get(Calendar.MONTH) + 1;
            Integer week = null;
            Integer day = null;
            String[] selectors = pathInfo.getSelectors();
            for (String selector : selectors) {
                if (selector.startsWith("y-")) {
                    year = Integer.valueOf(selector.substring(2));
                } else if (selector.startsWith("m-")) {
                    month = selector.length() == 4 ? Integer.valueOf(selector.substring(2)) : null;
                } else if (selector.startsWith("w-")) {
                    week = Integer.valueOf(selector.substring(2));
                } else if (selector.startsWith("d-")) {
                    day = Integer.valueOf(selector.substring(2));
                }
            }
            Resource statistics = getStatistics();
            if (week != null) {
                dataSet = new Week(statistics, year, week);
            } else {
                if (day != null) {
                    dataSet = getDay(statistics, year, month != null ? month : today.get(Calendar.MONTH) + 1, day);
                } else {
                    if (month != null) {
                        dataSet = new Month(statistics, year, month);
                    } else {
                        dataSet = new Year(statistics, year);
                    }
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("getDataSet({}): '{}'",
                        "y-" + year + (week != null ? "/w-" + week : "/m-" + month) + (day != null ? "/d-" + day : ""),
                        dataSet);
            }
        }
        return dataSet;
    }

    protected Day getDay(Resource statistics, int year, int month, int day) {
        return getDay(statistics, day(getLocale(), year, month, day));
    }

    protected Day getDay(Resource statistics, Calendar date) {
        String path = new SimpleDateFormat(DAY_PATH_FORMAT).format(date.getTime());
        Resource dayRes = statistics.getChild(path);
        return dayRes != null ? new Day(dayRes, date) : new Day(date);
    }

    // helpers

    public static Calendar day(Locale locale, int year, int month, int day) {
        Calendar result = Calendar.getInstance(locale);
        result.set(Calendar.YEAR, year);
        result.set(Calendar.MONTH, month - 1);
        result.set(Calendar.DAY_OF_MONTH, day);
        return result;
    }

    public static Calendar firstDayOfWeek(Locale locale, int year, int week) {
        Calendar day = Calendar.getInstance(locale);
        day.set(Calendar.YEAR, year);
        day.set(Calendar.WEEK_OF_YEAR, week);
        day.set(Calendar.DAY_OF_WEEK, day.getFirstDayOfWeek());
        return day;
    }

    public static Calendar lastDayOfWeek(Locale locale, int year, int week) {
        Calendar day = Calendar.getInstance(locale);
        day.set(Calendar.YEAR, year);
        day.set(Calendar.WEEK_OF_YEAR, week);
        day.set(Calendar.DAY_OF_WEEK, day.getFirstDayOfWeek() + 6);
        return day;
    }

    public static Calendar firstDayOfMonth(Locale locale, int year, int month) {
        Calendar day = Calendar.getInstance(locale);
        day.set(Calendar.YEAR, year);
        day.set(Calendar.MONTH, month - 1);
        day.set(Calendar.DAY_OF_MONTH, 1);
        return day;
    }

    public static Calendar lastDayOfMonth(Locale locale, int year, int month) {
        Calendar day = Calendar.getInstance(locale);
        day.set(Calendar.YEAR, year);
        day.set(Calendar.MONTH, month - 1);
        day.set(Calendar.DAY_OF_MONTH, 1);
        day.roll(Calendar.DATE, false);
        return day;
    }
}
