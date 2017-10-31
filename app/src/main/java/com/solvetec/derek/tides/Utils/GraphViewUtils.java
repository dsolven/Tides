package com.solvetec.derek.tides.Utils;

import android.database.Cursor;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
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
        cursor.close();

        DataPoint[] dataPointArray = seriesList.toArray(new DataPoint[0]);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPointArray);
        return series;
    }
}
