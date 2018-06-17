package org.reapbenefit.gautam.intern.potholedetectorbeta;

public class OnBoardingItem {
    private int imageID;
    private String title;
    private String description;

    public int getImageID() {
        return imageID;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageID(int imageID) {
        this.imageID = imageID;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
