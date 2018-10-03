package com.example.kunsubin.capturecastscreenandroid.recorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import com.example.kunsubin.capturecastscreenandroid.config.Configs;
import com.example.kunsubin.capturecastscreenandroid.socket.TcpSocketClient;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordService extends Service {
    private static final String TAG = "RecordService";
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private boolean running;
    private int width;
    private int height;
    private int dpi;
    
    private ScreenHandler screenHandler;
    private int threadCount;
    private ExecutorService executorService;
    private Scheduler scheduler;
    
    private long imgFlag = 0;
    private long postedImgFlag = 0;
    
    private TcpSocketClient mTcpSocketClient;
    
    private RecordServiceListener recordServiceListener;
    
    public void setListener(RecordServiceListener listener) {
        recordServiceListener = listener;
    }
    
    public void removeListener() {
        recordServiceListener = null;
    }
    
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    
    private class ScreenHandler extends Handler {
        public ScreenHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            mTcpSocketClient=new TcpSocketClient(InetAddress.getByName(Configs.IP_SERVER), Configs.PORT_SERVER);
            mTcpSocketClient.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        
        HandlerThread serviceThread = new HandlerThread("service_thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        
        threadCount = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "onCreate: threadCount" + threadCount);
        executorService = Executors.newFixedThreadPool(threadCount);
        
        HandlerThread handlerThread = new HandlerThread("Screen Record");
        handlerThread.start();
        screenHandler = new ScreenHandler(handlerThread.getLooper());
        
        //get the size of the window
        WindowManager mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        //        width = mWindowManager.getDefaultDisplay().getWidth() + 40;
        width = mWindowManager.getDefaultDisplay().getWidth();
        height = mWindowManager.getDefaultDisplay().getHeight();
        //height = 2300;
        Log.i(TAG, "onCreate: w is " + width + " h is " + height);
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        
        scheduler = Schedulers.from(executorService);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecord();
        compositeDisposable.dispose();
    }
    
    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }
    
    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }
        executorService = Executors.newFixedThreadPool(threadCount);
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        createVirtualDisplayForImageReader();
        running = true;
        if (recordServiceListener != null) {
            recordServiceListener.onRecorderStatusChanged(running);
        }
        return true;
    }
    
    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            executorService = null;
        }
        if (imageReader != null)
            imageReader.close();
        if (recordServiceListener != null) {
            recordServiceListener.onRecorderStatusChanged(running);
        }
        return true;
    }
    
    private void createVirtualDisplayForImageReader() {
        
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi
                  , DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, imageReader.getSurface()
                  , null, screenHandler);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                try {
                    Image img = imageReader.acquireLatestImage();
                    if (img != null) {
                        Log.d(TAG, "onImageAvailable: ");
                        int width = img.getWidth();
                        int height = img.getHeight();
                        Image.Plane[] planes = img.getPlanes();
                        
                        ByteBuffer buffer = planes[0].getBuffer();
                      
                        img.close();
                        
                        ImageInfo imageInfo = new ImageInfo(width, height, buffer);
                        
                        Observable.just(new FlagImageInfo(imageInfo, imgFlag++))
                                  .subscribeOn(scheduler)
                                  .observeOn(scheduler)
                                  .map(getBitmapFunction())
                                  .subscribe(getBitmapConsumer());
                        
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, screenHandler);
    }
    
    private Function<FlagImageInfo, FlagBitmap> getBitmapFunction() {
        return new Function<FlagImageInfo, FlagBitmap>() {
            @Override
            public FlagBitmap apply(FlagImageInfo flagImageInfo) throws Exception {
                ImageInfo imageInfo = flagImageInfo.imageInfo;
                Bitmap bitmap = Bitmap.createBitmap(imageInfo.width, imageInfo.height,
                          Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(imageInfo.byteBuffer);
               // bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                bitmap=Bitmap.createScaledBitmap(bitmap, Configs.WIDTH_SCALE, Configs.HEIGHT_SCALE, false);
                
                return new FlagBitmap(bitmap, flagImageInfo.flag);
            }
        };
    }
    
    private Consumer<FlagBitmap> getBitmapConsumer() {
        return new Consumer<FlagBitmap>() {
            @Override
            public void accept(FlagBitmap flagBitmap) throws Exception {
                Bitmap bitmap = flagBitmap.bitmap;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, Configs.QUALITY_SCREEN_IMAGE, byteArrayOutputStream);
                
                byte[] b = byteArrayOutputStream.toByteArray();
                String base64Str = Base64.encodeToString(b, Base64.DEFAULT);
                
                if (flagBitmap.flag > postedImgFlag) {
                    postedImgFlag = flagBitmap.flag;
                    Log.d(TAG, "Length: "+base64Str.length());
                    Log.d(TAG,"Size: "+b.length);
                    //server split stringbase64
                    base64Str+="  ";
                    mTcpSocketClient.send(base64Str.getBytes());
                    Thread.sleep(100);
                }
                
                try {
                    byteArrayOutputStream.flush();
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bitmap.recycle();
                }
            }
        };
    }
    
    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }
    
    private class ImageInfo {
        private int width;
        private int height;
        private ByteBuffer byteBuffer;
        
        public ImageInfo(int width, int height, ByteBuffer byteBuffer) {
            this.width = width;
            this.height = height;
            this.byteBuffer = byteBuffer;
        }
    }
    private class FlagImageInfo {
        private ImageInfo imageInfo;
        private long flag;
        
        public FlagImageInfo(ImageInfo imageInfo, long flag) {
            this.imageInfo = imageInfo;
            this.flag = flag;
        }
    }
    private class FlagBitmap {
        private Bitmap bitmap;
        private long flag;
        public FlagBitmap(Bitmap bitmap, long flag) {
            this.bitmap = bitmap;
            this.flag = flag;
        }
    }
}