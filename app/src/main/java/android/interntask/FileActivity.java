package android.interntask;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Path;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {

    FrameLayout linearLayout;
    ImageButton draw, record;
    Paint paint;
    View view;
    Path path2;
    Bitmap bitmap;
    Canvas canvas;
    boolean isActive = false;
    static int i = 0;
    // recording
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSION_KEY = 1;
    boolean isRecording = false;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        final TextView fileText = findViewById(R.id.textField);
        fileText.setText(getIntent().getStringExtra("text"));

        final ScrollView scrollView = findViewById(R.id.scrollView);
        final LinearLayout layout = findViewById(R.id.linearLayout);
        draw = findViewById(R.id.paint);
        draw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isActive) {
                    View view1 = findViewById(R.id.reservedNamedId);
                    view1.setVisibility(View.INVISIBLE);
                    path2.reset();
                    isActive = false;
                    draw.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    Toast toast = Toast.makeText(FileActivity.this, "Drawing functionality disabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                } else {
                    try {
                        View view1 = findViewById(R.id.reservedNamedId);
                        view1.setVisibility(View.VISIBLE);
                        isActive = true;
                    } catch (NullPointerException e){

                    }
                    draw.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    Toast toast = Toast.makeText(FileActivity.this, "Drawing functionality enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                }
                if (findViewById(R.id.reservedNamedId) == null) {
                    draw.setEnabled(true);
                    view = new SketchSheetView(getApplicationContext());

                    paint = new Paint();

                    path2 = new Path();

                    linearLayout = findViewById(R.id.parentLayout);
                    linearLayout.addView(view, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            scrollView.getHeight() - layout.getHeight()));

                    view.setBackgroundColor(getResources().getColor(R.color.transparent));
                    view.setId(R.id.reservedNamedId);
                    paint.setDither(true);

                    paint.setColor(Color.parseColor("#000000"));

                    paint.setStyle(Paint.Style.STROKE);

                    paint.setStrokeJoin(Paint.Join.ROUND);

                    paint.setStrokeCap(Paint.Cap.ROUND);

                    paint.setStrokeWidth(5);

                    isActive = true;

                }

            }

        });
        record = findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] PERMISSIONS = {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                };
                if (!Function.hasPermissions(FileActivity.this , PERMISSIONS)) {
                    ActivityCompat.requestPermissions(FileActivity.this, PERMISSIONS, REQUEST_PERMISSION_KEY);
                }

                if (!isRecording) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    mScreenDensity = metrics.densityDpi;

                    mMediaRecorder = new MediaRecorder();

                    mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

                    record.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));

                    Log.e("TAG","I'm here 1");
                }

                onToggleScreenShare();


            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            isRecording = false;
            actionBtnReload();
            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        isRecording = true;
        actionBtnReload();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_KEY:
            {
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    onToggleScreenShare();
                } else {
                    isRecording = false;
                    actionBtnReload();
                    Snackbar.make(findViewById(android.R.id.content), "Please enable Microphone and Storage permissions.",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }

    public void onToggleScreenShare() {
        if (!isRecording) {
            initRecorder();
            shareScreen();
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            record.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
            stopScreenSharing();
        }

    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }

        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                isRecording = false;
                actionBtnReload();
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            Snackbar.make(findViewById(android.R.id.content), "Wanna Stop recording and exit?",
                    Snackbar.LENGTH_INDEFINITE).setAction("Stop",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mMediaRecorder.stop();
                            mMediaRecorder.reset();
                            stopScreenSharing();
                            finish();
                        }
                    }).show();
        } else {
            finish();
        }
    }

    public void actionBtnReload() {
        if (isRecording) {
            Toast toast = Toast.makeText(FileActivity.this,"Press again to stop recording.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
        }

    }

    private void initRecorder() {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //THREE_GPP
            mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/video.mp4");
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(16); // 30
            mMediaRecorder.setVideoEncodingBitRate(3000000);
            mMediaRecorder.setAudioSamplingRate(48000);
            mMediaRecorder.setAudioEncodingBitRate(384000);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();
        isRecording = false;
        Toast.makeText(this, "Recording has ended. Your file has been saved.", Toast.LENGTH_SHORT).show();
        actionBtnReload();
    }



    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }


    class SketchSheetView extends View {

        public SketchSheetView(Context context) {

            super(context);

            bitmap = Bitmap.createBitmap(820, 480, Bitmap.Config.ARGB_4444);

            canvas = new Canvas(bitmap);

            this.setBackgroundColor(Color.WHITE);
        }

        private ArrayList<DrawingClass> DrawingClassArrayList = new ArrayList<DrawingClass>();

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            DrawingClass pathWithPaint = new DrawingClass();

            canvas.drawPath(path2, paint);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                path2.moveTo(event.getX(), event.getY());

                path2.lineTo(event.getX(), event.getY());
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                path2.lineTo(event.getX(), event.getY());

                pathWithPaint.setPath(path2);

                pathWithPaint.setPaint(paint);

                DrawingClassArrayList.add(pathWithPaint);
            }

            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (DrawingClassArrayList.size() > 0) {

                canvas.drawPath(
                        DrawingClassArrayList.get(DrawingClassArrayList.size() - 1).getPath(),

                        DrawingClassArrayList.get(DrawingClassArrayList.size() - 1).getPaint());
            }
        }
    }

    public class DrawingClass {

        Path DrawingClassPath;
        Paint DrawingClassPaint;

        public Path getPath() {
            return DrawingClassPath;
        }

        public void setPath(Path path) {
            this.DrawingClassPath = path;
        }


        public Paint getPaint() {
            return DrawingClassPaint;
        }

        public void setPaint(Paint paint) {
            this.DrawingClassPaint = paint;
        }
    }

}
