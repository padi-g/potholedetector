package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import com.microsoft.windowsazure.mobileservices.*;

import java.net.MalformedURLException;

/*
Utility class to provide access to Azure App Service through mMobileBackendUrl. Should be used to connect auth
and DB instances.
 */

public class AzureServiceAdapter {
    private String mMobileBackendUrl = "https://potholedetector.azurewebsites.net";
    private Context mContext;
    private MobileServiceClient mClient;
    private static AzureServiceAdapter mInstance = null;

    private AzureServiceAdapter(Context context) {
        mContext = context;
        try {
            mClient = new MobileServiceClient(mMobileBackendUrl, mContext);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void Initialize(Context context) {
        if (mInstance == null) {
            mInstance = new AzureServiceAdapter(context);
        } else {
            // throw new IllegalStateException("AzureServiceAdapter is already initialized");
        }
    }

    public static AzureServiceAdapter getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException("AzureServiceAdapter is not initialized");
        }
        return mInstance;
    }

    public MobileServiceClient getClient() {
        return mClient;
    }

    // Place any public methods that operate on mClient here.
}