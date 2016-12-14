package com.example.rachael.digitalpantry;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class RecipeFragment extends Fragment {

    private int mColumnCount = 1;
    private static final String RPC_QUEUE_NAME = "home_queue";
    private static final String IP_ADDRESS  = "172.30.121.246";
    private OnListFragmentInteractionListener mListener;
//    private static final String IP_ADDRESS  = "192.168.0.42";
    final private ArrayList<ListItem> items = new ArrayList<>();
    RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecipeFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RecipeFragment newInstance() {
        RecipeFragment fragment = new RecipeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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

                    String message = "{\"type\":\"recipes\"}"; // This is the message I send to Madhav
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
                    updateGui(response);

                    channel.close();
                    connection.close();
                } catch (IOException | TimeoutException | InterruptedException ex) {
                    Log.e("Rabbit Mq ERROR", "I broke it!");
                    ex.printStackTrace();
                }
            }
        }).start();

    }

    public void updateGui(String response) {
        Log.d("Rabbit MQ", "Response: " + response);

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new StringReader(response));
        reader.setLenient(true);
        ArrayList<Object> attempt = gson.fromJson(response, ArrayList.class);
        items.clear();
        for (Object o : attempt) {
            Recipe r = gson.fromJson(gson.toJson(o), Recipe.class);
            ListItem list = new ListItem(r.title, r.image);
            list.id = Integer.toString(r.id);
            items.add(list);
        }

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try{
                    adapter.notifyDataSetChanged();
                }
                catch (Exception e) {

                }
            }
        });
    }
    MyListViewAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

//            ArrayList<ListItem> items = new ArrayList<ListItem>(
//                    Arrays.asList(new ListItem("Teriyaki Chicken", "http://redirect.bigoven.com/pics/rs/640/chicken-teriyaki-10.jpg"),
//                            new ListItem("Lasagna", "https://photos.bigoven.com/avatar/photo/wurstel.jpg"),
//                            new ListItem("Teriyaki Chicken", "http://redirect.bigoven.com/pics/rs/640/chicken-teriyaki-10.jpg")));

             adapter = new MyListViewAdapter(getActivity(), items, mListener);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
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
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(String recipeId, String url);
    }
}
