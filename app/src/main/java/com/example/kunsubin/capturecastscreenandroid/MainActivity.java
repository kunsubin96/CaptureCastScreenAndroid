package com.example.kunsubin.capturecastscreenandroid;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.example.kunsubin.capturecastscreenandroid.recorder.RecordService;
import com.example.kunsubin.capturecastscreenandroid.recorder.RecordServiceListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int RECORD_REQUEST_CODE = 101;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;
    
    private BroadcastReceiver broadcastReceiverNetworkState;
    
    private Button mButtonRecorder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }
    private void initView(){
        mButtonRecorder=findViewById(R.id.btn_record);
    }
    private void initEvent() {
        initBroadcastReceiverNetworkStateChanged();
        projectionManager = (MediaProjectionManager) this.getSystemService(MEDIA_PROJECTION_SERVICE);
        
        
        Intent recordIntent = new Intent(this, RecordService.class);
        this.bindService(recordIntent, recorderConnection, BIND_AUTO_CREATE);
    
        mButtonRecorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordService.isRunning()) {
                    recordService.stopRecord();
                } else {
                    Intent captureIntent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                }
            }
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            recordService.setMediaProject(mediaProjection);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    recordService.startRecord();
                }
            }).start();
        }
    }
    private ServiceConnection recorderConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            
            recordService.setListener(new RecordServiceListener() {
                @Override
                public void onRecorderStatusChanged(boolean isRunning) {
                
                }
            });
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    
    @Override
    public void onDestroy() {
        recordService.removeListener();
        unbindService(recorderConnection);
        if (broadcastReceiverNetworkState != null) {
            unregisterReceiver(broadcastReceiverNetworkState);
        }
        super.onDestroy();
    }
    
    private void initBroadcastReceiverNetworkStateChanged() {
        final IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        filters.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        broadcastReceiverNetworkState = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + intent.getAction());
                int state = intent.getIntExtra("wifi_state", -66);
                Log.i(TAG, "state= " + state);
                if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())
                    && state == 13) {
                   // vm.mode.set(1);
                } else {
                    //vm.mode.set(0);
                }
            }
        };
        registerReceiver(broadcastReceiverNetworkState, filters);
    }
    
}
