package com.vmovier.lib.view;

@SuppressWarnings("WeakerAccess")
public interface OnControlViewListener {
    void onVisibilityChange(boolean isVisible);
    void onLockStateChange(boolean isLock);
}
