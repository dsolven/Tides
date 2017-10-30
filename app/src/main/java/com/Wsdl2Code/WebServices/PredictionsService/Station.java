package com.Wsdl2Code.WebServices.PredictionsService;

/**
 * Created by dsolven on 10/22/2017.
 */

public class Station {

    public int station_id;
    public String station_name;
    public double latitude;
    public double longitude;

    public Station() {
    }

    public Station(int station_id, String station_name, double latitude, double longitude) {
        this.station_id = station_id;
        this.station_name = station_name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
