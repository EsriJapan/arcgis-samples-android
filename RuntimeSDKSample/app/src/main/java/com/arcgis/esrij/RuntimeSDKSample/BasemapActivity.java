package com.arcgis.esrij.RuntimeSDKSample;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Polygon;

public class BasemapActivity extends Activity {

    // MapView
    private MapView mMapView = null;

    // ベースマップの切り替えメニュー.
    private MenuItem mStreetsMenuItem = null;
    private MenuItem mTopoMenuItem = null;
    private MenuItem mHybridMenuItem = null;
    private MenuItem mGrayMenuItem = null;
    private MenuItem mOceansMenuItem = null;

    // ベースマップごとに MapOptions を作成
    private final MapOptions mapoptionStreets = new MapOptions(MapOptions.MapType.STREETS);
    private final MapOptions mapoptionTopo = new MapOptions(MapOptions.MapType.TOPO);
    private final MapOptions mapoptionHybrid = new MapOptions(MapOptions.MapType.HYBRID);
    private final MapOptions mapoptionGray = new MapOptions(MapOptions.MapType.GRAY);
    private final MapOptions mapoptionOceans = new MapOptions(MapOptions.MapType.OCEANS);

    // 現在の地図表示範囲をベースマップ切り替え後も使用する
    private Polygon mCurrentMapExtent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basemap);

        // レイアウトファイルで指定した地図の表示
        mMapView = (MapView) findViewById(R.id.map);

        // Esri ロゴの有効化.
        mMapView.setEsriLogoVisible(true);
        // 日付変更線を跨ぐ地図表示を有効化
        mMapView.enableWrapAround(true);

        // マップの状態変化のリスナーを設定。ベースマップを切り替えるときに呼び出される.
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            public void onStatusChanged(Object source, STATUS status) {
            // ベースマップが変更されたらマップ範囲を設定する
            if (STATUS.LAYER_LOADED == status) {
                mMapView.setExtent(mCurrentMapExtent);
            }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_basemap, menu);

        // ベースマップ切り替えのメニュー項目を取得
        mStreetsMenuItem = menu.getItem(0);
        mTopoMenuItem = menu.getItem(1);
        mHybridMenuItem = menu.getItem(2);
        mGrayMenuItem = menu.getItem(3);
        mOceansMenuItem = menu.getItem(4);
        // デフォルトのチェック項目を指定.
        mTopoMenuItem.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // マップを変更する前に、マップの現在の範囲を取得
        mCurrentMapExtent = mMapView.getExtent();

        // 選択したメニューごとに処理をハンドリング.
        switch (item.getItemId()) {
            // 道路地図
            case R.id.World_Street_Map:
                mMapView.setMapOptions(mapoptionStreets);
                mStreetsMenuItem.setChecked(true);
                return true;
            // 地形図
            case R.id.World_Topo:
                mMapView.setMapOptions(mapoptionTopo);
                mTopoMenuItem.setChecked(true);
                return true;
            // 地形図
            case R.id.Hybrid:
                mMapView.setMapOptions(mapoptionHybrid);
                mHybridMenuItem.setChecked(true);
                return true;
            // キャンバス（グレー）
            case R.id.Gray:
                mMapView.setMapOptions(mapoptionGray);
                mGrayMenuItem.setChecked(true);
                return true;
            // 海洋図
            case R.id.Ocean_Basemap:
                mMapView.setMapOptions(mapoptionOceans);
                mOceansMenuItem.setChecked(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    protected void onResume() {
        super.onResume();
        mMapView.unpause();
    }

}