package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import org.reapbenefit.gautam.intern.potholedetectorbeta.DefinitePotholeCluster;


public class CustomClusterRenderer extends DefaultClusterRenderer<DefinitePotholeCluster>{

    private double hitPercentage;

    public CustomClusterRenderer(Context context, GoogleMap map, ClusterManager<DefinitePotholeCluster> clusterManager) {
        super(context, map, clusterManager);
    }

    public void setHitPercentage(double hitPercentage) {
        this.hitPercentage = hitPercentage;
    }

    @Override
    protected void onBeforeClusterItemRendered(DefinitePotholeCluster item, MarkerOptions markerOptions) {
        markerOptions.icon(CustomBitmapDescriptorFactory.fromHitPercentage(hitPercentage));
        super.onBeforeClusterItemRendered(item, markerOptions);
    }
}
