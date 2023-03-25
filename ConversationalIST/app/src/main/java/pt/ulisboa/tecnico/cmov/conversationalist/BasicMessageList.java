package pt.ulisboa.tecnico.cmov.conversationalist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import pt.ulisboa.tecnico.cmov.conversationalist.BasicMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class BasicMessageList {

    private List<BasicMessage> messageList;

    public void setMessageList(List<String> senders, List<String> contents, List<String> Times, List<Integer>is_photoList, List<Integer>is_mapList, List<Integer>is_pollList, List<Integer>idsList){
        List<BasicMessage> messageList = new ArrayList<BasicMessage>();

        for (int i= 0; i < senders.size(); i++){
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String time = Times.get(i);
            LocalDateTime dt =  LocalDateTime.parse(time, dtf);

            if (is_photoList.get(i) == 0 && is_mapList.get(i) == 0 && is_pollList.get(i) == 0) {
                messageList.add(new BasicMessage(senders.get(i), contents.get(i), dt, idsList.get(i)));
            }
            else if(is_mapList.get(i) == 1){
                String[] coors = contents.get(i).split(",");
                LatLng coordinates = new LatLng(Double.parseDouble(coors[0]), Double.parseDouble(coors[1]));
                Log.d("qPassa2", ""+coordinates);
                messageList.add(new BasicMessage(senders.get(i), coordinates, dt, idsList.get(i)));
            }
            else if(is_pollList.get(i) == 1){
                Log.d("whatPoll", "aqui");
                List<Pair<String, Integer>> poll = new ArrayList<Pair<String, Integer>>();
                String[] results = contents.get(i).split(",");
                for (String result: results){
                    String[] resultPair = result.split(":");
                    Pair<String, Integer> pair = new Pair<String, Integer>(resultPair[0], Integer.parseInt(resultPair[1]));
                    poll.add(pair);
                }
                messageList.add(new BasicMessage(senders.get(i), poll, dt, idsList.get(i)));
            }
            else{
                String encodedImage = contents.get(i);
                Bitmap decodedByte = null;
                if (encodedImage.equals( "placeholder")){

                    Bitmap none = null;
                    BasicMessage bm = new BasicMessage(senders.get(i), none, dt, idsList.get(i));
                    messageList.add(bm);
                }
                else{
                    byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                    decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    messageList.add(new BasicMessage(senders.get(i), decodedByte, dt, idsList.get(i)));
                }


            }

        }

        this.messageList = messageList;
    }

    public List<BasicMessage> getMessageList() {
        return messageList;
    }
}
