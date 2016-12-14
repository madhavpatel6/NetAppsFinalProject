package com.example.rachael.digitalpantry;


import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SingleRecipeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SingleRecipeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SingleRecipeFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static final String RPC_QUEUE_NAME = "home_queue";
    private static final String IP_ADDRESS  = "172.30.121.246";
//    private static final String IP_ADDRESS  = "192.168.0.42";
    public static final String RECIPE_ID = "RECIPE_ID";
    public static final String RECIPE_URL = "IMAGE_URL";
    public static String recipe_id;
    private static String recipe_url;
    public SingleRecipeFragment() {
        // Required empty public constructor
    }

    public static SingleRecipeFragment newInstance(String recipe_id, String recipe_url) {
        SingleRecipeFragment fragment = new SingleRecipeFragment();
        Bundle args = new Bundle();
        args.putString(RECIPE_ID, recipe_id);
        args.putString(RECIPE_URL, recipe_url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getArguments() != null) {
            recipe_id = getArguments().getString(RECIPE_ID);
            recipe_url = getArguments().getString(RECIPE_URL);
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

                    String message = "{\"type\":\"recipe\",\"data\":[{\"id\":\""+ recipe_id +"\"}]}"; // This is the message I send to Madhav
                    Log.d("Rabbit MQ", "Message :" + message);
                    channel.basicPublish("", RPC_QUEUE_NAME, props, message.getBytes());

                    while (true) {
                        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                        if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                            response = new String(delivery.getBody());
                            break;
                        }
                    }

                    Log.d("Rabbit MQ", "Response: " + response);
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

    ImageView image;
    TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_single_recipe, container, false);
        textView = (TextView) view.findViewById(R.id.recipe_details);
        image = (ImageView) view.findViewById(R.id.recipe_image);

        new DownloadImageTask(getActivity(), image).execute(recipe_url);
        textView.setText("Recipe Instructions Here");
        return view;
    }

    public void updateGui(String resonse) {
        // Update GUI here from response
        Gson gson = new Gson();
        final ArrayList<String> steps = gson.fromJson(resonse, ArrayList.class);

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try{
                    textView.setText("");
                    for (String step : steps) {
                        textView.append(step + "\n");
                    }
                }
                catch (Exception e) {

                }
            }
        });

    }

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
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}