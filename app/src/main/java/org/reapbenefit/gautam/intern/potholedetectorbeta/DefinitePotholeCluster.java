package org.reapbenefit.gautam.intern.potholedetectorbeta;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class DefinitePotholeCluster implements ClusterItem {

    private LatLng position;
    private String title;
    private int hits;

    public DefinitePotholeCluster(LatLng position) {
        this.position = position;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getHits() {
        return hits;
    }
}
