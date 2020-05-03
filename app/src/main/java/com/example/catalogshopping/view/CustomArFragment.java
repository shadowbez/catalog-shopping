package com.example.catalogshopping.view;

import android.Manifest;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class CustomArFragment extends ArFragment {

    @Override
    public String[] getAdditionalPermissions() {
        String[] additionalPermissions = super.getAdditionalPermissions();
        int permissionLength = additionalPermissions != null ? additionalPermissions.length : 0;
        String[] permissions = new String[permissionLength + 1];
        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (permissionLength > 0) {
            System.arraycopy(additionalPermissions, 0, permissions, 1, additionalPermissions.length);
        }
        return permissions;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        config.setFocusMode(Config.FocusMode.AUTO);
//        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        return config;
    }
}
