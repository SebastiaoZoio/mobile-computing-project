package pt.ulisboa.tecnico.cmov.conversationalist;

import android.graphics.Bitmap;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BasicMessage {

    private String sender;
    private String content = null;
    private List<Pair<String, Integer>> poll = null;
    private LocalDateTime time;
    private Bitmap image;
    private Boolean isImage = false;
    private LatLng coordinates;
    private boolean isMap = false;
    private boolean isPoll = false;
    private int id = 0;

    public BasicMessage(String sender, String content, LocalDateTime time, int id) {
        this.sender = sender;
        this.content = content;
        this.time = time;
        this.id = id;
    }

    public BasicMessage(String sender, Bitmap image, LocalDateTime time, int id) {
        this.sender = sender;
        this.image = image;
        this.time = time;
        this.isImage = true;
        this.id = id;
    }

    public BasicMessage(String sender, LatLng coordinates, LocalDateTime time, int id){
        this.sender = sender;
        this.time = time;
        this.coordinates = coordinates;
        this.isMap = true;
        this.id = id;
    }

    public BasicMessage(String sender, List<Pair<String, Integer>> poll, LocalDateTime time, int id) {
        this.sender = sender;
        this.time = time;
        this.poll = poll;
        this.isPoll = true;
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }


    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Bitmap getImage() {
        return image;
    }

    public List<Pair<String, Integer>> getPoll() {
        return poll;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Boolean isImage(){
        return this.isImage;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isMap() {
        return isMap;
    }

    public boolean isPoll() { return isPoll; }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString()
    {
        return "Sender: " + this.getSender() + " - Content: " + this.getContent();
    }
}
