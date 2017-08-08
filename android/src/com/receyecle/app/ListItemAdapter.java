package com.receyecle.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by perrasr on 2/21/17.
 */

public class ListItemAdapter extends ArrayAdapter<ListItem> {

    private LayoutInflater mInflater;
    List<ListItem> lista;

    public ListItemAdapter(Context context, int rid, List<ListItem> list){
        super(context, rid, list);
        lista = list;
        mInflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent){

        // Retrieve data
        final ListItem item = getItem(position);

        // Use layout file to generate View
        View view = mInflater.inflate(R.layout.list_results, null);

        // Set number
        TextView num;
        num = (TextView)view.findViewById(R.id.number);
        num.setText(item.number);

        // Set classifier
        TextView classifier;
        classifier = (TextView) view.findViewById(R.id.classifier);
        classifier.setText(item.classifier);
        classifier.setTag(position);


        return view;
    }
}
