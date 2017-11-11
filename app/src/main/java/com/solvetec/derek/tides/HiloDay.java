package com.solvetec.derek.tides;

import java.util.List;

/**
 * Created by Derek on 11/6/2017.
 */

public class HiloDay {
    public Long timestamp;
    public List<Double> data;

    public HiloDay(Long timestamp, List<Double> data) {
        this.timestamp = timestamp;
        this.data = data;
    }
}
