package com.example.rachael.digitalpantry;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Created by Rachael on 12/7/2016.
 */

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView imageView;
    Activity myActivity;

    public DownloadImageTask(Activity activity, ImageView bmImage) {
        this.imageView = bmImage;
        this.myActivity = activity;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap myBitmap = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            myBitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Drawable draw =  myActivity.getResources().getDrawable(R.drawable.ic_no_image);
            myBitmap = ((BitmapDrawable) draw).getBitmap();
            Log.e("Error", e.getMessage());
        }
        return myBitmap;
    }

    protected void onPostExecute(Bitmap result) {
        imageView.setImageBitmap(result);
    }
}

//new DownloadImageTask((ImageView) findViewById(R.id.imageView1))
//        .execute(MY_URL_STRING);
