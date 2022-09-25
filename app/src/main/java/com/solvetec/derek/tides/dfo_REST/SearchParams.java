package com.solvetec.derek.tides.dfo_REST;


/**
 * Created by dsolven on 10/25/2017.
 */

public class SearchParams {
    public String dataName;
    public double latitudeMin;
    public double latitudeMax;
    public double longitudeMin;
    public double longitudeMax;
    public double depthMin;
    public double depthMax;
    public String dateMin;
    public String dateMax;
    public int start;
    public int sizeMax;
    public boolean metadata;
    public String metadataSelection;
    public String order;

    public SearchParams(
            String dataName,
            double latitudeMin,
            double latitudeMax,
            double longitudeMin,
            double longitudeMax,
            double depthMin,
            double depthMax,
            String dateMin,
            String dateMax,
            int start,
            int sizeMax,
            boolean metadata,
            String metadataSelection,
            String order) {

        this.dataName = dataName;
        this.latitudeMin = latitudeMin;
        this.latitudeMax = latitudeMax;
        this.longitudeMin = longitudeMin;
        this.longitudeMax = longitudeMax;
        this.depthMin = depthMin;
        this.depthMax = depthMax;
        this.dateMin = dateMin;
        this.dateMax = dateMax;
        this.start = start;
        this.sizeMax = sizeMax;
        this.metadata = metadata;
        this.metadataSelection = metadataSelection;
        this.order = order;
    }
}
