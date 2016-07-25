package com.example.cam;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zyang_000 on 2016/7/21.
 */
public class MSurface extends SurfaceView implements SurfaceHolder.Callback {


    public MSurface(Context context) {
        super(context);
        getHolder().addCallback(this);
    }


    protected void onDraw(Canvas canvas, Bitmap bm) {
        super.onDraw(canvas);
        //Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon);

        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(bm, 10, 10, new Paint());
    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas(null);
            synchronized (holder) {
                onDraw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }
}

