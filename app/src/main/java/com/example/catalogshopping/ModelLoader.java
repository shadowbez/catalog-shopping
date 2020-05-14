package com.example.catalogshopping;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.catalogshopping.model.Product;
import com.example.catalogshopping.view.CustomArFragment;
import com.example.catalogshopping.view.MainActivity;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;

public class ModelLoader {

    private static final String TAG = "ModelLoader";

    /**
     * WeakReference of MainActivity in order to safely gather activity reference and use properties.
     */
    private final WeakReference<MainActivity> owner;

    public ModelLoader(WeakReference<MainActivity> owner) {
        this.owner = owner;
    }

    public void loadModelAndInformation(Anchor anchor, String modelFilePath, Product product) {
        if (owner.get() == null) {
            Log.d(TAG, "Model not loaded as activity is null");
            return;
        }
//        RenderableSource renderableSource = RenderableSource.builder().setSource(
//                owner.get(),
//                Uri.parse(file.getPath()),
//                RenderableSource.SourceType.)
//                .setScale(0.5f)
//                .setRecenterMode(RenderableSource.RecenterMode.ROOT)
//                .build();

        // BUILD MODEL FIRST
        ModelRenderable.builder()
                .setSource(owner.get(), Uri.parse(modelFilePath))
                .build()
                .handle((renderable, throwable) -> {
                    MainActivity activity = owner.get();
                    if (activity == null) {
                        return null;
                    } else if (throwable != null) {
                        showException(throwable, activity);
                    } else {
                        // STORE REFERENCE TO LOADED MODELS SO THAT THEY ARE CLEARED AFTERWARDS
                        owner.get().getLoadedNodes().add(addNodeToScene(anchor, renderable, activity.getCustomArFragment(), activity, product));
                    }
                    return null;
                });
    }

    private AnchorNode addNodeToScene(Anchor anchor, ModelRenderable renderable, CustomArFragment fragment, MainActivity mainActivity, Product product) {
        // PREPARE INFO NODE
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();

        // BUILD INFO RENDERABLE
        ViewRenderable.builder()
                .setView(mainActivity, R.layout.product_info_item)
                .build()
                .thenAccept(viewRenderable -> {
                    // LOAD INFO NODE
                    TransformableNode infoNode = new TransformableNode(fragment.getTransformationSystem());
                    infoNode.setRenderable(viewRenderable);
                    infoNode.setParent(anchorNode);
                    infoNode.setLocalPosition(new Vector3(0f, node.getLocalPosition().y + 0.5f, 0f));

                    // Fetch all UI elements from layout. The "old fashioned" way is used because of a glitch within Sceneform
                    LinearLayout mainLinear = (LinearLayout) viewRenderable.getView();

                    LinearLayout firstLinear = (LinearLayout) mainLinear.getChildAt(0);
                    LinearLayout secondLinear = (LinearLayout) mainLinear.getChildAt(1);

                    LinearLayout firstOneLin = (LinearLayout) firstLinear.getChildAt(0);
                    LinearLayout firstTwoLin = (LinearLayout) firstLinear.getChildAt(1);

                    LinearLayout secondOneLin = (LinearLayout) secondLinear.getChildAt(0);
                    LinearLayout secondTwoLin = (LinearLayout) secondLinear.getChildAt(1);


                    TextView name = (TextView) firstOneLin.getChildAt(1);
                    TextView price = (TextView) firstTwoLin.getChildAt(1);

                    TextView description = (TextView) secondOneLin.getChildAt(1);

                    Button close = (Button) secondTwoLin.getChildAt(0);
                    Button addToCart = (Button) secondTwoLin.getChildAt(1);

                    // Setup UI elements
                    price.setText(String.valueOf(product.getProductFirestore().getPrice()));
                    name.setText(product.getProductFirestore().getName());
                    description.setText(product.getProductFirestore().getDescription());

                    close.setOnClickListener(e -> {
                        removeAnchorNode(anchorNode);
                    });

                    addToCart.setOnClickListener(e -> {
                        int existed = mainActivity.getShoppingCart().addItem(product);
                        if (existed < 0) {
                            mainActivity.getProductsAdapter().notifyItemInserted(mainActivity.getShoppingCart().getProducts().size() - 1);
                        }
                    });

                });


        return anchorNode;
    }

    /**
     * Show exception if node cannot be processed/loaded properly
     * @param throwable
     * @param context
     */
    private void showException(Throwable throwable, Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(throwable.getMessage())
                .setTitle("ARCore error");
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    /**
     * Remove node from scene safely.
     * @param anchorNode the reference to the node to be removed.
     */
    public void removeAnchorNode(AnchorNode anchorNode) {
        MainActivity activity = owner.get();
        if (activity != null && anchorNode != null) {
            activity.getCustomArFragment().getArSceneView().getScene().removeChild(anchorNode);
            anchorNode.getAnchor().detach();
            anchorNode.setParent(null);
            anchorNode = null;

            Log.i(TAG, "AnchorNode removed");
        } else {
            Log.i(TAG, "AnchorNode was null");
        }
    }
}
