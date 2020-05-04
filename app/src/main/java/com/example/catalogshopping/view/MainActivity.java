package com.example.catalogshopping.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;

import com.example.catalogshopping.CrosshairDrawable;
import com.example.catalogshopping.ModelLoader;
import com.example.catalogshopping.ProductsAdapter;
import com.example.catalogshopping.R;
import com.example.catalogshopping.model.Product;
import com.example.catalogshopping.model.ProductFirestore;
import com.example.catalogshopping.model.ShoppingCart;
import com.example.catalogshopping.other.ErrorMessageConstants;
import com.example.catalogshopping.other.FirebaseStorageConstants;
import com.example.catalogshopping.other.FirestoreConstants;
import com.example.catalogshopping.other.Util;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private FirebaseFirestore db;
    private CollectionReference products;


    private Button scan;
    private Button load;
    private Button clear;

    private ShoppingCart shoppingCart;
    private Product currentProduct;
    private ProductFirestore currentProductFirestore;
    private File currentProductImage;
    private File currentProductModel;

    private boolean[] loaded = new boolean[3];

    private List<AnchorNode> loadedNodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFirebaseElements();

        loadedNodes = new CopyOnWriteArrayList<>(); //TODO explain this shit
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

        // FIRESTORE
        db = FirebaseFirestore.getInstance();
        products = db.collection(FirestoreConstants.PRODUCTS);
    }

    private void initUI() {
        scan = (Button) findViewById(R.id.button_main_scan);
        load = (Button) findViewById(R.id.button_main_load);
        clear = (Button) findViewById(R.id.button_main_clear);

        clear.setOnClickListener(e -> {
            for (AnchorNode elem : loadedNodes) {
                modelLoader.removeAnchorNode(elem);
                loadedNodes.remove(elem);
            }
        });

        scan.setOnClickListener(e -> {
            createAndSendPhoto();
        });

        if (!checkAllLoaded()) {
            load.setVisibility(Button.INVISIBLE);
        }
        load.setOnClickListener(e -> {
            addModelToScene(currentProductModel);
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



    private void createAndSendPhoto() {
        //TODO WUUUUUD
        loaded[0] = false;
        loaded[1] = false;
        loaded[2] = false;
        load.setVisibility(Button.INVISIBLE);
        // REMOVE ANCHORS
        // SET NULLS

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
                sendPhotoFirebase(bitmap);
            } else {
                Util.showToast(this, true, "Failed to copyPixels: " + copyResult);
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));

    }

    private void sendPhotoFirebase(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(barcodes -> {
                    for (FirebaseVisionBarcode barcode: barcodes) {
                        Rect bounds = barcode.getBoundingBox();
                        Point[] corners = barcode.getCornerPoints();

                        String rawValue = barcode.getRawValue();

                        Snackbar.make(findViewById(android.R.id.content),
                                rawValue, Snackbar.LENGTH_LONG).show();

                        // GET INFO FIRESTORE
                        getProductFirestore(rawValue);
                        // GET MODEL STORAGE
                        getProductImageFirebaseStorage(rawValue);
                        getProductModelFirebaseStorage(rawValue);

                        break;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, ErrorMessageConstants.ML_KIT_CONN_ERR);
                    Snackbar.make(findViewById(android.R.id.content),
                            ErrorMessageConstants.ML_KIT_CONN_ERR, Snackbar.LENGTH_LONG).show();
                });
    }

    private void getProductFirestore(String id) {
        DocumentReference docRef = products.document(id);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                ProductFirestore productFirestore = documentSnapshot.toObject(ProductFirestore.class);
                productFirestore.setId(id);
                Log.i(TAG, productFirestore.toString());
                currentProductFirestore = productFirestore;

                loaded[0] = true;
                // TODO Do sync block on all???
                if (checkAllLoaded()) {
                    load.setVisibility(Button.VISIBLE);
                } else {
                    load.setVisibility(Button.INVISIBLE);
                }
            } else {
                Log.i(TAG, "Product with id: " + id + " not found. Creating dummy.");
                currentProductFirestore = Util.dummyProductFirestore(id);

                loaded[0] = true;
                if (checkAllLoaded()) {
                    load.setVisibility(Button.VISIBLE);
                } else {
                    load.setVisibility(Button.INVISIBLE);
                }
            }
        })
        .addOnFailureListener(e -> {
            Log.i(TAG, ErrorMessageConstants.FIRESTORE_CONN_ERR);
            Util.showToast(this, true, ErrorMessageConstants.FIRESTORE_CONN_ERR);
        });
    }


    private void getProductModelFirebaseStorage(String rawValue) {
        try {
            File modelFile = File.createTempFile(rawValue, FirebaseStorageConstants.SFB);

            storageReference.child(FirebaseStorageConstants.MODELS + rawValue + "." + FirebaseStorageConstants.SFB)
                    .getFile(modelFile).addOnSuccessListener(taskSnapshot -> {
                Log.i(TAG, "Firebase storage model loaded successfully");
                currentProductModel = modelFile;

                loaded[1] = true;
                if (checkAllLoaded()) {
                    load.setVisibility(Button.VISIBLE);
                } else {
                    load.setVisibility(Button.INVISIBLE);
                }
            }).addOnFailureListener(e1 -> {
                Log.i(TAG, e1.toString());
                Util.showToast(this, true, e1.toString());
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void getProductImageFirebaseStorage(String rawValue) {
        try {
            File imageFile = File.createTempFile(rawValue, FirebaseStorageConstants.PNG);

            storageReference.child(FirebaseStorageConstants.IMAGES + rawValue + "." + FirebaseStorageConstants.PNG)
                    .getFile(imageFile).addOnSuccessListener(taskSnapshot -> {
                Log.i(TAG, "Firebase storage image loaded successfully");
                currentProductImage = imageFile;

                loaded[2] = true;
                if (checkAllLoaded()) {
                    load.setVisibility(Button.VISIBLE);
                } else {
                    load.setVisibility(Button.INVISIBLE);
                }
            }).addOnFailureListener(e1 -> {
                Log.i(TAG, e1.toString());
                Util.showToast(this, true, e1.toString());
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean checkAllLoaded() {
        boolean load = true;

        for (int i = 0; i < loaded.length; i++) {
            if (!loaded[i]) {
                load = false;
                break;
            }
        }

        return load;
    }

    public List<AnchorNode> getLoadedNodes() {
        return loadedNodes;
    }
}
