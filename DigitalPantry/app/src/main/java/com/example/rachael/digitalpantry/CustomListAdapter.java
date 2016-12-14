package com.example.rachael.digitalpantry;

/**
 * Created by Rachael on 12/12/2016.
 */

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<ListItem> items;

    public CustomListAdapter(Activity context, List<ListItem> items) {
        super(context, R.layout.mylist, new String[items.size()]);
        this.context=context;
        this.items = items;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.mylist, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.item);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView extratxt = (TextView) rowView.findViewById(R.id.textView1);

        txtTitle.setText(items.get(position).name);
        new DownloadImageTask(context, imageView).execute(items.get(position).imageUrl);
        extratxt.setText("Quantity: " + items.get(position).quantity);
        return rowView;

    };
}
