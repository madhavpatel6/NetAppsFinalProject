package com.example.rachael.digitalpantry;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InPantryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InPantryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InPantryFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static final String RPC_QUEUE_NAME = "home_queue";
//    private static final String IP_ADDRESS  = "192.168.0.42";
    private static final String ARRAY = "array";
    private static final String SHARED_PREFS = "shared prefs";
    private static final String IP_ADDRESS  = "172.30.121.246";
    public InPantryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment InPantryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InPantryFragment newInstance() {
        InPantryFragment fragment = new InPantryFragment();
        return fragment;
    }
@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }

    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                // Make the connection to the queue
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(IP_ADDRESS);
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                String replyQueueName = channel.queueDeclare().getQueue();
                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicConsume(replyQueueName, true, consumer);
                // ----- Done making connection to the Queue -------------- //

                String response = null;
                String corrId = java.util.UUID.randomUUID().toString();

                AMQP.BasicProperties props = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(corrId)
                        .replyTo(replyQueueName)
                        .build();

                String message = "{\"type\":\"pantry\"}"; // This is the message I send to Madhav
                Log.d("Rabbit MQ", "Message Sent: " + message);
                channel.basicPublish("", RPC_QUEUE_NAME, props, message.getBytes());

                while (true) {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                        response = new String(delivery.getBody());
                        break;
                    }
                }

                // Handle the response message
                // parse into the following information
                updateGUI(response);

                channel.close();
                connection.close();
            } catch (IOException | TimeoutException | InterruptedException ex) {
                Log.e("Rabbit Mq ERROR", "I broke it!");
                ex.printStackTrace();
            }
        }
    }).start();


    // TODO: generate list of objects in pantry

        list = new ArrayList<ListItem>();
    }

    ListView listView;
    ArrayList<ListItem> list;

    public void updateGUI(String response) {
        Log.d("Rabbit MQ", response);
        Gson gson = new Gson();
        ArrayList<Object> objects = gson.fromJson(response, ArrayList.class);
        list.clear();
        for (Object o: objects) {
            ListItem item = gson.fromJson(gson.toJson(o), ListItem.class);
            item.imageUrl = "";
            list.add(item);
        }

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try{
                    updateView();
                }
                catch (Exception e) {

                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_in_pantry, container, false);

        /* Initialize all views here */
        listView = (ListView) view.findViewById(R.id.pantry_list);
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        // Creating alert Dialog with one Button
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Base);
                        // Setting Dialog Title
                        alertDialog.setTitle("ADD ITEM TO SHOPPING LIST");
                        // Setting Positive "Add" Button
                        alertDialog.setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int which) {
                                        Gson gson = new Gson();
                                        ArrayList<ShoppingItem> shoppingList = new ArrayList<ShoppingItem>();
                                        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                                        if (prefs.contains(ARRAY)) {
                                            ArrayList<Object> objects = gson.fromJson(prefs.getString(ARRAY, ""), ArrayList.class);
                                            for (int i = 0; i < objects.size(); i++) {
                                                ShoppingItem item = gson.fromJson( gson.toJson(objects.get(i)), ShoppingItem.class);
                                                shoppingList.add(item);
                                            }
                                        }
                                        ShoppingItem item = new ShoppingItem(list.get(position).name, "",
                                                0.0, 1);
                                        shoppingList.add(item);
                                        SharedPreferences.Editor edit = prefs.edit();
                                        edit.putString(ARRAY, gson.toJson(shoppingList));
                                        edit.commit();
                                    }
                                });
                        // Setting Negative "Cancel" Button
                        alertDialog.setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Write your code here to execute after dialog
                                        dialog.cancel();
                                    }
                                });
                        alertDialog.show();
                    }
                }
        );
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Base);
//                // Setting Dialog Title
//                alertDialog.setTitle("ADD ITEM TO SHOPPING LIST");
//
//                // Setting Dialog Message
//                alertDialog.setMessage("Add Item");
//                final EditText input = new EditText(getContext());
//                alertDialog.setView(input);
//
//                // Setting Positive "Add" Button
//                alertDialog.setPositiveButton("Add",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog,int which) {
//                                // TODO: get cost from api call
//                                shoppingList.add(new ShoppingItem(input.getText().toString(),
//                                        "", 0.0));
//                                updateView();
//                            }
//                        });
//                // Setting Negative "Cancel" Button
//                alertDialog.setNegativeButton("Cancel",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                // Write your code here to execute after dialog
//                                dialog.cancel();
//                            }
//                        });
//                alertDialog.show();
//            }
//        });
        updateView();
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: do we want to allow user to add to shopping list from here
                updateView();
                return true;
            }
        });
        return view;

    }
    private void updateView() {
        CustomListAdapter adapter = new CustomListAdapter(getActivity(), list);
        listView.setAdapter(adapter);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
