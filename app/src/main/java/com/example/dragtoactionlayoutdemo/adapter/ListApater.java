package com.example.dragtoactionlayoutdemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v7.widget.RecyclerView;
import com.example.dragtoactionlayoutdemo.R;

/**
 * Created by rebecca on 17/6/16.
 */

public class ListApater extends RecyclerView.Adapter<ListApater.ListHolder> {

    String names[] = {"Cloud", "Movie", "Laptop", "Loop", "Menu", "Mood", "Palette", "Search", "Time", "Work"};
    Context context;

    public ListApater(Context context) {
        this.context = context;
    }




    @Override
    public ListHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(  R.layout.list_item_h, parent, false);
        return new ListHolder(view);
    }
    @Override
    public void onBindViewHolder(ListHolder holder, int position) {
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return 10;
    }


    class ListHolder extends RecyclerView.ViewHolder {
        TextView subName;

        public ListHolder(View itemView) {
            super(itemView);
            subName = (TextView) itemView.findViewById(R.id.subname);
        }

        public void setData(int position) {
            subName.setText("This is position " + position);
        }


    }
}