package com.arcgis.android.bookmarkoss;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Bookmark;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.OnBoomListener;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private ArcGISMap mMap;
    private String[] mNavigationDrawerItemTitles;

    private BookmarkList mBookmarks;
    private Bookmark mBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 地図の定義
        mMapView = (MapView) findViewById(R.id.mapView);
        mMap = new ArcGISMap(Basemap.createImagery());
        mMapView.setMap(mMap);

        // アクションバーのタイトル定義
        mNavigationDrawerItemTitles = getResources().getStringArray(R.array.baseballplace_titles);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            // 初期表示は横浜スタジアム
            getSupportActionBar().setTitle(mNavigationDrawerItemTitles[3]);
        }

        // ブックマークデータ作成
        addDefaultBookmarks();

        // BoonMenu ボタン作成
        BoomMenuButton bmb = (BoomMenuButton) findViewById(R.id.bmb);
        bmb.setButtonEnum(ButtonEnum.TextOutsideCircle);
        for (int i = 0; i < bmb.getButtonPlaceEnum().buttonNumber(); i++) {

            TextOutsideCircleButton.Builder builder = null;
            if(i % 2 == 0){
                builder = new TextOutsideCircleButton.Builder()
                        .normalImageRes(R.drawable.mitt)
                        .normalText(mNavigationDrawerItemTitles[i]);
            }else{
                builder = new TextOutsideCircleButton.Builder()
                        .normalImageRes(R.drawable.bat)
                        .normalText(mNavigationDrawerItemTitles[i]);
            }
            bmb.addBuilder(builder);
        }

        // BoonMenu ボタンイベント定義
        bmb.setOnBoomListener(new OnBoomListener() {
            @Override
            public void onClicked(int index, BoomButton boomButton) {
                // 選択したブックマークを表示
                selectBookmark(index);
            }
            @Override
            public void onBackgroundClick() {}
            @Override
            public void onBoomWillHide() {}
            @Override
            public void onBoomDidHide() {}
            @Override
            public void onBoomWillShow() {}
            @Override
            public void onBoomDidShow() {}
        });
    }

    /**
     * 選択されたブックマークを表示する
     */
    private void selectBookmark(int position){

        // アクションバータイトル変更
        getSupportActionBar().setTitle(mNavigationDrawerItemTitles[position]);

        // ブックマークオブジェクトから表示
        mMapView.setViewpointAsync(mBookmarks.get(position).getViewpoint());
    }

    /**
     * ブックマークリストを作成する
     */
    private void addDefaultBookmarks() {

        // get the maps BookmarkList
        mBookmarks = mMap.getBookmarks();

        Viewpoint viewpoint;

        // 東京ドーム
        viewpoint = new Viewpoint(35.704651999999996, 139.75302499999998, 6e3);
        mBookmark = new Bookmark(mNavigationDrawerItemTitles[0], viewpoint);
        mBookmarks.add(mBookmark);

        // 阪神甲子園球場
        viewpoint = new Viewpoint(34.721206,135.361622, 6e3);
        mBookmark = new Bookmark(mNavigationDrawerItemTitles[1], viewpoint);
        mBookmarks.add(mBookmark);

        // ナゴヤドーム
        viewpoint = new Viewpoint(35.1858451,136.9452949, 6e3);
        mBookmark = new Bookmark(mNavigationDrawerItemTitles[2], viewpoint);
        mBookmarks.add(mBookmark);

        // 横浜スタジアム
        viewpoint = new Viewpoint(35.443435, 139.63996399999996, 6e3);
        mBookmark = new Bookmark(mNavigationDrawerItemTitles[3], viewpoint);
        mBookmarks.add(mBookmark);
        // Set the viewpoint to the default bookmark selected in the spinner
        mMapView.setViewpointAsync(viewpoint);

        // 広島市民球場
        viewpoint = new Viewpoint(34.3917937,132.4833125, 6e3);
        mBookmark = new Bookmark(mNavigationDrawerItemTitles[4], viewpoint);
        mBookmarks.add(mBookmark);

        // 明治神宮野球場
        viewpoint = new Viewpoint(35.67451,139.7148603, 6e3);
        mBookmark = new Bookmark(mNavigationDrawerItemTitles[5], viewpoint);
        mBookmarks.add(mBookmark);

    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mMapView.pause();

    }
}