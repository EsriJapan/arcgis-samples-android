package com.arcgis.android.gcf2017;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.ArcGISFeatureTable;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.ArcGISVectorTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.raster.Raster;
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
 * EsriJapan 2017
 * GIS Communuty Forum : Offline Session Demo App
 * Created by wakanasato on 2017/05/01.
 * */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /** map */
    private MapView mMapView;
    private ArcGISMap mMap;

    /** ローカルジオデータベース(*.geodatabase)関連 */
    static GeodatabaseSyncTask geodatabaseSyncTask;
    static GenerateGeodatabaseParameters generateParams;
    static GenerateGeodatabaseJob generateJob;
    static Geodatabase geodatabase;
    static String mGeodatabasePath;

    /** for update server prarams */
    static SyncGeodatabaseParameters syncParams;

    /** 吹き出し */
    private Callout mCallout;
    private static android.graphics.Point mSreenPoint;

    /** 編集フラグ */
    static private boolean editFlg= false;

    /** Android スライドメニュー用 */
    private static HashMap<Integer,Integer> LNum;
    private void init(){
        LNum = new HashMap<>();
        LNum.put(0,R.id.RLayer);
        LNum.put(1,R.id.tpk);
        LNum.put(2,R.id.vtpk);
        LNum.put(3,R.id.FLayer);
    }

    /** debug用 */
    private String TAG = "esrij_sample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /////////////////////////////////////
        // 地図表示処理
        /////////////////////////////////////
        // ArcGISの地図のレイアウトを生成する
        mMapView = (MapView) findViewById(R.id.mapView);
        // AGOL(ArcGIS Online) のベースマップ(topographic)を読み込む
        mMap = new ArcGISMap(Basemap.Type.LIGHT_GRAY_CANVAS, 35.3312442,139.6202471,  10);

        ////////////////////////////////////////
        // Android 端末内のgeodatabaseファイル作成パスを取得する
        ////////////////////////////////////////
        getGeodatabasePath();
        init();

        // ローカルファイルを読み込んで、mapviewに追加しておく。visibly=falseにしておく
        setPkges();
        chkGeodatabase();

        mCallout = mMapView.getCallout();
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                    Log.d(TAG, "setOnTouchListener: " + motionEvent.toString());

                // タッチされたポイントを作成
                mSreenPoint = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));

                if(editFlg){
                    // 追加モードならタッチ場所を追加する
                    addPoint(mSreenPoint);
                }else{
                    // ルート・閲覧・編集モードならデータを表示する
                    displayPoint(mSreenPoint);
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                Log.d(TAG, "setOnLongClickListener: " + e.toString());

                if(editFlg){
                    // タッチされたポイントを作成
                    android.graphics.Point longtapPoint = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));
                    // 削除処理をコールする
                    deletePoint(longtapPoint);
                }
            }
        });

        //////////////////////////////////////////////////
        // 同期ボタン実装
        //////////////////////////////////////////////////
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // switchのステータスをみて同期するか決める
                if(editFlg){
                    // 同期の処理を呼ぶ
                    Snackbar.make(view, "同期処理を開始しました", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    createSyncParameters();
                }else{
                    // なにもしない
                    Snackbar.make(view, "編集スイッチをonにしてください", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                Toast.makeText(getApplicationContext(), "closed!" , Toast.LENGTH_SHORT).show();

                // 編集フラグ更新
                Switch editSwitch = (Switch)findViewById(R.id.editSwitch);
                editFlg = editSwitch.isChecked();
                // ウィンドウをclose したときに更新する
                // 2:tpklayer,3:vtpklayer,4:raster,1:flayer
                for(int i=0; i < LNum.size() ;i++){
                    ChkCheckbox(i);
                }
            }

        };

        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // 読み込んだArcGISMapオブジェクトをMapViewに設定する
        mMapView.setMap(mMap);

    }

    /**
     * チェックボックスの制御
     * */
    private void ChkCheckbox(int pLayerNumber){

        CheckBox chkbox = (CheckBox)findViewById(LNum.get(pLayerNumber));
        if(mMap.getOperationalLayers().get(pLayerNumber) != null){
            // ローカルからのデータがある場合表示/非表示を切り替える
            if(chkbox.isChecked()){
                mMap.getOperationalLayers().get(pLayerNumber).setVisible(true);
            }else{
                mMap.getOperationalLayers().get(pLayerNumber).setVisible(false);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    // 表示
    /////////////////////////////////////////////////////////////////////
    /**
     * ポイント情報を表示する
     *
     * */
    private static ArcGISFeature mSelectedFeature;
    private void displayPoint(android.graphics.Point pScreenPoint){

        final ListenableFuture<IdentifyLayerResult> future = mMapView.identifyLayerAsync(mFeatureLayer, pScreenPoint, 5, false);

        // add done loading listener to fire when the selection returns
        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    IdentifyLayerResult result = future.get();
                    List<GeoElement> resultGeoElements = result.getElements();
                    if (resultGeoElements.size() > 0) {
                        if (resultGeoElements.get(0) instanceof ArcGISFeature) {

                            // 選択されたジオメトリがわかる
                            mSelectedFeature = null;
                            mSelectedFeature = (ArcGISFeature) resultGeoElements.get(0);
                            String selectedId = mSelectedFeature.getAttributes().get("name").toString();
                            showCallout(selectedId);

                        }
                    }
                    else {
                        // 何も選択されない場所をタップしときcalloutを閉じる
                        mCallout.dismiss();
                    }
                } catch (Exception e) {
                    Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 吹き出しの表示
     */
    private void showCallout(String title){

        // create a text view for the callout
        RelativeLayout calloutLayout = new RelativeLayout(getApplicationContext());

        TextView calloutContent = new TextView(getApplicationContext());
        calloutContent.setId(R.id.calloutTextView);
        calloutContent.setTextColor(Color.BLACK);
        calloutContent.setTextSize(18);
        calloutContent.setPadding(0,10,10,0);

        calloutContent.setText(title);

        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        relativeParams.addRule(RelativeLayout.RIGHT_OF, calloutContent.getId());

        calloutLayout.addView(calloutContent);

        try {
            mCallout.setLocation(mMapView.screenToLocation(mSreenPoint));
            mCallout.setContent(calloutLayout);
            mCallout.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////
    // 追加
    ////////////////////////////////////////////////////////////////
    /**
     * 新しいポイントを追加する
     * From touch eventから
     * */
    private void addPoint(android.graphics.Point pScreenPoint){

        // 変換した座標からジオメトリ(point)を作成する
        Point mapPoint = mMapView.screenToLocation(pScreenPoint);
        // ポイントの座標変換
        Point wgs84Point = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
        addFeature(wgs84Point);
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
        // 項目にデータを入れる
        attributes.put("name","ESRIジャパンnow！");
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
    ////////////////////////////////////////////////////////////////
    // 削除
    ////////////////////////////////////////////////////////////////
    /**
     * ローカルジオデータベースからポイントを削除する
     * */
    private void deletePoint(android.graphics.Point pLongtapPoint){

        final ListenableFuture<IdentifyLayerResult> future = mMapView.identifyLayerAsync(mFeatureLayer, pLongtapPoint, 5, false);

        // add done loading listener to fire when the selection returns
        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    IdentifyLayerResult result = future.get();
                    List<GeoElement> resultGeoElements = result.getElements();
                    if (resultGeoElements.size() > 0) {
                        if (resultGeoElements.get(0) instanceof ArcGISFeature) {

                            // 選択されたジオメトリがわかる
                            ArcGISFeature deleteFeauture = (ArcGISFeature) resultGeoElements.get(0);
                            delete(deleteFeauture);
                        }
                    }
                    else {
                        // 何も選択されない場所をタップしときcalloutを閉じる
                        mCallout.dismiss();
                    }
                } catch (Exception e) {
                    Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     *　geodetabaseからポイント削除
     * */
    private boolean delete(ArcGISFeature pDeleteFeature){

        if(!mGdbFeatureTable.canDelete(pDeleteFeature)){
            // Deal with indicated error
            return false;
        }

        final ListenableFuture<Void> deleteFeatureResult = mGdbFeatureTable.deleteFeatureAsync(pDeleteFeature);
        deleteFeatureResult.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // ポイント追加の成功をチェックする:エラーがあればexceptionをthrowする
                    deleteFeatureResult.get();
                    Toast.makeText(getApplicationContext(), "delete point geodatabase", Toast.LENGTH_SHORT).show();

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
        return true;
    }
    ////////////////////////////////////////////////////////////////
    // geodatabase作成
    ////////////////////////////////////////////////////////////////
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
    FeatureLayer mFeatureLayer;
    private void readGeoDatabase(){

        geodatabase = new Geodatabase(mGeodatabasePath);
        geodatabase.loadAsync();
        geodatabase.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {

                // geodatabaseの読込 地図追加
                if(geodatabase.getLoadStatus() == LoadStatus.LOADED ){

                    if(geodatabase.getGeodatabaseFeatureTables().size() > 0){
                        int cnt = 0;
                        // 今回読み込むレイヤーは１つ=0
                        mGdbFeatureTable = geodatabase.getGeodatabaseFeatureTables().get(0);
                        try{
                            mFeatureLayer = new FeatureLayer(mGdbFeatureTable);
                            mFeatureLayer.setVisible(true);
//                            setPkges();// 他表示レイヤーを初期化
                            mMap.getOperationalLayers().add(mFeatureLayer);
                            cnt++;
                        }catch (Exception e){
                            e.printStackTrace();
                            String message = e.getMessage();
                        }
                    }else{
                        Log.e(TAG, geodatabase.getLoadStatus() + "/ FeatureTables.size=" + geodatabase.getGeodatabaseFeatureTables().size());
                    }

                }else if(geodatabase.getLoadStatus() == LoadStatus.FAILED_TO_LOAD){
                    Log.e(TAG,geodatabase.getLoadStatus().toString());
                }else if(geodatabase.getLoadStatus() == LoadStatus.NOT_LOADED){
                    Log.e(TAG,geodatabase.getLoadStatus().toString());
                }else if(geodatabase.getLoadStatus() == LoadStatus.LOADING){
                    Log.e(TAG,geodatabase.getLoadStatus().toString());
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////////
    // 同期
    ////////////////////////////////////////////////////////////////
    /**
     * サーバー(AGOL)と同期する
     * ① 同期タスクを作成する
     * ② 同期パラメータを取得する
     * */
    private void createSyncParameters() {

        // 同期したいレイヤーでタスクオブジェクトを作成する
        geodatabaseSyncTask = new GeodatabaseSyncTask(getResources().getString(R.string.org_agolfFeatureLayer));
        readGeoDatabase();

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
    static SyncGeodatabaseJob syncJob;
    private void syncGeodatabase() {

        // 同期ジョブオブヘジェクトを作成する
        syncJob = geodatabaseSyncTask.syncGeodatabaseAsync(syncParams, geodatabase);
        if(geodatabase.isSyncEnabled()){
            Log.i(TAG,"同期できる");
        }else{
            Log.e(TAG,"同期できない");
        }

        // 同期中のステータスをチェックする
        syncJob.addJobChangedListener(new Runnable() {
            @Override
            public void run() {
                if (syncJob.getError() != null) {
                    // 同期中にエラーがある場合
                    Log.e(TAG,syncJob.getError().toString());
                    Log.e(TAG,syncJob.getError().getCause().toString());
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

    ////////////////////////////////////////////////////////////////////////////////////////
    // 端末ローカルのパスまわり
    ////////////////////////////////////////////////////////////////////////////////////////
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
     * ローカルファイルから表示するPKGを設定する
     * 
     * */
    private void setPkges(){

        String extern = Environment.getExternalStorageDirectory().getPath();
        // Raster
        String rstPath  = extern + getResources().getString(R.string.local_raster);
        File rstfile = new File(rstPath);
        if(!rstfile.exists()){
            Log.d(TAG, rstPath + ":" + rstfile.exists());
            ArcGISFeatureTable substituteRasterTable = new ServiceFeatureTable(getResources().getString(R.string.org_emptyLayer));
            FeatureLayer substituteRasterLayer = new FeatureLayer(substituteRasterTable);
            mMap.getOperationalLayers().add(substituteRasterLayer);
        }else{
            Raster raster = new Raster(rstPath);
            final RasterLayer rasterLayer = new RasterLayer(raster);
            rasterLayer.setVisible(false);
            mMap.getOperationalLayers().add(rasterLayer);
//            mMap.getOperationalLayers().add(rasterLayer);
            rasterLayer.addDoneLoadingListener(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "raster added! ");
                }
            });
        }

        // tpk
        String tpkpath  = extern + getResources().getString(R.string.local_tpk);
        File tpkfile = new File(tpkpath);
        if(!tpkfile.exists()){
            Log.d(TAG, tpkpath + ":" + tpkfile.exists());
            ArcGISFeatureTable substituteTpkTable = new ServiceFeatureTable(getResources().getString(R.string.org_emptyLayer));
            FeatureLayer substituteTpkLayer = new FeatureLayer(substituteTpkTable);
            mMap.getOperationalLayers().add(substituteTpkLayer);
        }else{
            TileCache tileCache = new TileCache(tpkpath);
            ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tileCache);
            tiledLayer.setVisible(false);
            mMap.getOperationalLayers().add(tiledLayer);
        }

        // vtpk
        String vtpkpath = extern + getResources().getString(R.string.local_vtpk);
        File vtpkfile = new File(vtpkpath);
        if(!vtpkfile.exists()){
            Log.d(TAG, vtpkpath + ":" + vtpkfile.exists());
            ArcGISFeatureTable substituteVtpkTable = new ServiceFeatureTable(getResources().getString(R.string.org_emptyLayer));
            FeatureLayer substituteVtpkLayer = new FeatureLayer(substituteVtpkTable);
            mMap.getOperationalLayers().add(substituteVtpkLayer);
        }else{
            ArcGISVectorTiledLayer vectorTiledLayer = new ArcGISVectorTiledLayer(vtpkpath);
            vectorTiledLayer.setVisible(false);
            mMap.getOperationalLayers().add(vectorTiledLayer);
        }

    }

    /**
     * ローカルファイルをMapViewへ追加する
     * */
    public void chkGeodatabase(){

        String extern = Environment.getExternalStorageDirectory().getPath();
        File file = new File(mGeodatabasePath);
        Log.i(TAG, "GDB File Path:"+ mGeodatabasePath + ":" + file.exists());
        if(!file.exists()) {
            // ファイル作成メソッドをcallする
            createGeodatabaseSyncTask();
        }else {
            // 存在する場合は、既存のgeodatabaseから読み込む
            readGeoDatabase();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return true;
    }


}
