package com.example.kunsubin.capturecastscreenandroid.socket;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TcpSocketClient extends Thread {
    private static final String TAG = "TcpSocketClient";
    
    private Socket mSocket;
    private OutputStream mOutputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private Handler mHandler;
    private final InetAddress mRemoteHost;
    private final int mRemotePort;
    public TcpSocketClient(InetAddress remoteHost, int remotePort) {
        super( "TcpSocketClientThread");
        this.mRemoteHost = remoteHost;
        this.mRemotePort = remotePort;
    }
    
    @Override
    public void run() {
        try {
            mSocket = new Socket(mRemoteHost, mRemotePort);
            mOutputStream = mSocket.getOutputStream();
            mBufferedOutputStream = new BufferedOutputStream(mOutputStream);
        } catch (IOException e) {
            Log.e(TAG, "Socket creation failed - " + e.toString());
            mSocket = null;
            mOutputStream = null;
            mBufferedOutputStream = null;
        }
        
        Looper.prepare();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if(message == null || message.obj == null) return;
                byte[] msg = (byte[])message.obj;
                try {
                    mBufferedOutputStream.write(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    close();
                }
            }
        };
        Looper.loop();
    }
    public void close() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mSocket = null;
                mOutputStream = null;
                mBufferedOutputStream = null;
            }
        }
    }
    public void send(final byte[] data) {
        Log.d(TAG,"Sending");
        if(mHandler == null || mSocket == null || mOutputStream == null)  {
            Log.d(TAG,"Null object");
            return;
        }
        Log.d(TAG,"Not null object");
        Message message = mHandler.obtainMessage();
        message.obj = data;
        mHandler.sendMessage(message);
    }
}
