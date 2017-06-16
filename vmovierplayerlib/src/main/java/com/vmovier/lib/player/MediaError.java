package com.vmovier.lib.player;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

// In theory; every errors should be defined in MediaPlayer.java
//   --> http://developer.android.com/intl/zh-cn/reference/android/media/MediaPlayer.OnErrorListener.html
// but some codes are just defined in C; and not accessible in the Java land; see MediaErrors.h
//   --> https://android.googlesource.com/platform/frameworks/av/+/master/include/media/stagefright/MediaErrors.h
//  and also pvmf_return_codes.h
//   --> https://github.com/android/platform_external_opencore/blob/master/pvmi/pvmf/include/pvmf_return_codes.h
//
//  本类只记录经常出现的一些问题. c层面的 error 暂时不考虑

@SuppressWarnings("WeakerAccess, unused")
public class MediaError implements Parcelable {

    // Media errors
    public static final int                             MEDIA_ERROR_BASE = -1000;
    public static final int   ERROR_ALREADY_CONNECTED = MEDIA_ERROR_BASE;
    public static final int   ERROR_NOT_CONNECTED     = MEDIA_ERROR_BASE - 1;
    public static final int   ERROR_UNKNOWN_HOST      = MEDIA_ERROR_BASE - 2;
    public static final int   ERROR_CANNOT_CONNECT    = MEDIA_ERROR_BASE - 3;
    public static final int   ERROR_IO                = MEDIA_ERROR_BASE - 4;
    public static final int   ERROR_CONNECTION_LOST   = MEDIA_ERROR_BASE - 5;
    public static final int   ERROR_MALFORMED         = MEDIA_ERROR_BASE - 7;
    public static final int   ERROR_OUT_OF_RANGE      = MEDIA_ERROR_BASE - 8;
    public static final int   ERROR_BUFFER_TOO_SMALL  = MEDIA_ERROR_BASE - 9;
    public static final int   ERROR_UNSUPPORTED       = MEDIA_ERROR_BASE - 10;
    public static final int   ERROR_END_OF_STREAM     = MEDIA_ERROR_BASE - 11;
    // Not technically an error.
    public static final int   INFO_FORMAT_CHANGED    = MEDIA_ERROR_BASE - 12;
    public static final int   INFO_DISCONTINUITY     = MEDIA_ERROR_BASE - 13;
    public static final int   INFO_OUTPUT_BUFFERS_CHANGED = MEDIA_ERROR_BASE - 14;
    // The following constant values should be in sync with
    // drm/drm_framework_common.h
    public static final int                                              DRM_ERROR_BASE = -2000;
    public static final int   ERROR_DRM_UNKNOWN                        = DRM_ERROR_BASE;
    public static final int   ERROR_DRM_NO_LICENSE                     = DRM_ERROR_BASE - 1;
    public static final int   ERROR_DRM_LICENSE_EXPIRED                = DRM_ERROR_BASE - 2;
    public static final int   ERROR_DRM_SESSION_NOT_OPENED             = DRM_ERROR_BASE - 3;
    public static final int   ERROR_DRM_DECRYPT_UNIT_NOT_INITIALIZED   = DRM_ERROR_BASE - 4;
    public static final int   ERROR_DRM_DECRYPT                        = DRM_ERROR_BASE - 5;
    public static final int   ERROR_DRM_CANNOT_HANDLE                  = DRM_ERROR_BASE - 6;
    public static final int   ERROR_DRM_TAMPER_DETECTED                = DRM_ERROR_BASE - 7;
    public static final int   ERROR_DRM_NOT_PROVISIONED                = DRM_ERROR_BASE - 8;
    public static final int   ERROR_DRM_DEVICE_REVOKED                 = DRM_ERROR_BASE - 9;
    public static final int   ERROR_DRM_RESOURCE_BUSY                  = DRM_ERROR_BASE - 10;
    public static final int   ERROR_DRM_INSUFFICIENT_OUTPUT_PROTECTION = DRM_ERROR_BASE - 11;
    public static final int   ERROR_DRM_LAST_USED_ERRORCODE            = DRM_ERROR_BASE - 11;
    public static final int   ERROR_DRM_VENDOR_MAX                     = DRM_ERROR_BASE - 500;
    public static final int   ERROR_DRM_VENDOR_MIN                     = DRM_ERROR_BASE - 999;

    // Exo Error
    public static final int   EXO_ERROR_BASE = -3000;
    public static final int   EXO_ERROR_QUERYING_DECODERS     = EXO_ERROR_BASE - 1;
    public static final int   EXO_ERROR_NO_SECURE_DECODER     = EXO_ERROR_BASE - 2;
    public static final int   EXO_ERROR_NO_DECODER            = EXO_ERROR_BASE - 3;
    public static final int   EXO_ERROR_INSTANTIATING_DECODER = EXO_ERROR_BASE - 4;

