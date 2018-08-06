package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.util.Log;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Properties;

public class BlobClientProvider {
    /**
     * Validates the connection string and returns the storage blob client.
     * The connection string must be in the Azure connection string format.
     *
     * @return The newly created CloudBlobClient object
     *
     * @throws RuntimeException
     * @throws IOException
     * @throws URISyntaxException
     * @throws IllegalArgumentException
     * @throws InvalidKeyException
     */
    private static final String TAG = "BlobClientProvider";
    private static final String connectionString = "***REMOVED***";

    public static CloudBlobClient getBlobClientReference() throws RuntimeException, IOException, IllegalArgumentException,
            URISyntaxException, InvalidKeyException {

        CloudStorageAccount cloudStorageAccount = null;
        try {
            cloudStorageAccount = CloudStorageAccount.parse(connectionString);
            return cloudStorageAccount.createCloudBlobClient();
        } catch (IllegalArgumentException|URISyntaxException e) {
            // thrown when the connection string specifies an invalid URI.
            // check that the string is in the correct Azure connection format.
            // Log.e(TAG, e.getMessage());
        }
        catch (InvalidKeyException e) {
            // thrown when the connection string specifies an invalid key.
            // confirm that the account name and account key in the connection string are valid
            // Log.e(TAG, e.getMessage());
        }
        catch (NullPointerException e) {
            // thrown when the cloudStorageAccount object could not be created.
            // Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
