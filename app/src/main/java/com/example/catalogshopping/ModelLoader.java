package com.example.catalogshopping;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.catalogshopping.model.Product;
import com.example.catalogshopping.model.ProductFirestore;
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

    private final WeakReference<MainActivity> owner;

    public ModelLoader(WeakReference<MainActivity> owner) {
        this.owner = owner;
    }

    public void loadModel(Anchor anchor, String modelFilePath, Product product) {
        if (owner.get() == null) {
            Log.d(TAG, "Activity is null.  Cannot load model.");
            return;
        }
//        RenderableSource renderableSource = RenderableSource.builder().setSource(
//                owner.get(),
//                Uri.parse(file.getPath()),
//                RenderableSource.SourceType.)
//                .setScale(0.5f)
//                .setRecenterMode(RenderableSource.RecenterMode.ROOT)
//                .build();


        ModelRenderable.builder()
                .setSource(owner.get(), Uri.parse(modelFilePath))
                .build()
                .handle((renderable, throwable) -> {
                    MainActivity activity = owner.get();
                    if (activity == null) {
                        return null;
                    } else if (throwable != null) {
//                        activity.onException(throwable);
                        onException(throwable, activity);
                    } else {
//                        activity.addNodeToScene(anchor, renderable);
                        owner.get().getLoadedNodes().add(addNodeToScene(anchor, renderable, activity.getCustomArFragment(), activity, product));
                    }
                    return null;
                });
    }


//    private AnchorNode addNodeToScene(Anchor anchor, ModelRenderable renderable, CustomArFragment fragment) {
//        AnchorNode anchorNode = new AnchorNode(anchor);
//        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
//        node.setRenderable(renderable);
//        node.setParent(anchorNode);
//        fragment.getArSceneView().getScene().addChild(anchorNode);
//        node.select();
//
//        return anchorNode;
//    }

    private AnchorNode addNodeToScene(Anchor anchor, ModelRenderable renderable, CustomArFragment fragment, MainActivity mainActivity, Product product) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();


        ViewRenderable.builder()
                .setView(mainActivity, R.layout.product_info_item)
                .build()
                .thenAccept(viewRenderable -> {
                    TransformableNode infoNode = new TransformableNode(fragment.getTransformationSystem());
                    infoNode.setRenderable(viewRenderable);
                    infoNode.setParent(anchorNode);
                    infoNode.setLocalPosition(new Vector3(0f, node.getLocalPosition().y + 0.5f, 0f));

                    infoNode.select();

                    LinearLayout mainLinear = (LinearLayout) viewRenderable.getView();

                    LinearLayout firstLinear = (LinearLayout) mainLinear.getChildAt(0);
                    LinearLayout secondLinear = (LinearLayout) mainLinear.getChildAt(1);

                    ConstraintLayout firstCon = (ConstraintLayout) firstLinear.getChildAt(0);
                    ConstraintLayout secondCon = (ConstraintLayout) secondLinear.getChildAt(0);

                    TextView price = (TextView) firstCon.getViewById(R.id.text_info_productPrice);
                    TextView name = (TextView) firstCon.getViewById(R.id.text_info_productName);
                    TextView description = (TextView) secondCon.getViewById(R.id.text_info_productDescription);
                    Button close = (Button) firstCon.getViewById(R.id.button_info_close);
                    Button addToCart = (Button) secondCon.getViewById(R.id.button_info_addCart);

                    price.setText(String.valueOf(product.getProductFirestore().getPrice()));
                    name.setText(product.getProductFirestore().getName());
                    description.setText(product.getProductFirestore().getDescription());

                    close.setOnClickListener(e -> {
                        removeAnchorNode(anchorNode);
                    });

                    // TODO FIX
                    addToCart.setOnClickListener(e -> {
                        mainActivity.getShoppingCart().addItem(product);
                        mainActivity.getProductsAdapter().notifyItemInserted(mainActivity.getShoppingCart().getProducts().size() - 1);
                    });

                });


        return anchorNode;
    }

    private void onException(Throwable throwable, Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(throwable.getMessage())
                .setTitle("ARCore error!");
        AlertDialog dialog = builder.create();

        dialog.show();
    }

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
