package com.solvetec.derek.tides.Utils;

import android.content.Context;
import android.database.Cursor;

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
        Viewport viewport = graphView.getViewport();
        List<Series> seriesList = graphView.getSeries();
        Series series = seriesList.get(0);
        GridLabelRenderer glr = graphView.getGridLabelRenderer();
        final Context context = graphView.getContext();

        graphView.getGraphContentLeft();
        graphView.offsetLeftAndRight(0);


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
            public void setViewport(Viewport viewport) {

            }
        });


        // As a double-check, calculate the time of day of the first, middle, and last entries in
        // the cursor. Display these values on the data grid label.
//
        int valueColumnIndex = dataCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_VALUE);
        int dateColumnIndex = dataCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_DATE);
//
//        dataCursor.moveToFirst();
//        Long dateFirst = dataCursor.getLong(dateColumnIndex);
//        dataCursor.moveToLast();
//        Long dateLast = dataCursor.getLong(dateColumnIndex);
//
//        String dateFirstString = DateUtils.getDateString(dateFirst, context.getString(R.string.format_date_date_and_time));
//        String dateLastString = DateUtils.getDateString(dateLast, context.getString(R.string.format_date_date_and_time));
//
        ArrayList<String> dates = new ArrayList<>();

        if (dataCursor.moveToFirst()) {
            int i = 0;
            do {
                Long date = dataCursor.getLong(dateColumnIndex);
                String dateString = DateUtils.getDateString(date, context.getString(R.string.format_date_date_and_time));
                dates.add(dateString);
            } while (dataCursor.moveToNext());
        }

        String temp = "temp";

//        String[] datesArray = (String[]) dates.toArray();



    }
}
