package com.github.navisensfindme;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class User {
    private double initialLat;
    private double initialLong;
    private double currentLat;
    private double currentLong;

    public double getInitialLat() {
        return initialLat;
    }

    public void setInitialLat(double initialLat) {
        this.initialLat = initialLat;
    }

    public double getInitialLong() {
        return initialLong;
    }

    public void setInitialLong(double initialLong) {
        this.initialLong = initialLong;
    }

    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLong() {
        return currentLong;
    }

    public void setCurrentLong(double currentLong) {
        this.currentLong = currentLong;
    }

    public void init(double latitude, double longitude){
        this.setInitialLat(latitude);
        this.setInitialLong(longitude);
    }

    public void addMetersToDegree(double x, double y){
        double convertedLat;
        double convertedLong;
        double earthRadius =  6371009.0D; // earth_radius = 6371009 in meters
        convertedLat = (y/earthRadius) * (180/Math.PI);
        convertedLong = (x/earthRadius) * (180/Math.PI) / Math.cos(this.getInitialLat() * Math.PI/180);

        this.setCurrentLat(this.getInitialLat() + convertedLat);
        this.setCurrentLong(this.getInitialLong() + convertedLong);
        Log.i("locationData","newLatitude,"+ this.currentLat + ",newLatitude,"+this.currentLong);
    }
}