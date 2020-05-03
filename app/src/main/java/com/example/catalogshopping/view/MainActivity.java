package com.example.catalogshopping.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.catalogshopping.CrosshairDrawable;
import com.example.catalogshopping.ModelLoader;
import com.example.catalogshopping.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CustomArFragment fragment;

    private CrosshairDrawable crosshair = new CrosshairDrawable();
    private boolean isTracking;
    private boolean isHitting;

    private ModelLoader modelLoader;

    private StorageReference storageReference;

    private Button dw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFirebaseElements();

        modelLoader = new ModelLoader(new WeakReference<>(this));
        fragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment__main_ar);

        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            updateCrosshairStatus();
        });

        initUI();
    }

    public CustomArFragment getCustomArFragment() {
        return fragment;
    }

    private void initFirebaseElements() {
        // FIREBASE STORAGE
        //        FirebaseApp.initializeApp(this);
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    private void initUI() {
        dw = (Button) findViewById(R.id.button);
        dw.setOnClickListener(e -> {
            try {
                File file = File.createTempFile("def", "sfb");
                storageReference.child("/model/def.sfb").getFile(file).addOnSuccessListener(taskSnapshot -> {
                    Log.i("wololo", "SUCCC");
                    addModelToScene(file);
                }).addOnFailureListener(e1 -> Log.i("wololo", "NOOOOOOO"));

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void updateCrosshairStatus() {
        boolean trackingChanged = updateTrackingStatus();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(crosshair);
            } else {
                contentView.getOverlay().remove(crosshair);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                crosshair.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }

    private boolean updateTrackingStatus() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    private android.graphics.Point getScreenCenter() {
        View view = findViewById(android.R.id.content);
        return new android.graphics.Point(view.getWidth() / 2, view.getHeight() / 2);
    }

    private void addModelToScene(File file) {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    modelLoader.loadModel(hit.createAnchor(), file);
                    break;
                }
            }
        }
    }
}
