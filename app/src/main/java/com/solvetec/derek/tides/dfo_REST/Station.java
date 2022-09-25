package com.solvetec.derek.tides.dfo_REST;

/**
 * Created by dsolven on 10/22/2017.
 */

public class Station {

    public String station_id;
    public String station_name;
    public double latitude;
    public double longitude;
    public String timezone_id;

    public Station() {
    }

    public Station(String station_id, String station_name, double latitude, double longitude, String timezone_id) {
        this.station_id = station_id;
        this.station_name = station_name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone_id = timezone_id;
    }

    // Copy constructor
    public Station(Station station) {
        this.station_id = station.station_id;
        this.station_name = station.station_name;
        this.latitude = station.latitude;
        this.longitude = station.longitude;
        this.timezone_id = station.timezone_id;
    }
}
