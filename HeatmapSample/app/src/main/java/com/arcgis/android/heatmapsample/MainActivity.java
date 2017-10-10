package com.arcgis.android.heatmapsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.Renderer;

/**
 * ヒートマップを表現してみる
 * サンプルコード
 *
 * wroteby EsriJapan wakanasato
 * 2017/10/10
 *
 * */
public class MainActivity extends AppCompatActivity {

    public MapView mMapView;
    public ArcGISMap mArcGISMap;

    // レインボー ヒートマップの定義
    String rainbowheatmap = "{\"type\":\"heatmap\",\"blurRadius\":22.4468085106383,\"colorStops\":[" +
            "{\"ratio\":0,\"color\":[133,193,200,0]},\n" +
            "{\"ratio\":0.01,\"color\":[255,0,0,255]},\n" +
            "{\"ratio\":0.09249999999999999,\"color\":[255,165,0,255]},\n" +
            "{\"ratio\":0.175,\"color\":[255,255,0,255]},\n" +
            "{\"ratio\":0.2575,\"color\":[0,128,0,255]},\n" +
            "{\"ratio\":0.33999999999999997,\"color\":[0,255,255,255]},\n" +
            "{\"ratio\":0.42250000000000004,\"color\":[0,0,255,255]},\n" +
            "{\"ratio\":0.505,\"color\":[128,0,128,255]},\n" +
            "{\"ratio\":1,\"color\":[255,255,0,255]}\n" +
            "],\"maxPixelIntensity\":1466.0194349589192,\"minPixelIntensity\":0}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // フィーチャレイヤーをレンダラークラスを使用してヒートマップを表示する
        mMapView = (MapView) findViewById(R.id.mapView);
        mArcGISMap = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 35.658581, 139.745433, 10);
        mMapView.setMap(mArcGISMap);

        // 表示したいフィーチャレイヤーを呼び出す
        FeatureTable featureTable = new ServiceFeatureTable("https://services.arcgis.com/wlVTGRSYTzAbjjiC/arcgis/rest/services/tokyohoikuen23/FeatureServer/0");
        FeatureLayer featureLayer = new FeatureLayer(featureTable);

        // ヒートマップでレンダリングするための定義をjsonから作成する
        Renderer heatRend = Renderer.fromJson(rainbowheatmap);

        // 作成したヒートマップの定義をフィーチャ レイヤーに追加する
        featureLayer.setRenderer(heatRend);
        mArcGISMap.getOperationalLayers().add(featureLayer);

    }
}
