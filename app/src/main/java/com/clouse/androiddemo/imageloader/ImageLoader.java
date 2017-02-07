package com.clouse.androiddemo.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by 浩 on 2017/2/5.
 * 用于图片加载
 */

public class ImageLoader {
    /**
     * 单例实例对象
     */
    private static ImageLoader mInstance;

    /**
     * 缓存bitmap对象
     */
    private LruCache<String, Bitmap> mLruBitmapCache;

    //在使用一个变量后一定要确保是否初始化完成
    private Semaphore mLoopHandlerSemaphore = new Semaphore(0);

    /**
     * 线程池，用于执行runnable对象
     */
    private ExecutorService mThreadPool;
    /**
     * 默认线程池线程数
     */
    private static final int DEFAULT_THREAD_COUNT = 1;

    /**
     * 后台轮询线程，轮询线程队列，取出runnable到线程池中
     */
    private Thread mLoopThread;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    private Type mType;
    /**
     * 轮询线程默认的取出策略
     */
    private static final Type DEFAULT_TYPE_FOR_LOOP = Type.LIFO;
    //线程队列的信号量，因为从线程队列取任务到线程池是瞬间完成
    private Semaphore mThreadPoolSemaphore;

    /**
     * 后台轮询线程的handler
     */
    private Handler mLoopHander;

    /**
     * 主线程更新图片handler
     */
    private Handler mUIHandler;

    /**
     * 后台轮询线程从任务队列取任务的策略，先进先出，后进先出
     */
    private enum Type {
        FIFO, LIFO;
    }

    /**
     * 私有构造方法
     */
    private ImageLoader(int threadCount,Type type) {
        init(threadCount,type);
    }

    /**
     * 初始化操作
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        mType = type;
        mLoopThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                //为了保证mLoopHandler初始化完毕在addTask执行前
                mLoopHander = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //TODO:从队列中去task加入到线程池中执行
                        mThreadPool.execute(getTaskFromQueue());
                        try {
                            mThreadPoolSemaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //初始化完毕释放掉一个信号量
                mLoopHandlerSemaphore.release();
                Looper.loop();
            }
        };
        mLoopThread.start();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruBitmapCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mThreadPoolSemaphore = new Semaphore(threadCount);
    }
    private Runnable getTaskFromQueue() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == mType.LIFO) {
            return mTaskQueue.removeLast();
        } else {
             return  null;
        }
    }

    /**
     * 外部调用获取实例方法
     *
     * @return
     */
    public static ImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEFAULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    public void loadImage(final String path, final ImageView imageView){
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    Bitmap bitmap = holder.bitmap;
                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }
        Bitmap bitmap = getBitmapFromLruCache(path);
        if (bitmap != null) {
            refreshBitmapToImageView(path, imageView, bitmap);
        } else addTask(new Runnable() {
            @Override
            public void run() {
                ImageSize imageSize = getImageSize(imageView);
                Bitmap bitmap = decodeBitmapFromPath(path, imageSize);
                addBitmapToLruCache(path,bitmap);
                refreshBitmapToImageView(path,imageView,bitmap);
                mThreadPoolSemaphore.release();
            }
        });
    }

    /**
     * 将bitmap加入到缓存中
     * @param key
     * @param bitmap
     */
    private void addBitmapToLruCache(String key, Bitmap bitmap) {
        if (getBitmapFromLruCache(key) == null) {
            mLruBitmapCache.put(key,bitmap);
        }
    }

    private void refreshBitmapToImageView(String path, ImageView imageView, Bitmap bitmap) {
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bitmap;
        holder.imageView = imageView;
        holder.path = path;
        Message msg = mUIHandler.obtainMessage();
        msg.obj = holder;
        msg.sendToTarget();
    }

    private Bitmap decodeBitmapFromPath(String path, ImageSize imageSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = caculateInsampleSize(options,imageSize.width,imageSize.height);
        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path,options);
        return bm;
    }

    private int caculateInsampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        int width = options.outWidth;
        int height = options.outHeight;
        if (width > reqWidth || height > reqHeight) {
            //四舍五入
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round (height * 1.0f /reqHeight);
            //取较大值，压缩的更小
            inSampleSize = Math.max(widthRadio,heightRadio);
        }
        return inSampleSize;
    }

    private ImageSize getImageSize(ImageView imageView) {
        ImageSize imageSize =  new ImageSize();
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (width <= 0) {
            ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            width = lp.width;
        }
        if (width <= 0) {
            try {
                Class<? extends ImageView> imageViewClass = imageView.getClass();
                Field f = imageViewClass.getDeclaredField("mMaxWidth");
                f.setAccessible(true);
                width = f.getInt(imageView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (width <= 0) {
            DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
            width = displayMetrics.widthPixels;
        }
        if (height <= 0) {
            ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            height = lp.height;
        }
        if (height <= 0) {
            try {
                Class<? extends ImageView> imageViewClass = imageView.getClass();
                Field f = imageViewClass.getDeclaredField("mMaxHeight");
                f.setAccessible(true);
                height = f.getInt(imageView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (height <= 0) {
            DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    private synchronized void addTask(Runnable task) {
        mTaskQueue.add(task);
        //if mLoopHander == null wait; 加synchronized是为了多个线程进入，由信号量导致死锁
        if (mLoopHander == null) {
            try {
                mLoopHandlerSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mLoopHander.sendEmptyMessage(0x100);
    }

    private Bitmap getBitmapFromLruCache(String path) {
        return mLruBitmapCache.get(path);
    }

    private class ImageBeanHolder{
        ImageView imageView;
        String path;
        Bitmap bitmap;
    }

    private class ImageSize {
        int width;
        int height;
    }
}
