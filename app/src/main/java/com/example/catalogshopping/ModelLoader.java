package com.example.catalogshopping;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.catalogshopping.view.CustomArFragment;
import com.example.catalogshopping.view.MainActivity;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;

public class ModelLoader {

    private static final String TAG = "ModelLoader";

    private final WeakReference<MainActivity> owner;

    public ModelLoader(WeakReference<MainActivity> owner) {
        this.owner = owner;
    }

    public void loadModel(Anchor anchor, String filePath) {
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
                .setSource(owner.get(), Uri.parse(filePath))
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
                        owner.get().getLoadedNodes().add(addNodeToScene(anchor, renderable, activity.getCustomArFragment()));
                    }
                    return null;
                });
    }


    private AnchorNode addNodeToScene(Anchor anchor, ModelRenderable renderable, CustomArFragment fragment) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();

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
