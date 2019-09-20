package com.erg.reco;

import java.io.Serializable;

public class Poi implements Serializable {

    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private String category;
    private String photo;

    public Poi() {
        this.id = 0;
        this.name = "";
        this.latitude = 0;
        this.longitude = 0;
        this.category = "";
        this.photo = "";
    }

    public Poi(int id, String name, double latitude, double longitude, String category, String photo) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.photo = photo;
    }

    public Poi(String name, double latitude, double longitude) {
        this.id = -1;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = "default";
        this.photo = "";
    }

    public Poi(int id) {
        this.id = id;
        this.name = "";
        this.latitude = 0;
        this.longitude = 0;
        this.category = "default";
        this.photo = "";
    }

    public String getPhoto() {
        return this.photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String toString() {
        return String.valueOf(id) + "," + name + "," + String.valueOf(latitude) + "," + String.valueOf(longitude);
    }
}