    // Custom Error
    public static final int ERROR_CUSTOM = -4000;
    public static final int ERROR_UNKNOWN = ERROR_CUSTOM - 1;
    public static final int ERROR_PREPARE = ERROR_CUSTOM - 2;
    public static final int ERROR_METERED_NETWORK = ERROR_CUSTOM - 3; // 移动网络错误

    //
    private static final int ERROR_MIN = -20000;

    private final int errorCode;
    // 出错之前的Bundle, 用来恢复状态使用.
    private Bundle mRestoreBundle;

    public MediaError(int code) {
        if (code < ERROR_MIN) {
            code = ERROR_UNKNOWN;  // sometimes maybe -2147483648.
        }
        this.errorCode = code;
    }


    public Bundle getRestoreBundle() {
        return mRestoreBundle;
    }

    public void setRestoreBundle(Bundle mRestoreBundle) {
        this.mRestoreBundle = mRestoreBundle;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        switch (errorCode) {
            case ERROR_ALREADY_CONNECTED:
                return "ERROR_ALREADY_CONNECTED";
            case ERROR_NOT_CONNECTED:
                return "ERROR_NOT_CONNECTED";
            case ERROR_UNKNOWN_HOST:
                return "ERROR_UNKNOWN_HOST";
            case ERROR_CANNOT_CONNECT:
                return "ERROR_CANNOT_CONNECT";
            case ERROR_IO:
                return "ERROR_IO";
            case ERROR_CONNECTION_LOST:
                return "ERROR_CONNECTION_LOST";
            case ERROR_MALFORMED:
                return "ERROR_MALFORMED";
            case ERROR_OUT_OF_RANGE:
                return "ERROR_OUT_OF_RANGE";
            case ERROR_BUFFER_TOO_SMALL:
                return "ERROR_BUFFER_TOO_SMALL";
            case ERROR_UNSUPPORTED:
                return "ERROR_UNSUPPORTED";
            case ERROR_END_OF_STREAM:
                return "ERROR_END_OF_STREAM";
            case ERROR_DRM_UNKNOWN:
                return "ERROR_DRM_UNKNOWN";
            case ERROR_DRM_NO_LICENSE:
                return "ERROR_DRM_NO_LICENSE";
            case ERROR_DRM_LICENSE_EXPIRED:
                return "ERROR_DRM_LICENSE_EXPIRED";
            case ERROR_DRM_SESSION_NOT_OPENED:
                return "ERROR_DRM_SESSION_NOT_OPENED";
            case ERROR_DRM_DECRYPT_UNIT_NOT_INITIALIZED:
                return "ERROR_DRM_DECRYPT_UNIT_NOT_INITIALIZED";
            case ERROR_DRM_DECRYPT:
                return "ERROR_DRM_DECRYPT";
            case ERROR_DRM_CANNOT_HANDLE:
                return "ERROR_DRM_CANNOT_HANDLE";
            case ERROR_DRM_TAMPER_DETECTED:
                return "ERROR_DRM_TAMPER_DETECTED";
            case ERROR_DRM_NOT_PROVISIONED:
                return "ERROR_DRM_NOT_PROVISIONED";
            case ERROR_DRM_DEVICE_REVOKED:
                return "ERROR_DRM_DEVICE_REVOKED";
            case ERROR_DRM_RESOURCE_BUSY:
                return "ERROR_DRM_RESOURCE_BUSY";
            case ERROR_DRM_INSUFFICIENT_OUTPUT_PROTECTION:
                return "ERROR_DRM_INSUFFICIENT_OUTPUT_PROTECTION || ERROR_DRM_LAST_USED_ERRORCODE";
            case ERROR_DRM_VENDOR_MAX:
                return "ERROR_DRM_VENDOR_MAX";
            case ERROR_DRM_VENDOR_MIN:
                return "ERROR_DRM_VENDOR_MIN";
            // exo
            case EXO_ERROR_QUERYING_DECODERS:
                return "EXO_ERROR_QUERYING_DECODERS";
            case EXO_ERROR_NO_SECURE_DECODER:
                return "EXO_ERROR_NO_SECURE_DECODER";
            case EXO_ERROR_NO_DECODER:
                return "EXO_ERROR_NO_DECODER";
            case EXO_ERROR_INSTANTIATING_DECODER:
                return "EXO_ERROR_INSTANTIATING_DECODER";
            // Custom Error
            case ERROR_UNKNOWN:
                return "ERROR_UNKNOWN";
            case ERROR_PREPARE:
                return "ERROR_PREPARE";
            case ERROR_METERED_NETWORK:
                return "ERROR_METERED_NETWORK";
            default:
                return "ERROR_UNKNOWN";
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.errorCode);
        dest.writeBundle(this.mRestoreBundle);
    }

    protected MediaError(Parcel in) {
        this.errorCode = in.readInt();
        this.mRestoreBundle = in.readBundle(Bundle.class.getClassLoader());
    }

    public static final Creator<MediaError> CREATOR = new Creator<MediaError>() {
        @Override
        public MediaError createFromParcel(Parcel source) {
            return new MediaError(source);
        }

        @Override
        public MediaError[] newArray(int size) {
            return new MediaError[size];
        }
    };
}
