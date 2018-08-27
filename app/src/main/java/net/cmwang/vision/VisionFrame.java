package net.cmwang.vision;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v8.renderscript.RenderScript;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import net.cmwang.accesscontrol.LivePreviewActivity;

import java.io.ByteArrayOutputStream;

import io.github.silvaren.easyrs.tools.Nv21Image;

import static io.github.silvaren.easyrs.tools.Resize.resize;

public class VisionFrame implements Parcelable {

    private RenderScript rs = RenderScript.create(LivePreviewActivity.getAppContext());

    private static final String TAG = "VisionFrame";
    private final int width;
    private final int height;
    private final int rotation;
    private final int facing;
    private final byte[] data;

    public static final Creator<VisionFrame> CREATOR = new Creator<VisionFrame>() {
        @Override
        public VisionFrame createFromParcel(Parcel in) {
            return new VisionFrame(in);
        }

        @Override
        public VisionFrame[] newArray(int size) {
            return new VisionFrame[size];
        }
    };

    public byte[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotation() {
        return rotation;
    }

    public int getCameraFacing() {
        return facing;
    }

    private VisionFrame(byte[] data, int width, int height, int rotation, int facing) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.facing = facing;
    }

    public VisionFrame(Parcel parcel) {
        this.data = new byte[parcel.readInt()];
        parcel.readByteArray(data);

        this.width = parcel.readInt();
        this.height = parcel.readInt();
        this.rotation = parcel.readInt();
        this.facing = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(data.length);
        parcel.writeByteArray(data);

        parcel.writeInt(width);
        parcel.writeInt(height);
        parcel.writeInt(rotation);
        parcel.writeInt(facing);
    }

    public static class Builder {
        private byte [] data;
        private int width;
        private int height;
        private int rotation;
        // default
        private int cameraFacing = GraphicOverlay.LENS_FACING_BACK;

        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setRotation(int rotation) {
            // convert rotation to FirebaseVisionImage rotation
            if (this.cameraFacing == GraphicOverlay.LENS_FACING_FRONT) {
                rotation = (360 - rotation) % 360; // compensate for it being mirrored
            }
            switch (rotation) {
                case 0:
                    rotation = FirebaseVisionImageMetadata.ROTATION_0;
                    break;
                case 90:
                    rotation = FirebaseVisionImageMetadata.ROTATION_90;
                    break;
                case 180:
                    rotation = FirebaseVisionImageMetadata.ROTATION_180;
                    break;
                case 270:
                    rotation = FirebaseVisionImageMetadata.ROTATION_270;
                    break;
                default:
                    Log.e(TAG, "Bad rotation value: " + rotation);
            }
            this.rotation = rotation;
            return this;
        }

        public Builder setCameraFacing(int facing) {
            cameraFacing = facing;
            return this;
        }

        public VisionFrame build() {
            return new VisionFrame(data, width, height, rotation, cameraFacing);
        }
    }

    private static Bitmap FlipRotateBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        //matrix.postScale(-1, 1, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // convert frame to base64 string
    public String toBase64() {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, this.width, this.height, null);
        Bitmap outputBitmap = Nv21Image.nv21ToBitmap(rs, yuvimage.getYuvData(), this.width, this.height);

        // resize
        outputBitmap = resize(rs, outputBitmap, 640, 400);

        outputBitmap = FlipRotateBitmap(outputBitmap);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        //Log.d(TAG, "base64: " + encoded);
        return encoded;
    }
}
