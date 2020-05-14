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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The main screen of the program.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /**
     * Drawable for notifying user when a surface is valid or not
     */
    private CrosshairDrawable crosshair = new CrosshairDrawable();
    /**
     * Boolean on whether ARCore is searching for a surface.
     */
    private boolean isTracking;
    /**
     * Boolean on whether a detected valid surface is being looked at.
     */
    private boolean isHitting;

    /**
     * Reference for custom fragment.
     */
    private CustomArFragment fragment;
    /**
     * SCAN button
     */
    private Button scanButton;
    /**
     * LOAD button
     */
    private Button loadButton;
    /**
     * CLEAR button
     */
    private Button clearButton;

    /**
     * Firestore database root
     */
    private FirebaseFirestore db;
    /**
     * Reference of a collection in firestore
     */
    private CollectionReference products;
    /**
     * Reference of Cloud Storage
     */
    private StorageReference storageReference;
    /**
     * Detector object for ML Kit VISION API
     */
    private FirebaseVisionBarcodeDetector detector;
    /**
     * Barcode scanning options - currently set to only detect EAN 13
     */
    private FirebaseVisionBarcodeDetectorOptions options =
            new FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                            FirebaseVisionBarcode.FORMAT_EAN_13)
                    .build();

    /**
     * Boolean array for hiding/appearing the LOAD button when certain conditions are met.
     */
    private boolean[] loaded = new boolean[3];
    /**
     * Reference of the current product which was scanned
     */
    private ProductFirestore currentProductFirestore;
    /**
     * Reference of the current file of the image product which was scanned
     */
    private File currentProductImage;
    /**
     * Reference of the current file of the model product which was scanned
     */
    private File currentProductModel;

    /**
     * Reference for all AR Nodes placed in the scene. Used for removing them afterwards.
     */
    private List<AnchorNode> loadedNodes;

    /**
     * Class which loads models and images
     */
    private ModelLoader modelLoader;
    /**
     * ShoppingCart data storage
     */
    private ShoppingCart shoppingCart;
    /**
     * Adapter for RecyclerView
     */
    private ProductsAdapter productsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFirebaseElements();

        loadedNodes = new CopyOnWriteArrayList<>();
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

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    public ProductsAdapter getProductsAdapter() {
        return productsAdapter;
    }

    public List<AnchorNode> getLoadedNodes() {
        return loadedNodes;
    }

    /**
     * Add speficic model to the scene depending.
     * @param file
     */
    public void addModelToScene(File file) {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    Product product = new Product(currentProductFirestore, currentProductImage, currentProductModel);
                    modelLoader.loadModelAndInformation(hit.createAnchor(), file.getPath(), product);
                    break;
                }
            }
        }
    }

    /**
     * Initialize shopping cart functionality
     */
    private void initShoppingCart() {
        shoppingCart = new ShoppingCart();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_main_cart);
        productsAdapter = new ProductsAdapter(shoppingCart, new WeakReference<>(this));

        recyclerView.setAdapter(productsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    /**
     * Initialize all firebase components
     */
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

    /**
     * Initialize UI
     */
    private void initUI() {
        scanButton = (Button) findViewById(R.id.button_main_scan);
        loadButton = (Button) findViewById(R.id.button_main_load);
        clearButton = (Button) findViewById(R.id.button_main_clear);

        clearButton.setOnClickListener(e -> {
            for (AnchorNode elem : loadedNodes) {
                modelLoader.removeAnchorNode(elem);
                loadedNodes.remove(elem);
            }
        });

        scanButton.setOnClickListener(e -> {
            createAndSendPhoto();
        });

        if (!checkAllLoaded()) {
            loadButton.setVisibility(Button.INVISIBLE);
        }
        loadButton.setOnClickListener(e -> {
            addModelToScene(currentProductModel);
        });
    }

    /**
     * Update crosshair dedpending on valid or invalid surface
     */
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

    /**
     * Update the tracking status
     * @return true if its tracking, false otherwise
     */
    private boolean updateTrackingStatus() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    /**
     * Update the hit test for surfaces
     * @return true if it's hitting, false otherwise
     */
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

    /**
     * Helper method to get the screen center dimensions
     * @return the point of centre
     */
    private android.graphics.Point getScreenCenter() {
        View view = findViewById(android.R.id.content);
        return new android.graphics.Point(view.getWidth() / 2, view.getHeight() / 2);
    }

    /**
     * Method to create a screenshot of the ArFragment and then send it to cloud for analysis.
     */
    private void createAndSendPhoto() {
        // LOAD BUTTON DISAPPEARS - LOADING PROCESS
        synchronized (loaded) {
            loaded[0] = false;
            loaded[1] = false;
            loaded[2] = false;
            loadButton.setVisibility(Button.INVISIBLE);
        }

        ArSceneView view = fragment.getArSceneView();

        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create different thread to make the pixel copying process
        final HandlerThread handlerThread = new HandlerThread("copy");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                // Successful copying - send bitmap to firebase
                actionPhotoFirebase(bitmap);
            } else {
                Util.showToast(this, true, "Failed to copyPixels: " + copyResult);
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));

    }

    /**
     * Method to send a bitmap to firebase for analysis and then make a specific action
     * @param bitmap the bitmap to be sent
     */
    private void actionPhotoFirebase(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(barcodes -> {
                    for (FirebaseVisionBarcode barcode: barcodes) {
                        Rect bounds = barcode.getBoundingBox();
                        Point[] corners = barcode.getCornerPoints();

                        String rawValue = barcode.getRawValue();

                        Util.showSnackbar(findViewById(android.R.id.content), false, rawValue);
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

    /**
     * Method which gets the product information from firestore
     * @param id
     */
    private void getProductFirestore(String id) {
        DocumentReference docRef = products.document(id);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                ProductFirestore productFirestore = documentSnapshot.toObject(ProductFirestore.class);
                productFirestore.setId(id);
                Log.i(TAG, productFirestore.toString());
                currentProductFirestore = productFirestore;

                updateAndCheckLoadVisibility(0);
            } else {
                Log.i(TAG, "Product with id: " + id + " not found. Creating dummy.");
                currentProductFirestore = Util.dummyProductFirestore(id);

                updateAndCheckLoadVisibility(0);
            }
        })
        .addOnFailureListener(e -> {
            Log.i(TAG, ErrorMessageConstants.FIRESTORE_CONN_ERR);
            Util.showToast(this, true, ErrorMessageConstants.FIRESTORE_CONN_ERR);
        });
    }

    /**
     * Method which gets the model from Cloud Storage. If not found then the default one will be fetched.
     * @param rawValue the ID of the product
     */
    private void getProductModelFirebaseStorage(String rawValue) {
        try {
            File modelFile = File.createTempFile(rawValue, FirebaseStorageConstants.SFB);

            storageReference.child(FirebaseStorageConstants.MODELS + rawValue + "." + FirebaseStorageConstants.SFB)
                    .getFile(modelFile).addOnSuccessListener(taskSnapshot -> {
                Log.i(TAG, "Firebase storage model loaded successfully");
                currentProductModel = modelFile;

                updateAndCheckLoadVisibility(1);
            }).addOnFailureListener(e1 -> {
                Log.i(TAG, e1.toString());
                Util.showToast(this, true, "Loading default model");

                storageReference.child(FirebaseStorageConstants.MODELS + "default" + "." + FirebaseStorageConstants.SFB)
                        .getFile(modelFile).addOnSuccessListener(taskSnapshot -> {
                    Log.i(TAG, "Firebase storage default model loaded successfully");
                    currentProductModel = modelFile;

                    updateAndCheckLoadVisibility(1);
                }).addOnFailureListener(e2 -> {
                    Log.i(TAG, e1.toString());
                    Util.showToast(this, true, e2.toString());
                });
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method which gets the image from Cloud Storage. If not found then the default one will be fetched.
     * @param rawValue the ID of the product
     */
    private void getProductImageFirebaseStorage(String rawValue) {
        try {
            File imageFile = File.createTempFile(rawValue, FirebaseStorageConstants.PNG);

            storageReference.child(FirebaseStorageConstants.IMAGES + rawValue + "." + FirebaseStorageConstants.PNG)
                    .getFile(imageFile).addOnSuccessListener(taskSnapshot -> {
                Log.i(TAG, "Firebase storage image loaded successfully");
                currentProductImage = imageFile;

                updateAndCheckLoadVisibility(2);
            }).addOnFailureListener(e1 -> {
                Log.i(TAG, e1.toString());
                Util.showToast(this, true, "Loading default image");

                storageReference.child(FirebaseStorageConstants.IMAGES + "default" + "." + "png")
                        .getFile(imageFile).addOnSuccessListener(taskSnapshot -> {
                    Log.i(TAG, "Firebase storage image loaded successfully");
                    currentProductImage = imageFile;

                    updateAndCheckLoadVisibility(2);
                }).addOnFailureListener(e2 -> {
                    Log.i(TAG, e1.toString());
                    Util.showToast(this, true, e2.toString());
                });
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Helper method to check if all information, model and image are loaded in order for the
     * LOAD button to appear
     * @return true if everything is loaded in memory, false if not
     */
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

    /**
     * Prevent race conditions from different firebase threads.
     * @param num Thread number
     */
    private void updateAndCheckLoadVisibility(int num) {
        synchronized (loaded) {
            loaded[num] = true;
            if (checkAllLoaded()) {
                loadButton.setVisibility(Button.VISIBLE);
            } else {
                loadButton.setVisibility(Button.INVISIBLE);
            }
        }
    }
}
