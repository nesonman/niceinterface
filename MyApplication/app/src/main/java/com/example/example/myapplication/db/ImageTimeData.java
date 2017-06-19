package com.example.example.myapplication.db;

public class ImageTimeData {
    public String imageName;
    public int imageID;
    public long imageTimestamp;

    public ImageTimeData(String imageName, int imageID, long imageTimestamp) {
        this.imageName = imageName;
        this.imageID = imageID;
        this.imageTimestamp = imageTimestamp;
    }
}
