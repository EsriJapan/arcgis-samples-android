package com.arcgis.esrij.comfortabletap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.android.action.IdentifyResultSpinner;
import com.esri.android.action.IdentifyResultSpinnerAdapter;
import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.identify.IdentifyParameters;
import com.esri.core.tasks.identify.IdentifyResult;
import com.esri.core.tasks.identify.IdentifyTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    MapView mapView;
    IdentifyParameters identifyParameters = null;
    GraphicsLayer highlightGraphicsLayer;
    static ProgressDialog dialog;
    String taskURL = "http://sampleserver5.arcgisonline.com/arcgis/rest/services/Recreation/MapServer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // レイアウトファイルで指定した地図の表示
        mapView = (MapView)findViewById(R.id.map);

        // 拡大鏡の有効化
        mapView.setShowMagnifierOnLongPress(true);

        // 拡大を表示した状態で地図の移動を許可
        mapView.setAllowMagnifierToPanMap(true);

       // ダイナミック マップ サービスの表示
        mapView.addLayer(new ArcGISDynamicMapServiceLayer(taskURL));

        // 選択したポイントをハイライトする グラフィック レイヤ
        highlightGraphicsLayer = new GraphicsLayer();
        mapView.addLayer(highlightGraphicsLayer);

        // シングルタップでポイントを選択する
        mapView.setOnSingleTapListener(new OnSingleTapListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSingleTap(final float x, final float y) {

            if (!mapView.isLoaded()) {
                return;
            }

            EditText editText = (EditText) findViewById(R.id.edit_text);
            // 入力テキストを取得
            String inputText = editText.getText().toString();
            // 取得した文字列を数値型に変換
            int tolerance = Integer.valueOf(inputText).intValue();

            // Identify タスクの入力パラメータの設定
            identifyParameters = new IdentifyParameters();
            // タップの許容値を設定
            identifyParameters.setTolerance(tolerance);
            identifyParameters.setDPI(98);
            identifyParameters.setLayers(new int[]{0});
            identifyParameters.setLayerMode(IdentifyParameters.ALL_LAYERS);

            Point identifyPoint = mapView.toMapPoint(x, y);
            SpatialReference sr = mapView.getSpatialReference();

            identifyParameters.setGeometry(identifyPoint);
            identifyParameters.setSpatialReference(sr);
            identifyParameters.setMapHeight(mapView.getHeight());
            identifyParameters.setMapWidth(mapView.getWidth());
            identifyParameters.setReturnGeometry(true);

            Envelope env = new Envelope();
            mapView.getExtent().queryEnvelope(env);
            identifyParameters.setMapExtent(env);

            // Identify タスクを実行
            MyIdentifyTask mTask = new MyIdentifyTask(identifyPoint);
            mTask.execute(identifyParameters);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ViewGroup createIdentifyContent(final List<IdentifyResult> results) {

        // create a new LinearLayout in application context
        LinearLayout layout = new LinearLayout(this);

        // view height and widthwrap content
        layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        // default orientation
        layout.setOrientation(LinearLayout.HORIZONTAL);

        // Spinner to hold the results of an identify operation
        IdentifyResultSpinner spinner = new IdentifyResultSpinner(this, results);

        // スピナーをクリック可能にする
        spinner.setClickable(true);
        // スピナーの水平方向のスクロール
        spinner.canScrollHorizontally(BIND_ADJUST_WITH_ACTIVITY);

        // MyIdentifyAdapter creates a bridge between spinner and it's data
        MyIdentifyAdapter adapter = new MyIdentifyAdapter(this, results);
        spinner.setAdapter(adapter);
        spinner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        layout.addView(spinner);

        return layout;
    }

    public class MyIdentifyAdapter extends IdentifyResultSpinnerAdapter {
        List<IdentifyResult> resultList;
        Context m_context;

        public MyIdentifyAdapter(Context context, List<IdentifyResult> results) {
            super(context, results);
            this.resultList = results;
            this.m_context = context;
        }

        // Get a TextView that displays identify results in the callout.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String LSP = System.getProperty("line.separator");
            StringBuilder outputVal = new StringBuilder();

            // Resource Object to access the Resource fields
            Resources res = getResources();

            // Get Name attribute from identify results
            IdentifyResult curResult = this.resultList.get(position);

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.FACILITY))) {
                outputVal.append("設備　 ： "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.FACILITY)).toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.DESCRIPTION))) {
                outputVal.append("説明　 ： "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.DESCRIPTION)).toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.QUALITY))) {
                outputVal.append("品質　 ： "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.QUALITY))
                        .toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.OBSERVED))) {
                outputVal.append("記録日 ： "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.OBSERVED))
                        .toString());
                outputVal.append(LSP);
            }

            // Create a TextView to write identify results
            TextView txtView;
            txtView = new TextView(this.m_context);
            txtView.setText(outputVal);
            txtView.setTextColor(Color.BLACK);
            txtView.setLayoutParams(new ListView.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            txtView.setGravity(Gravity.CENTER_VERTICAL);

            return txtView;
        }
    }

    private class MyIdentifyTask extends
            AsyncTask<IdentifyParameters, Void, IdentifyResult[]> {

        IdentifyTask task = new IdentifyTask(taskURL);

        IdentifyResult[] M_Result;

        Point mAnchor;

        MyIdentifyTask(Point anchorPoint) {
            mAnchor = anchorPoint;
        }

        @Override
        protected void onPreExecute() {
            // create dialog while working off UI thread
            dialog = ProgressDialog.show(MainActivity.this, "Identify タスク",
                    "検索しています...");
        }

        protected IdentifyResult[] doInBackground(IdentifyParameters... params) {

            // check that you have the identify parameters
            if (params != null && params.length > 0) {
                IdentifyParameters mParams = params[0];

                try {
                    // Run IdentifyTask with Identify Parameters
                    M_Result = task.execute(mParams);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return M_Result;
        }

        @Override
        protected void onPostExecute(IdentifyResult[] results) {

            highlightGraphicsLayer.removeAll();

            // dismiss dialog
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            ArrayList<IdentifyResult> resultList = new ArrayList<IdentifyResult>();
            IdentifyResult result_1 = null;

            // 選択したフィーチャをハイライトするシンボルを定義する
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.argb(100, 255, 20, 147), 50, SimpleMarkerSymbol.STYLE.CIRCLE);

            for (int index = 0; index < results.length; index++) {

                result_1 = results[index];
                String displayFieldName = result_1.getDisplayFieldName();
                Map<String, Object> attr = result_1.getAttributes();
                for (String key : attr.keySet()) {
                    if (key.equalsIgnoreCase(displayFieldName)) {
                        resultList.add(result_1);
                    }
                }
                // 選択したフィーチャをハイライト
                Graphic graphic = new Graphic(result_1.getGeometry(), sms);
                highlightGraphicsLayer.addGraphic(graphic);
            }

            Callout callout = mapView.getCallout();
            callout.setContent(createIdentifyContent(resultList));
            if(0 < results.length){
                callout.show(mAnchor);
            }else{
                callout.hide();
            }
        }
    }
}
