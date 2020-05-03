package com.example.catalogshopping.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.catalogshopping.CrosshairDrawable;
import com.example.catalogshopping.ModelLoader;
import com.example.catalogshopping.ProductsAdapter;
import com.example.catalogshopping.R;
import com.example.catalogshopping.model.ShoppingCart;
import com.example.catalogshopping.other.FirebaseStorageConstants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ArSceneView;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CustomArFragment fragment;

    private CrosshairDrawable crosshair = new CrosshairDrawable();
    private boolean isTracking;
    private boolean isHitting;

    private ModelLoader modelLoader;

    private StorageReference storageReference;

    private FirebaseVisionBarcodeDetector detector;
    private FirebaseVisionBarcodeDetectorOptions options =
            new FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                            FirebaseVisionBarcode.FORMAT_EAN_13)
                    .build();


    private Button dw;

    private ShoppingCart shoppingCart;

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
        initShoppingCart();
    }

    public CustomArFragment getCustomArFragment() {
        return fragment;
    }

    private void initShoppingCart() {
        shoppingCart = new ShoppingCart();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_main_cart);
        ProductsAdapter productsAdapter = new ProductsAdapter(shoppingCart);

        recyclerView.setAdapter(productsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void initFirebaseElements() {
        // FIREBASE STORAGE
        //        FirebaseApp.initializeApp(this);
        storageReference = FirebaseStorage.getInstance().getReference();

        // COMPUTER VISION ML
        detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options);
    }

    private void initUI() {
        dw = (Button) findViewById(R.id.button);
        dw.setOnClickListener(e -> {
            try {
                File modelFile = File.createTempFile("default", FirebaseStorageConstants.SFB);
                File imageFile = File.createTempFile("default", FirebaseStorageConstants.PNG);

                storageReference.child(FirebaseStorageConstants.IMAGES + "default" + "." + FirebaseStorageConstants.PNG)
                        .getFile(imageFile).addOnSuccessListener(taskSnapshot -> {
                    Log.i(TAG, "Firebase storage image loaded successfully");

                }).addOnFailureListener(e1 -> Log.i(TAG, "Firebase storage image could not be loaded"));

                storageReference.child(FirebaseStorageConstants.MODELS + "default" + "." + FirebaseStorageConstants.SFB)
                        .getFile(modelFile).addOnSuccessListener(taskSnapshot -> {
                    Log.i(TAG, "Firebase storage file loaded successfully");
                    addModelToScene(modelFile);
                }).addOnFailureListener(e1 -> Log.i(TAG, "Firebase storage file could not be loaded"));

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
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
                    modelLoader.loadModel(hit.createAnchor(), file.getPath());
                    break;
                }
            }
        }
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



    private void photoAction() {
        ArSceneView view = fragment.getArSceneView();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                takePhotoFirebase(bitmap);
            } else {
                Toast toast = Toast.makeText(MainActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));

    }

    private void takePhotoFirebase(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(barcodes -> {
                    for (FirebaseVisionBarcode barcode: barcodes) {
                        Rect bounds = barcode.getBoundingBox();
                        Point[] corners = barcode.getCornerPoints();

                        String rawValue = barcode.getRawValue();

                        Snackbar.make(findViewById(android.R.id.content),
                                rawValue, Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {

                });
    }

}
