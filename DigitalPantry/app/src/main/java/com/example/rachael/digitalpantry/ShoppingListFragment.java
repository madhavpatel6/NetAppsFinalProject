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
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ShoppingListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ShoppingListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShoppingListFragment extends Fragment implements View.OnClickListener {
    private static final String RPC_QUEUE_NAME = "home_queue";
//    private static final String IP_ADDRESS  = "192.168.0.42";
    private static final String ARRAY = "array";
    private static final String SHARED_PREFS = "shared prefs";
    private static final String IP_ADDRESS  = "172.30.121.246";
    private OnFragmentInteractionListener mListener;
    ListView listView;
    final static ArrayList<ShoppingItem> shoppingList = new ArrayList<>();
    Button submit;
    FloatingActionButton addItemButton;
    TextView cost;
    static private Double totalCost;

    public ShoppingListFragment() {
        // Required empty public constructor
    }

    public static ShoppingListFragment newInstance() {
        ShoppingListFragment fragment = new ShoppingListFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if (prefs.contains(ARRAY)) {
            Gson gson = new Gson();
            shoppingList.clear();

            ArrayList<Object> objects = gson.fromJson(prefs.getString(ARRAY, ""), ArrayList.class);
            for (int i = 0; i < objects.size(); i++) {
                ShoppingItem item = gson.fromJson( gson.toJson(objects.get(i)), ShoppingItem.class);
                shoppingList.add(item);
            }
        }
        final String[] send = new String[shoppingList.size()];
        for (int i = 0; i < shoppingList.size(); i++) {
            send[i] = shoppingList.get(i).name;
        }

        totalCost = 0.0;
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

                    Gson gson = new Gson();
                    String message = "{\"type\":\"price\",\"data\":" + gson.toJson(send) + "}"; // This is the message I send to Madhav
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
                    Log.d("Rabbit MQ", response);
                    updateGUI(response);

                    channel.close();
                    connection.close();
                } catch (IOException | TimeoutException | InterruptedException ex) {
                    Log.e("Rabbit Mq ERROR", "I broke it!");
                    ex.printStackTrace();
                }
            }
        }).start();

    }

    public void updateGUI(String response) {
        Log.d("Rabbit MQ", response);
        Gson gson = new Gson();
        totalCost = 0.0;
        ArrayList<Double> prices = gson.fromJson(response, ArrayList.class);
        for (int i=0; i<prices.size(); i++) {
            shoppingList.get(i).cost = prices.get(i);
            totalCost += prices.get(i);
        }
        updateView();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);

        /* Initialize all views here */
        listView = (ListView) view.findViewById(R.id.add_items_list);
        submit = (Button) view.findViewById(R.id.add_items_btn);
        addItemButton = (FloatingActionButton) view.findViewById(R.id.add_items_fab);
        cost = (TextView) view.findViewById(R.id.cost);

        submit.setOnClickListener(this);


        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creating alert Dialog with one Button
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Base);
                // Setting Dialog Title
                alertDialog.setTitle("ADD ITEM TO SHOPPING LIST");

                // Setting Dialog Message
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);

                final TextView m_text = new TextView(getContext());
                m_text.setText("Item Name");
                m_text.setTextColor(Color.WHITE);
                layout.addView(m_text);
                final EditText name = new EditText(getContext());
                layout.addView(name);
                final TextView m_text1 = new TextView(getContext());
                m_text1.setText("Item Quantity");
                m_text1.setTextColor(Color.WHITE);
                layout.addView(m_text1);
                final EditText quantity = new EditText(getContext());
                layout.addView(quantity);
                alertDialog.setView(layout);

                // Setting Positive "Add" Button
                alertDialog.setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int which) {
                                // TODO: get cost from api call
                                final ShoppingItem item = new ShoppingItem(name.getText().toString().toLowerCase(),
                                        "", 0.0, Integer.parseInt(quantity.getText().toString()));

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

                                            Gson gson = new Gson();
                                            String message = "{\"type\":\"price\",\"data\":[\"" + item.name + "\"]}"; // This is the message I send to Madhav
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
                                            Log.d("Rabbit MQ", response);
                                            ArrayList<Double> prices = gson.fromJson(response, ArrayList.class);
                                            item.cost = prices.get(0);
                                            totalCost += item.cost;
                                            shoppingList.add(item);
                                            updateView();
                                            channel.close();
                                            connection.close();
                                        } catch (IOException | TimeoutException | InterruptedException ex) {
                                            Log.e("Rabbit Mq ERROR", "I broke it!");
                                            ex.printStackTrace();
                                        }
                                    }
                                }).start();


                                updateView();
                            }
                        });
                // Setting Negative "Cancel" Button
                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }
        });


        for (ShoppingItem item : shoppingList) {
            totalCost += item.cost;
        }
        updateView();

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                totalCost -= shoppingList.get(position).cost;
                shoppingList.remove(position);
                updateView();
                return true;
            }
        });
        return view;

    }
    private void updateView() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                CustomShoppingListAdapter adapter = new CustomShoppingListAdapter(getActivity(), shoppingList);
                listView.setAdapter(adapter);
                cost.setText(String.format("%.2f", totalCost));
            }
        });
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

    @Override
    public void onClick(View v) {
        totalCost = 0.0;
        updateView();

        Log.d("Rabbit MQ", Integer.toString(shoppingList.size()));

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

                    String send =  "[";
                    for (int i = 0; i < shoppingList.size(); i++) {
                        send+= "{\"name\":\""+shoppingList.get(i).name+"\",\"quantity\":\""+shoppingList.get(i).quantity+"\"}";
                        if (i+1 != shoppingList.size()) {
                            send += ",";
                        }
                    }
                    send += "]";

                    String message = "{\"type\":\"add\",\"data\":" + send + "}"; // This is the message I send to Madhav
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
                    shoppingList.clear();
                    updateView();
                    channel.close();
                    connection.close();
                } catch (IOException | TimeoutException | InterruptedException ex) {
                    Log.e("Rabbit Mq ERROR", "I broke it!");
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Gson gson = new Gson();
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(SHARED_PREFS,
                Context.MODE_PRIVATE).edit();
        editor.putString(ARRAY, gson.toJson(shoppingList));
        editor.commit();
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
