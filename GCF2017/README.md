## GCF2017

このサンプルは、2017年5月に開催されたGISコミュニティフォーラムでデモンストレーションを行ったAndroidアプリです。
Android端末でプロットしたポイントデータを、サーバー(ArcGIS Server / ArcGIS Online)のフィーチャ レイヤーと同期することができます。
あらかじめ端末に保存した以下のデータを読み込んで表示することができます。

 * タイルパッケージ
 * ベクタータイルパッケージ
 * ラスターデータファイル

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
  編集するArcGIS Feature layerおよび、読み込みしたい端末データのファイルパスを定義します。

