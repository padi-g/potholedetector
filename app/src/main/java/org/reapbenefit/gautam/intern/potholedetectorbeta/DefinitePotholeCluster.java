package org.reapbenefit.gautam.intern.potholedetectorbeta;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class DefinitePotholeCluster implements ClusterItem {

    private LatLng position;

    public DefinitePotholeCluster(LatLng position) {
        this.position = position;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }
}
