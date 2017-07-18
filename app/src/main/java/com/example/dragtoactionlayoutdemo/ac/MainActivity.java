package com.example.dragtoactionlayoutdemo.ac;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.dragtoactionlayoutdemo.R;
import com.example.dragtoactionlayoutdemo.adapter.ListApater;
import com.example.dragtoactionlayoutdemo.view.DragToActionLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    private void initView() {
        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ListApater la=new ListApater(this);
        rv.setAdapter(la);


        DragToActionLayout mRefreshLayout = (DragToActionLayout) findViewById(R.id.refresh_widget);
        mRefreshLayout.setOnDragListener(new DragToActionLayout.OnDragListener() {
            @Override
            public void onFinish() {
                startActivity(new Intent(MainActivity.this, Main2Activity.class));
            }

            @Override
            public void onDragDistanceChange(float distance, float percent, float offset) {

            }
        });
    }
}
