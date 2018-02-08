package com.solvetec.derek.tides.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.solvetec.derek.tides.R;
import com.solvetec.derek.tides.SunriseSunset;
import com.solvetec.derek.tides.data.TidesContract;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dsolven on 10/31/2017.
 */

public class GraphViewUtils {

    private static final String TAG = GraphViewUtils.class.getCanonicalName();

    public static final int GRAPH_SUNRISE = 0;
    public static final int GRAPH_SUNSET = 1;
    public static final int GRAPH_TIDE = 2;
    public static final int GRAPH_CURRENT_TIME = 3;

    public static DataPoint[] getSeries(Cursor cursor) {

        ArrayList<DataPoint> seriesList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Double value = cursor.getDouble(cursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_VALUE));
                Long date = cursor.getLong(cursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_DATE));
                seriesList.add(new DataPoint(date, value));
            } while (cursor.moveToNext());
        }

        DataPoint[] dataPointArray = new DataPoint[seriesList.size()];
        dataPointArray = seriesList.toArray(dataPointArray);
        return dataPointArray;
    }

    public static void formatGraphBounds(GraphView graphView) {
        // TODO: 11/6/2017 Dev: Here are all of the objects inside graphView
        Viewport viewport = graphView.getViewport();
        List<Series> seriesList = graphView.getSeries();
        LineGraphSeries series = (LineGraphSeries) seriesList.get(GRAPH_TIDE);
        GridLabelRenderer glr = graphView.getGridLabelRenderer();
        final Context context = graphView.getContext();

        // Manual bounds
        glr.setHumanRounding(false);
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(series.getLowestValueX());
        viewport.setMaxX(series.getHighestValueX());

        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0d);
        viewport.setMaxY(humanRound(series.getHighestValueY(), true));


        // Setup the label renderer
        glr.setLabelFormatter(new LabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return DateUtils.getDateString(Double.valueOf(value).longValue(), context.getString(R.string.format_date_hours_and_seconds));
                } else {
                    return Double.valueOf(value).toString();
                }
            }

            @Override
            public void setViewport(Viewport viewport) {}
        });
    }

    public static void formatSeriesColorTide(GraphView graphView) {
        Context context = graphView.getContext();

        List<Series> seriesList = graphView.getSeries();

        // Tide data
        LineGraphSeries series = (LineGraphSeries) seriesList.get(GRAPH_TIDE);
        int backgroundColor = context.getResources().getColor(R.color.colorGraphBackground);
        int backgroundColorOpacityMask = context.getResources().getColor(R.color.colorGraphBackgroundOpacityMask);
        backgroundColor = backgroundColor & backgroundColorOpacityMask; // Set opacity
        series.setBackgroundColor(backgroundColor);
        series.setDrawBackground(true);

    }

    public static void formatSeriesColorSunrise(GraphView graphView) {
        Context context = graphView.getContext();

        List<Series> seriesList = graphView.getSeries();

        // Sunrise and sunset
        int sunsetColor = context.getResources().getColor(R.color.colorGraphSunsetBackground);
        int sunsetColorOpacityMask = context.getResources().getColor(R.color.colorGraphSunsetBackgroundOpacityMask);
        sunsetColor = sunsetColor & sunsetColorOpacityMask; // Set opacity
        // Sunrise
        LineGraphSeries sunriseSeries = (LineGraphSeries) seriesList.get(GRAPH_SUNRISE);
        sunriseSeries.setBackgroundColor(sunsetColor);
        sunriseSeries.setDrawBackground(true);
        // Sunset
        LineGraphSeries sunsetSeries = (LineGraphSeries) seriesList.get(GRAPH_SUNSET);
        sunsetSeries.setBackgroundColor(sunsetColor);
        sunsetSeries.setDrawBackground(true);
    }

    // Copied from GridLabelRenderer.java.
    private static double humanRound(double in, boolean roundAlwaysUp) {
        // round-up to 1-steps, 2-steps or 5-steps
        int ten = 0;
        while (Math.abs(in) >= 10d) {
            in /= 10d;
            ten++;
        }
        while (Math.abs(in) < 1d) {
            in *= 10d;
            ten--;
        }
        if (roundAlwaysUp) {
            if (in == 1d) {
            } else if (in <= 2d) {
                in = 2d;
            } else if (in <= 5d) {
                in = 5d;
            } else if (in < 10d) {
                in = 10d;
            }
        } else { // always round down
            if (in == 1d) {
            } else if (in <= 4.9d) {
                in = 2d;
            } else if (in <= 9.9d) {
                in = 5d;
            } else if (in < 15d) {
                in = 10d;
            }
        }
        return in * Math.pow(10d, ten);
    }
}
