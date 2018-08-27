package net.cmwang.vision;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

public class VisionFrame implements Parcelable {

    private static final String TAG = "VisionFrame";
    private final int width;
    private final int height;
    private final int rotation;
    private final int facing;
    private final byte[] data;

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
}
