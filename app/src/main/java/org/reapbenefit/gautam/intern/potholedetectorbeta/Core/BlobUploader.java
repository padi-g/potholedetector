package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.InputStream;


public class BlobUploader extends IntentService {
    // TODO: storageConnectionString to be retrieved from Azure Account. Move to SAS in next commit.
    private static final String storageConnectionString = "";
    private Uri uploadUri;

    public BlobUploader() {
        super("BlobUploader");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            CloudBlobContainer container = getContainer();
            container.createIfNotExists();
            uploadUri = intent.getParcelableExtra("upload_uri");
            String filename = uploadUri.toString();
            CloudBlockBlob fileBlob = container.getBlockBlobReference(filename);
            InputStream fileStream = getContentResolver().openInputStream(this.uploadUri);
            fileBlob.upload(fileStream, fileStream.available());
        } catch (Exception e) {}

    }

    private CloudBlobContainer getContainer() throws Exception {
        CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("debug");

        return container;
    }
}
