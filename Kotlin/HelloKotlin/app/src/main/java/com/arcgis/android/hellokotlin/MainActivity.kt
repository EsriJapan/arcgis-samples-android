package com.arcgis.android.hellokotlin

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.TextView
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.MapView


/**
 * タップした緯度経度を表示する地図アプリ
 * ESRIジャパン wrote by wakanasato
 * */
class MainActivity : AppCompatActivity() {

    private var mMapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // MapView の作成
        mMapView = findViewById(R.id.mapView) as MapView

        // Basemap の作成
        val map = ArcGISMap(Basemap.Type.TOPOGRAPHIC, 35.3312442,139.6202471, 8)

        // MapViewへBasemapを追加する
        mMapView!!.map = map

        /**
         * タッチイベントを実装する
         * */
        mMapView!!.setOnTouchListener(object : DefaultMapViewOnTouchListener(this, mMapView) {

            override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {

                val screenPoint = android.graphics.Point(Math.round(motionEvent!!.getX()),Math.round((motionEvent!!.getY())))
                val arcgisPoint = mMapView!!.screenToLocation(screenPoint)
                // WGS84(緯度経度)へ変換
                val arcgis84p = GeometryEngine.project(arcgisPoint, SpatialReferences.getWgs84()) as Point

                // 表示するテキストを作成する
                val calloutTextview = TextView(applicationContext)
                calloutTextview.setText("Lat:" + String.format("%.4f", arcgis84p.getY()) + "/Lon:" + String.format("%.4f", arcgis84p.getX()))
                calloutTextview.setTextColor(Color.BLACK) // JavaCode!

                // 表示オブジェクトに設定
                val callout = mMapView!!.callout
                callout.location  = arcgisPoint
                callout.content = calloutTextview
                callout.show()
                return true
            }
        } )

    }

    override fun onPause() {
        mMapView!!.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.resume()
    }


}
