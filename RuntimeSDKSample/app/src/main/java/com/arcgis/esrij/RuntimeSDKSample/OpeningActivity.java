package com.arcgis.esrij.RuntimeSDKSample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.ImageView;

public class OpeningActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // タイトルバーを非表示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // オープニング用レイアウトを表示
        setContentView(R.layout.activity_opening);
        ImageView imageView1 = (ImageView)findViewById(R.id.imageView);
        imageView1.setImageResource(R.drawable.ejlogo);

        Handler hdl = new Handler();
        // 1 秒（1000ms）後に次に表示するクラスを実行
        hdl.postDelayed(new openingHandler(), 1000);
    }
    class openingHandler implements Runnable {
        public void run() {
            // 次に表示するクラス
            Intent intent = new Intent(getApplication(), ListSampleActivity.class);
            startActivity(intent);
            // オープニング終了
            OpeningActivity.this.finish();

        }
    }
}
