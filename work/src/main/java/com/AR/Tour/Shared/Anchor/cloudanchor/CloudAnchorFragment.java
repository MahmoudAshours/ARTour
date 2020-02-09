package com.AR.Tour.Shared.Anchor.cloudanchor;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.AR.Tour.Shared.Anchor.cloudanchor.helpers.CloudAnchorManager;
import com.AR.Tour.Shared.Anchor.cloudanchor.helpers.FirebaseManager;
import com.AR.Tour.Shared.Anchor.cloudanchor.helpers.ResolveDialogFragment;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Session;
import com.AR.Tour.Shared.Anchor.cloudanchor.helpers.SnackbarHelper;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;

import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

/**
 * Main Fragment for the App.
 *
 * <p>This is where the AR Session and the Cloud Anchors are managed.
 */
public class CloudAnchorFragment extends ArFragment {

    private Scene arScene;
    private AnchorNode anchorNode;
    private ModelRenderable andyRenderable;
    private FirebaseManager firebaseManager;
    private Button resolveButton;
    private final CloudAnchorManager cloudAnchorManager = new CloudAnchorManager();
    private final SnackbarHelper snackbarHelper = new SnackbarHelper();

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        return config;
    }

    public void onAttach(Context context) {
        super.onAttach(context);

        ModelRenderable.builder()
                .setSource(context, R.raw.gandam)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable);

        firebaseManager = new FirebaseManager(context);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate from the Layout XML file.
        View rootView = inflater.inflate(R.layout.cloud_anchor_fragment, container, false);
        LinearLayout arContainer = rootView.findViewById(R.id.ar_container);

        // Call the ArFragment's implementation to get the AR View.
        View arView = super.onCreateView(inflater, arContainer, savedInstanceState);
        arContainer.addView(arView);

        Button clearButton = rootView.findViewById(R.id.clear_button);
        clearButton.setOnClickListener(v -> onClearButtonPressed());

        resolveButton = rootView.findViewById(R.id.resolve_button);
        resolveButton.setOnClickListener(v -> onResolveButtonPressed());

        arScene = getArSceneView().getScene();
        arScene.addOnUpdateListener(frameTime -> cloudAnchorManager.onUpdate());
        setOnTapArPlaneListener((hitResult, plane, motionEvent) -> onArPlaneTap(hitResult));
        return rootView;
    }

    private synchronized void onArPlaneTap(HitResult hitResult) {
        if (anchorNode != null) {
            // Do nothing if there was already an anchor in the Scene.
            return;
        }
        Anchor anchor = hitResult.createAnchor();
        setNewAnchor(anchor);

        // The next line is the new addition.
        resolveButton.setEnabled(false);

        snackbarHelper.showMessage(getActivity(), "Now hosting anchor...");
        cloudAnchorManager.hostCloudAnchor(
                getArSceneView().getSession(), anchor, this::onHostedAnchorAvailable);
    }

    private synchronized void onClearButtonPressed() {
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners();

        // The next line is the new addition.
        resolveButton.setEnabled(true);

        setNewAnchor(null);
    }

    private synchronized void onResolveButtonPressed() {
        ResolveDialogFragment dialog = ResolveDialogFragment.createWithOkListener(
                this::onShortCodeEntered);
        ;
        dialog.show(getFragmentManager(), "Resolve");
    }

    // Modify the renderables when a new anchor is available.
    private synchronized void setNewAnchor(@Nullable Anchor anchor) {
        if (anchorNode != null) {
            // If an AnchorNode existed before, remove and nullify it.
            arScene.removeChild(anchorNode);
            anchorNode = null;
        }
        if (anchor != null) {
            if (andyRenderable == null) {
                // Display an error message if the renderable model was not available.
                Toast toast = Toast.makeText(getContext(), "Andy model was not loaded.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            // Create the Anchor.
            anchorNode = new AnchorNode(anchor);
            arScene.addChild(anchorNode);

            // Create the transformable andy and add it to the anchor.
            TransformableNode andy = new TransformableNode(getTransformationSystem());
            andy.setParent(anchorNode);
            andy.setRenderable(andyRenderable);
            andy.select();
        }
    }

    private synchronized void onHostedAnchorAvailable(Anchor anchor) {
        Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            String cloudAnchorId = anchor.getCloudAnchorId();
            firebaseManager.nextShortCode(shortCode -> {
                if (shortCode != null) {
                    firebaseManager.storeUsingShortCode(shortCode, cloudAnchorId);
                    snackbarHelper
                            .showMessage(getActivity(), "Cloud Anchor Hosted. Short code: " + shortCode);
                } else {
                    // Firebase could not provide a short code.
                    snackbarHelper
                            .showMessage(getActivity(), "Cloud Anchor Hosted, but could not "
                                    + "get a short code from Firebase.");
                }
            });
            setNewAnchor(anchor);
        } else {
            snackbarHelper.showMessage(getActivity(), "Error while hosting: " + cloudState.toString());
        }
    }

    private synchronized void onShortCodeEntered(int shortCode) {
        firebaseManager.getCloudAnchorId(shortCode, cloudAnchorId -> {
            if (cloudAnchorId == null || cloudAnchorId.isEmpty()) {
                snackbarHelper.showMessage(
                        getActivity(),
                        "A Cloud Anchor ID for the short code " + shortCode + " was not found.");
                return;
            }
            resolveButton.setEnabled(false);
            cloudAnchorManager.resolveCloudAnchor(
                    getArSceneView().getSession(),
                    cloudAnchorId,
                    anchor -> onResolvedAnchorAvailable(anchor, shortCode));
        });
    }

    private synchronized void onResolvedAnchorAvailable(Anchor anchor, int shortCode) {
        Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            snackbarHelper.showMessage(getActivity(), "Cloud Anchor Resolved. Short code: " + shortCode);
            setNewAnchor(anchor);
        } else {
            snackbarHelper.showMessage(
                    getActivity(),
                    "Error while resolving anchor with short code "
                            + shortCode
                            + ". Error: "
                            + cloudState.toString());
            resolveButton.setEnabled(true);
        }
    }
}
