package com.arcgis.esrij.RuntimeSDKSample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListSampleActivity extends Activity {

    protected ListView listView;
    protected TextView titleText;
    private String[] names = {"ベースマップ",
                                "位置情報表示モード",
                                };
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listsample);
        if (savedInstanceState == null) {
            listView = (ListView) findViewById(R.id.sample_list);
            titleText = (TextView) findViewById(R.id.title_sample_list_text);
        }
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch(position) {
                    case 0:
                        Intent intent = new Intent(ListSampleActivity.this, BasemapActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        Intent intent2 = new Intent(ListSampleActivity.this, AutoPanModeActivity.class);
                        startActivity(intent2);
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        });
    }
}