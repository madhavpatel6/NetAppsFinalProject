package com.example.rachael.digitalpantry;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;

public class Add_Item_Activity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> itemList;
    ArrayList<String> imageURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add__item);

        itemList = new ArrayList<>();
        imageURL = new ArrayList<>();
        createArrays();

//        listView = (ListView) findViewById(R.id.add_items_list);
//        CustomShoppingListAdapter adapter = new CustomShoppingListAdapter(this,
//                convertToArray(itemList),
//                convertToArray(imageURL));
//        listView.setAdapter(adapter);
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//            }
//        });

    }


    private String[] convertToArray(ArrayList<String> array) {
        String[] mStringArray = new String[array.size()];
        mStringArray = array.toArray(mStringArray);
        return mStringArray;
    }
    private void createArrays() {
        itemList.add("Apple");
        itemList.add("Oranges");
        itemList.add("Eggs");

        imageURL.add("http://images.clipartpanda.com/teacher-apple-clipart-apple.png");
        imageURL.add("");
        imageURL.add("http://cliparting.com/wp-content/uploads/2016/08/Egg-clipart-clipart-cliparts-for-you.jpg");
    };

    private void alertDialog(String title, String message) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText new_item = new EditText(this);

        alert.setTitle(title);
        alert.setMessage(title);

        alert.setView(new_item);

        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String myNewItem = new_item.getText().toString();

                // add him to the list of items
            }
        });

        alert.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alert.show();
    }

}
