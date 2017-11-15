package com.arcgis.android.polygonhit

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleFillSymbol

/**
 * 検索して該当したポリゴンにフォーカスするアプリ
 * ESRIジャパン wrote by wakanasato
 * */
class MainActivity : AppCompatActivity() {

    var mMapView: MapView? = null
    var mServiceFeatureTable: ServiceFeatureTable? = null
    var mFeatureLayer: FeatureLayer? = null
    var mSimpleFillSymbol: SimpleFillSymbol? =null
    var mGraphicOverlay : GraphicsOverlay? = null

    val TAG = "☆esrij☆"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // https://qiita.com/kubode/items/c7fe9c091fdf5533d36b
        mMapView = findViewById<MapView>(R.id.mapView) // compileSdkVersion26からはこう！
        // mMapView = findViewById(R.id.mapView) as MapView
        var arcgisMap = ArcGISMap(Basemap.createTopographic())
        // !!とプロパティ参照
        mMapView!!.map = arcgisMap

        // ポリゴンデータのレイヤーを読み込む
        mServiceFeatureTable = ServiceFeatureTable(resources.getString(R.string.FeatureLayerURL))
        mFeatureLayer = FeatureLayer(mServiceFeatureTable)
        mFeatureLayer!!.opacity = 0.5f

        //表示するポリゴンの色を指定する
        mSimpleFillSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.argb(50,251,236,53), null)

        mGraphicOverlay = GraphicsOverlay()
        mMapView!!.graphicsOverlays.add(mGraphicOverlay)


        // 地図に東京都のポリゴンデータを重ねる
        arcgisMap.operationalLayers.add(mFeatureLayer)

        // 東京周辺を表示する
        mMapView!!.setViewpointCenterAsync(Point(1.5556390442245528E7, 4253743.76954561, SpatialReferences.getWebMercator()), 100000.0)

    }

    /**
     * 入力の文字をもとに該当するポリゴンを探す
     * ラムダ式で書いてみる
     *
     * */
    var searchForPolygon: (String) -> Unit = { searchString ->

        // セレクトしているPolygonをクリアにする
        if(mGraphicOverlay!!.graphics.size > 0){
            mGraphicOverlay!!.graphics.clear()
        }

        //　検索文字列を作成してqueryに設定する
        var query = QueryParameters()
        query.whereClause = createstr(searchString);
        Log.d(TAG, query.whereClause.toString())

        // ホストしているポリゴンデータに対して検索をかける
        val future = mServiceFeatureTable!!.queryFeaturesAsync(query)
        // SAM変換で記述
        future.addDoneListener {
            try {
                // 検索結果全体を取得する
                val result = future.get()
                // 結果の検査
                if (result.iterator().hasNext()) {
                    // 最初の結果を取得して、該当するフィーチャへズームする
                    val feature = result.iterator().next()
                    val envelope = feature.geometry.extent
                    mMapView!!.setViewpointGeometryAsync(envelope, 200.0)

                    // ズームしたフィーチャの色を変更する
                    var selectGraphic = Graphic(feature.geometry,mSimpleFillSymbol)
                    mGraphicOverlay!!.graphics.add(selectGraphic)

                } else {
                    Toast.makeText(this@MainActivity, "この検索に該当するポリゴンは見つかりませんでした: " + searchString, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "フィーチャ検索に失敗しました: " + searchString + ". Error=" + e.message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Feature search failed for: " + searchString + ". Error=" + e.message)
            }
        }
     }

    /**
     * ArcGISへCALLする 検索文字列作成
     * */
    fun createstr (searchString: String) : String {
        // searchStringには画面入力された文字列が入ってくる
        var reString: String? = null
        val ku = searchString.split("区".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        if (ku.size > 1) {
            // 区でsplitできたら「区+丁目」で検索できる条件を作成する
            // upper(CSS_NAME) LIKE '新宿区' and (MOJI) LIKE '%新宿5丁目'
            reString = "upper(CSS_NAME) LIKE '" + ku[0] + "区' and (MOJI) LIKE '%" + ku[1] + "%'"
        } else {
            // 区でsplitできない場合は入力で検索できる条件を作成する
            reString = "upper(MOJI) LIKE '%" + ku[0] + "%'"
        }
        Log.d(TAG, "SQL String="+ reString)
        return reString
    }

    /**
     * 検索ウィジェットから検索文字列を受け取る
     */
    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val searchString = intent.getStringExtra(SearchManager.QUERY)
            Log.d("satowaka☆", searchString)
            var result = searchForPolygon(searchString)
        }
    }

    /**
     * アクションバーに検索ウィジェット用のメニューを作成する
     * */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // 検索条件などに対応するオブジェクトを作成
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        // 検索可能なActivityとして設定する
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(false)

        return true
    }

}
