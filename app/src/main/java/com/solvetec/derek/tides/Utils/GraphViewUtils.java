package com.solvetec.derek.tides.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.solvetec.derek.tides.R;
import com.solvetec.derek.tides.data.TidesContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dsolven on 10/31/2017.
 */

public class GraphViewUtils {

    public static LineGraphSeries<DataPoint> getSeries(Cursor cursor) {

        ArrayList<DataPoint> seriesList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Double value = cursor.getDouble(cursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_VALUE));
                Long date = cursor.getLong(cursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_DATE));
                seriesList.add(new DataPoint(date, value));
            } while (cursor.moveToNext());
        }

        DataPoint[] dataPointArray = seriesList.toArray(new DataPoint[0]);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPointArray);
        return series;
    }

    public static void formatGraph(GraphView graphView, Cursor dataCursor) {
        // TODO: 11/6/2017 Dev: Here are all of the objects inside graphView
        Viewport viewport = graphView.getViewport();
        List<Series> seriesList = graphView.getSeries();
        LineGraphSeries series = (LineGraphSeries) seriesList.get(0);
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

        // Colours
        series.setBackgroundColor(context.getResources().getColor(R.color.colorGraphBackground));
        series.setDrawBackground(true);
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
