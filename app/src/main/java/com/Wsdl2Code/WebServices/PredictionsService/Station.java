package com.Wsdl2Code.WebServices.PredictionsService;

/**
 * Created by dsolven on 10/22/2017.
 */

public class Station {

    private String station_id;
    private String station_name;

    public Station() {
    }

    public Station(String station_id, String station_name) {
        this.station_id = station_id;
        this.station_name = station_name;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }

    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }
}
