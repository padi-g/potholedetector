package org.reapbenefit.gautam.intern.potholedetectorbeta;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class CustomBitmapDescriptorFactory extends Object {

    public static BitmapDescriptor fromHitPercentage(double hitPercentage) {
        if (hitPercentage <= 20) {
            // lowest possible hit percentage
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        }
        else if (hitPercentage <= 40) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        }
        else if (hitPercentage <= 60) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
        }
        else if (hitPercentage <= 80) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE);
        }
        else {
            // highest possible hit percentage
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }
    }
}
