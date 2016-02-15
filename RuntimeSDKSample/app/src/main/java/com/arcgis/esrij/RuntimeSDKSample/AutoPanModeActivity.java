package com.arcgis.esrij.RuntimeSDKSample;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;

public class AutoPanModeActivity extends Activity {
    // MapView
    private MapView mMapView = null;
    private LocationDisplayManager locationDisplayManager = null;

    // ベースマップの切り替えメニュー.
    private MenuItem mCompass = null;
    private MenuItem mLocation = null;
    private MenuItem mNavigation = null;
    private MenuItem mOff = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autopanmode);

        // レイアウトファイルで指定した地図の表示
        mMapView = (MapView) findViewById(R.id.map);

        // Esri ロゴの有効化.
        mMapView.setEsriLogoVisible(true);
        // 日付変更線を跨ぐ地図表示を有効化
        mMapView.enableWrapAround(true);

        // MapView 上に現在位置を表示するために LocationDisplayManager を取得
        locationDisplayManager = mMapView.getLocationDisplayManager();
        // LocationDisplayManager に LocationListner を設定
        locationDisplayManager.setLocationListener(new MyLocationListener());
        // 現在位置の表示を開始
        locationDisplayManager.start();
    }

    // LocationListner を実装
    private class MyLocationListener implements LocationListener {

        public MyLocationListener() {
            super();
        }

        public void onLocationChanged(Location loc) {
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_autopanmode, menu);

        // AutoPanMode 切り替えのメニュー項目を取得
        mCompass = menu.getItem(0);
        mLocation = menu.getItem(1);
        mNavigation = menu.getItem(2);
        mOff = menu.getItem(3);

        // デフォルトのチェック項目を指定.
        mOff.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 選択したメニューごとに処理をハンドリング.
        switch (item.getItemId()) {
            // コンパス モード
            case R.id.mode_compass:
                locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.COMPASS);
                mCompass.setChecked(true);
                return true;
            // ロケーション モード
            case R.id.mode_location:
                locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                mLocation.setChecked(true);
                return true;
            // ナビゲーション モード
            case R.id.mode_navigation:
                locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
                mNavigation.setChecked(true);
                return true;
            // オフ モード
            case R.id.mode_off:
                locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
                mOff.setChecked(true);
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
