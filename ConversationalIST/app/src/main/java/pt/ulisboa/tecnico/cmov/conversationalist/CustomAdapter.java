package pt.ulisboa.tecnico.cmov.conversationalist;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomAdapter extends BaseAdapter {

    private Context context;
    private List<Map<String, String>> data;

    public CustomAdapter(Context context, List<Map<String, String>> data) {
        this.data = data;
        this.context=context;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.list_item, null);
            holder.txtRef = (TextView) view.findViewById(R.id.txtRef);
            holder.txtOf = (TextView) view.findViewById(R.id.txtOf);

            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }

        holder.txtRef.setText(data.get(position).get("name"));
        holder.txtOf.setText(data.get(position).get("type"));

        int flag = 0;

        try{
            data.get(position).get("inRadius").equals("0");
        } catch (Exception e){
            flag = 1;
        }

        if (flag == 0){
            if (data.get(position).get("inRadius").equals("0")) {
                holder.txtRef.setTextColor(Color.GRAY);
                holder.txtOf.setTextColor(Color.GRAY);
            }
            else {
                holder.txtRef.setTextColor(Color.BLACK);
                holder.txtOf.setTextColor(Color.BLACK);
            }
        }

        if (data.get(position).get("read").equals("0")) {
            holder.txtRef.setTypeface(null, Typeface.BOLD);
            holder.txtOf.setTypeface(null, Typeface.BOLD);
        } else {
            holder.txtRef.setTypeface(null, Typeface.NORMAL);
            holder.txtOf.setTypeface(null, Typeface.NORMAL);
        }
        return view;

    }

    public void updateList(List<Map<String, String>> newlist) {
        data.clear();
        data.addAll(newlist);
        this.notifyDataSetChanged();
    }

    class ViewHolder{
        TextView txtRef;
        TextView txtOf;
    }

}