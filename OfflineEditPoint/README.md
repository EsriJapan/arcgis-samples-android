## OfflineEditPoint

このサンプルは、Android端末でプロットしたポイントデータを、サーバー(ArcGIS Server / ArcGIS Online)のフィーチャ レイヤーと同期することができます。
タップしたポイントはローカルのgeodatabaseファイルに保存し、3つめのポイントをタップした時点で、指定したArcGIS Online のフィーチャ レイヤーと同期します。
画面の任意の場所をタップすると、吹き出し表示します。


## 設定
 * Android 端末設定
  Android Studio または adb コマンドを使用して、以下のディレクトリを作成します。
  ここでは、adbコマンドを使用して以下のディレクトリを作成します。
  1. `adb shell` // adbコマンドで端末にログイン
  1. `cd sdcard` // 外部領域のところまで移動
  1. `mkdir ArcGIS/samples/OfflineSample` // geodatabaseファイルを作成するディレクトリを作成</br>
  ※AndroidOS 6.0以上ならば、設定からアプリ自体の権限設定があるので書き込み権限を有効にする
 
 * Android アプリ設定</br>
  OfflineEditPoint/app/src/main/res/values/strings.xml</br>
  編集するArcGIS Feature layerを定義する

 * ArcGIS Online フィーチャ レイヤー設定</br>
  編集可能レイヤーの設定を行います。</br>
  参考：http://doc.arcgis.com/ja/arcgis-online/share-maps/manage-hosted-layers.htm#ESRI_SECTION2_CF5B6C3A15F94A6A81FC2083CEEC2A6E
 