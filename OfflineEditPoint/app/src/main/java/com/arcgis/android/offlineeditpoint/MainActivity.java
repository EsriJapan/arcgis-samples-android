/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.arcgis.android.offlineeditpoint;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.SyncLayerResult;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Esriジャパン Android Sample
 * OfflineEditPoint
 * ArcGIS Runtime SDK for Android version:100.0
 * */
public class MainActivity extends AppCompatActivity {

    /** map */
    private MapView mMapView;
    private ArcGISMap mMap;
    /** 吹き出し */
    private Callout mCallout;

    /** ローカルジオデータベース(*.geodatabase)関連 */
    static GeodatabaseSyncTask geodatabaseSyncTask;
    static GenerateGeodatabaseParameters generateParams;
    static GenerateGeodatabaseJob generateJob;
    static Geodatabase geodatabase;
    private static String mGeodatabasePath;

    /** for update server prarams */
    static SyncGeodatabaseParameters syncParams;
    static SyncGeodatabaseJob syncJob;

    /** タップされた回数をカウントする */
    static int mCalloutCnt = 0;
    /** debug用 */
    private String TAG = "esrij_sample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ArcGISの地図のレイアウトを生成する
        mMapView = (MapView) findViewById(R.id.mapView);
        // AGOL(ArcGIS Online) のベースマップ(topographic)を読み込む
        mMap = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 35.3312442,139.6202471,  13);
        // 読み込んだArcGISMapオブジェクトをMapViewに設定する
        mMapView.setMap(mMap);
        // Android 端末内のgeodatabaseファイル作成パスを取得する
        getGeodatabasePath();
        // ローカルgeodatabase 作成
        // ※作成のためのパスはあらかじめ端末内に作成しておく
        setupAgolLayer2geodatabase();

        // 画面をタッチしたときに発生するイベントを設定する
        // タッチ→吹き出し表示イベントを実装する
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                Log.d(TAG, "onSingleTapConfirmed: " + motionEvent.toString());

                // タッチしたポイントを座標に変換する
                android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
                        Math.round(motionEvent.getY()));
                // 変換した座標からジオメトリ(point)を作成する
                Point mapPoint = mMapView.screenToLocation(screenPoint);
                // ポイントの座標変換
                Point wgs84Point = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
                // 吹き出し表示のためのtextviewオブジェクトを作成する
                TextView calloutContent = new TextView(getApplicationContext());
                calloutContent.setTextColor(Color.BLACK);
                calloutContent.setSingleLine();
                // 座標の表示は小数点以下4桁になるようにフォーマットする
                calloutContent.setText("Lat: " +  String.format("%.4f", wgs84Point.getY()) +
                        ", Lon: " + String.format("%.4f", wgs84Point.getX()));
                // 吹き出し表示オブジェクトに作成した内容を設定する
                mCallout = mMapView.getCallout();
                mCallout.setLocation(mapPoint);
                mCallout.setContent(calloutContent);
                // 表示する
                mCallout.show();
                // タップしたところを地図のセンターにする
                mMapView.setViewpointCenterAsync(mapPoint);
                // ローカルgeodatabaseへタップしたポイントの情報を追加する
                addFeature(wgs84Point);
                // カウントして一定の数()になったらレイヤーを更新する
                countCalloutTap();
                return true;
            }
        });
    }

    /**
     * geodatabaseファイル作成のパスを取得する
     * */
    private void getGeodatabasePath(){

        // Android 外部領域パス取得
        // cf:http://blog.lciel.jp/blog/2014/02/08/android-about-storage/
        String extern = Environment.getExternalStorageDirectory().getPath();
        mGeodatabasePath  = extern + getResources().getString(R.string.local_geodatabase);

    }

    /**
     * ローカルgeodatabaseにポイントを追加する
     * */
    private void addFeature(Point pPoint){

        if (!mGdbFeatureTable.canAdd()) {
            // Deal with indicated error
            return;
        }
        java.util.Map<String, Object> attributes = new HashMap<String, Object>();
        Feature addedFeature = mGdbFeatureTable.createFeature(attributes, pPoint);

        final ListenableFuture<Void> addFeatureFuture = mGdbFeatureTable.addFeatureAsync(addedFeature);
        addFeatureFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // ポイント追加の成功をチェックする:エラーがあればexceptionをthrowする
                    addFeatureFuture.get();
                    Toast.makeText(getApplicationContext(), "add point geodatabase", Toast.LENGTH_SHORT).show();

                } catch (InterruptedException | ExecutionException e) {
                    // executionException may contain an ArcGISRuntimeException with edit error information.
                    if (e.getCause() instanceof ArcGISRuntimeException) {
                        ArcGISRuntimeException agsEx = (ArcGISRuntimeException)e.getCause();
                        Log.e(TAG, agsEx.toString());
                    } else {
                        Log.e(TAG, "other error");
                    }
                }
            }
        });
    }

    /**
     * 画面をタップした回数をカウントする
     * 3回タップするとAGOLLayerと同期を開始する
     * */
    private void countCalloutTap(){
        mCalloutCnt++;
        Toast.makeText(this, "count " + String.valueOf(mCalloutCnt), Toast.LENGTH_SHORT).show();

        if(mCalloutCnt > 2 ){
            // カウントとリセット
            mCalloutCnt = 0;
            //任意の場所を'mCalloutCnt'回タップしたら、それまでのポイントをAGOLLayerと同期する
            createSyncParameters();
        }
    }

    /**
     * ローカルgeodatabaseを作成する
     * ※すでに存在する場合は、既存のgeodatabaseファイルから読みこんでlayerを表示する
     * */
    private void setupAgolLayer2geodatabase(){
        // create a basemap from a local tile package
        Log.i(TAG, "GDB File Path:"+ mGeodatabasePath + ":" + isGeoDatabaseLocal());
        File file = new File(mGeodatabasePath);
        if(!file.exists()){
            // ローカルgeodatabaseが存在しない場合、新規作成する
            createGeodatabaseSyncTask();
        }else{
            // 存在する場合は、既存のgeodatabaseから読み込む
            readGeoDatabase();
        }
    }
    /**
     * ローカルgeodatabaseの存在チェック
     */
    public static boolean isGeoDatabaseLocal() {
        File file = new File(mGeodatabasePath);
        return file.exists();
    }

    /**
     * GeoDatabaseを新規に作成する
     * ① 同期させたいArcGIS Online の Feature Layer でタスクを作成する
     * ***/
    private void createGeodatabaseSyncTask() {

        // 同期したいレイヤーでgeodatabase作成タスクオブジェクトを作成する
        geodatabaseSyncTask = new GeodatabaseSyncTask(getResources().getString(R.string.org_agolfFeatureLayer));
        geodatabaseSyncTask.addDoneLoadingListener(new Runnable() {
            @Override public void run() {
                // ロードのステータスを検査する
                if (geodatabaseSyncTask.getLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
                    Log.e(TAG,geodatabaseSyncTask.getLoadError().toString());
                } else {
                    // Load に成功
                    // AGOLからローカルgeodatabase作成のためのパラメータを取得する
                    generateGeodatabaseParameters();
                }
            }
        });
        // タスクのロードを開始する
        geodatabaseSyncTask.loadAsync();
    }

    /**
     * GeoDatabaseを新規に作成する
     * ② 同期させたいArcGIS Online の Feature Layer のパラメータを取得する
     * */
    private void generateGeodatabaseParameters() {

        // geodatabase 作成のためのパラメータを取得する
        Envelope generateExtent = mMapView.getVisibleArea().getExtent();
        final ListenableFuture<GenerateGeodatabaseParameters> generateParamsFuture =
                geodatabaseSyncTask.createDefaultGenerateGeodatabaseParametersAsync(generateExtent);
        generateParamsFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    generateParams = generateParamsFuture.get();
                    // ローカルgeodatabaseを作成する
                    generateGeodatabase();
                }
                catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * GeoDatabaseを新規に作成する
     * ③ 同期させたいArcGIS Online の Feature Layer でローカル geodatabase を作成する
     * */
    private void generateGeodatabase() {

        // geodatabaseファイル作成ジョブオブヘジェクトを作成する
        generateJob = geodatabaseSyncTask.generateGeodatabaseAsync(generateParams, mGeodatabasePath);

        // データダウンロードのステータスをチェックする
        generateJob.addJobChangedListener(new Runnable() {
            @Override
            public void run() {

                // 作成中のステータスをチェックする
                if (generateJob.getError() != null) {
                    Log.e(TAG,generateJob.getError().toString());
                } else {
                    // ダウンロードの進行状況：メッセージを確認したり、ログやユーザーインターフェイスで進行状況を更新します
                }
            }
        });

        // ダウンロードとgeodatabaseファイル作成が終了したときのステータスを取得します
        generateJob.addJobDoneListener(new Runnable() {
            @Override
            public void run() {

                // 作成ジョブが終了したときのステータスを検査する
                String status = generateJob.getStatus().toString();
                if ((generateJob.getStatus() != Job.Status.SUCCEEDED) || (generateJob.getError() != null)) {
                    Log.e(TAG,generateJob.getError().toString());
                } else {
                    // 作成完了から返された値を取得する
                    if (generateJob.getResult() instanceof Geodatabase) {
                        Geodatabase syncResultGdb = (Geodatabase) generateJob.getResult();
                        geodatabase = syncResultGdb;
                        // 作成したgeodatabaseからフィーチャ レイヤーを表示する
                        readGeoDatabase();
                    }
                }
            }
        });
        // geodatabase 作成のジョブを開始します
        generateJob.start();
    }

    /**
     * 既存GeoDatabaseから読み込む
     * ***/
    GeodatabaseFeatureTable mGdbFeatureTable;
    private void readGeoDatabase(){

        geodatabase = new Geodatabase(mGeodatabasePath);
        geodatabase.loadAsync();
        geodatabase.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {

                // geodatabaseの読込 地図追加
                if(geodatabase.getLoadStatus() == LoadStatus.LOADED && geodatabase.getGeodatabaseFeatureTables().size() > 0){

                    int cnt = 0;
                    // 今回読み込むレイヤーは１つ=0
                    mGdbFeatureTable = geodatabase.getGeodatabaseFeatureTables().get(0);
                    try{
                        FeatureLayer featureLayer = new FeatureLayer(mGdbFeatureTable);
                        featureLayer.setLabelsEnabled(true);
                        mMap.getOperationalLayers().add(featureLayer);
                        cnt++;
                    }catch (Exception e){
                        e.printStackTrace();
                        String message = e.getMessage();
                    }
                }else{
                    Log.e(TAG, geodatabase.getLoadStatus() + "/ FeatureTables.size=" + geodatabase.getGeodatabaseFeatureTables().size());
                }
            }
        });
    }

    /**
     * サーバー(AGOL)と同期する
     * ① 同期タスクを作成する
     * ② 同期パラメータを取得する
     * */
    private void createSyncParameters() {

        // 同期したいレイヤーでタスクオブジェクトを作成する
        geodatabaseSyncTask = new GeodatabaseSyncTask(getResources().getString(R.string.org_agolfFeatureLayer));

        // タスクオブジェクトから同期するためのパラメータを作成する
        final ListenableFuture<SyncGeodatabaseParameters> syncParamsFuture = geodatabaseSyncTask.createDefaultSyncGeodatabaseParametersAsync(geodatabase);
        syncParamsFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try
                {
                    // パラメータを取得
                    syncParams = syncParamsFuture.get();
                    // パラーメータを使用してgeodatabaseを同期する
                    syncGeodatabase();
                }
                catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * サーバー(AGOL)と同期する
     * ③ 同期ジョブを作成する
     * ④ 同期する
     * */
    private void syncGeodatabase() {

        // 同期ジョブオブヘジェクトを作成する
        syncJob = geodatabaseSyncTask.syncGeodatabaseAsync(syncParams, geodatabase);

        // 同期中のステータスをチェックする
        syncJob.addJobChangedListener(new Runnable() {
            @Override
            public void run() {
                if (syncJob.getError() != null) {
                    // 同期中にエラーがある場合
                    Log.e(TAG,syncJob.getError().toString());
                } else {
                    // 同期の進行状況：メッセージを確認したり、ログやユーザーインターフェイスで進行状況を更新します
                }
            }
        });

        // 同期が終了したときのステータスを取得します
        syncJob.addJobDoneListener(new Runnable() {
            @Override
            public void run() {
                // 同期ジョブが終了したときのステータスを検査する
                if ((syncJob.getStatus() != Job.Status.SUCCEEDED) || (syncJob.getError() != null)) {
                    // エラーの場合
                    Log.e(TAG,syncJob.getError().toString());
                } else {
                    // 同期完了から返された値を取得する
                    List<SyncLayerResult> syncResults = (List<SyncLayerResult>) syncJob.getResult();
                    if (syncResults != null) {
                        // 同期結果を確認して、例えばユーザに通知する処理を作成します
                        Toast.makeText(getApplicationContext(), "sync end" , Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        // geodatabase 同期のジョブを開始します
        syncJob.start();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }
}
