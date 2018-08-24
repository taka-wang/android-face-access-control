package net.cmwang.vision.facedetection;

import android.support.annotation.NonNull;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import net.cmwang.accesscontrol.LivePreviewActivity;
import net.cmwang.vision.GraphicOverlay;
import net.cmwang.vision.VisionFrame;
import net.cmwang.vision.VisionProcessorBase;

import java.io.IOException;
import java.util.List;

public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {
    private static final String TAG = "FaceDetectionProcessor";
    private final FirebaseVisionFaceDetector faceDetector;

    private static final float minFaceSize = 0.15f;

    // Constraints
    private static double minEyeOpenProbability = 0.80;
    private static double maxEulerAngleY = 12.0;
    private static double minEulerAngleY = -12.0;

    private RenderScript rs;

    public FaceDetectionProcessor() {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS) // eye open, smile
                        //.setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setMinFaceSize(minFaceSize)
                        .setTrackingEnabled(true)
                        .build();

        faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        // create render script for nv21 conversion
        rs = RenderScript.create(LivePreviewActivity.getAppContext());
    }

    @Override
    public void stop() {
        try {
            faceDetector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return faceDetector.detectInImage(image);
    }

    @Override
    protected void onSuccess(@NonNull List<FirebaseVisionFace> faces,
                             @NonNull VisionFrame frame,
                             @NonNull GraphicOverlay graphicOverlay) {
        Log.d(TAG, "success");
        // clear overlay before update
        graphicOverlay.clear();

        for (int i = 0; i < faces.size(); ++i) {
            FirebaseVisionFace face = faces.get(i);
            Log.d(TAG, "face: " + i + " Angle: " + face.getHeadEulerAngleY() + " Left: " + face.getLeftEyeOpenProbability() + " Right: " + face.getRightEyeOpenProbability());
            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay);
            graphicOverlay.add(faceGraphic);

            if (face.getRightEyeOpenProbability() > minEyeOpenProbability &&
                    face.getLeftEyeOpenProbability() > minEyeOpenProbability &&
                    (face.getHeadEulerAngleY() < maxEulerAngleY && face.getHeadEulerAngleY() > minEulerAngleY )) {

                // Async Save to SD card
                //new MyTask(frame).execute();

                // update overlay
                faceGraphic.updateFace(face, frame.getCameraFacing());
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    /*
    private class MyTask extends AsyncTask<Void, Void, Void> {
        private VisionFrame frame;

        public MyTask(VisionFrame f) {
            this.frame = f;
        }

        @Override
        protected Void doInBackground(Void... params) {
            saveImage(this.frame);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Play camera sound
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);

            // notify LivePreview activity to update UI
            Intent intent = new Intent();
            intent.setAction(EnrollmentActivity.REFRESH_ACTIVITY);
            EnrollmentActivity.getAppContext().sendBroadcast(intent);
        }
    }

    private static Bitmap FlipRotateBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        //matrix.postScale(-1, 1, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // Save image to SD card
    private void saveImage(VisionFrame frame) {

        int w = frame.getWidth();
        int h = frame.getHeight();

        YuvImage yuvimage = new YuvImage(frame.getData(), ImageFormat.NV21, w, h, null);
        Bitmap outputBitmap = Nv21Image.nv21ToBitmap(rs, yuvimage.getYuvData(), w, h);
        outputBitmap = FlipRotateBitmap(outputBitmap);

        File fileDirectory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Face Detection/" + folderTimeStamp + "/" + EnrollmentActivity.getUserName());
        fileDirectory.mkdirs();

        String ts = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        File outputFile = new File(fileDirectory, EnrollmentActivity.getUserName() + "-" + ts + ".JPEG");

        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputBitmap.compress(Bitmap.CompressFormat.JPEG,100, outputStream);
            //yuvimage.compressToJpeg(new Rect(0, 0, w, h), 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    */

}
