/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.YoloV4Classifier;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import android.speech.tts.TextToSpeech;

import androidx.annotation.StringRes;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final int TF_OD_API_INPUT_SIZE = 416;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "yolov4-tiny-416-fp32.tflite";

    private static final String TF_OD_API_LABELS_FILE = "labels_v3.txt";

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    private int status;

    private static int count = 1;



    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    YoloV4Classifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED);
//            detector = TFLiteObjectDetectionAPIModel.create(
//                    getAssets(),
//                    TF_OD_API_MODEL_FILE,
//                    TF_OD_API_LABELS_FILE,
//                    TF_OD_API_INPUT_SIZE,
//                    TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    static int ttsState = 0;
    public Boolean mutex;
    static TextToSpeech tts;

    public Bitmap cropBitmap(Bitmap bitmap, float width, float height, float x, float y) {
        float originWidth = bitmap.getWidth();
        float originHeight = bitmap.getHeight();

        // ???????????? crop ??? ????????? ?????? ( x : left, y : top )


       // if (originWidth > width) { // ???????????? ????????? view ??? ???????????? ??????..
       //     x = (float) ((originWidth - width)/2.0);
       // }

//        if (originHeight > height) { // ???????????? ????????? view ??? ???????????? ??????..
 //           y = (float) ((originHeight - height)/2.0);
   //     }

        Bitmap cropedBitmap = Bitmap.createBitmap(bitmap, (int)x, (int)y, (int)width, (int)height);
        return cropedBitmap;
    }


    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        Matrix matrix_90 = new Matrix(); // ????????? 90??? ?????????
        matrix_90.postRotate(90);

        ArrayList<Bitmap> bmp_images = new ArrayList<Bitmap>();
        ArrayList<String> bmp_names = new ArrayList<String>();

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        Log.e("CHECK", "run: " + results.size());

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();


                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);

                                // Todo : month, day ????????????  api ????????????

                                if((result.getTitle().contains("month") || result.getTitle().contains("day") || result.getTitle().contains("time")) && count == 0 )
                                {
                                    count = 1;
                                    StringBuffer response = new StringBuffer();

                                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(cropBitmap(rgbFrameBitmap,location.right - location.left, location.bottom - location.top, location.left, location.top),
                                            (int) (location.right - location.left), (int) (location.bottom - location.top), true);
                                    // ???????????? ?????????????????? ??????????????????.
                                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix_90, true);

                                    bmp_images.add(rotatedBitmap); // ????????? ??????
                                    bmp_names.add(result.getTitle()); // ????????? ?????? ??????
                                    System.out.println(count);



                                } else if(result.getTitle().contains("ExpirationDate") && count == 0)
                                {
                                    count = 1;
                                    StringBuffer response = new StringBuffer();

                                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(cropBitmap(rgbFrameBitmap,location.right - location.left, location.bottom - location.top, location.left, location.top),
                                            (int) (location.right - location.left), (int) (location.bottom - location.top), true);
                                    // ???????????? ?????????????????? ??????????????????.
                                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix_90, true);

                                    response = OCRGeneralAPIDemo(rotatedBitmap);
                                    System.out.println(count);
                                    ExpirationDate(response); // ???????????? ????????????
                                }
                            }
                        }

                        StringBuffer response = new StringBuffer();
                        try {
                            response = EasyOCRAPI(bmp_images, bmp_names);
                            ExpirationDate(response); // ???????????? ????????????
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");
                                    }
                                });
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }

    // Todo : API GET
    public void get(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000); //????????? ???????????? Timeout ?????? ??????
            con.setReadTimeout(5000); // InputStream ?????? ?????? Timeout ?????? ??????
            //con.addRequestProperty("x-api-key", String.valueOf(R.string.EasyOCR_KEY)); //key??? ??????

            con.setRequestMethod("GET");



            //URLConnection??? ?????? doOutput ???????????? ????????? ????????? ????????????. URL ????????? ???????????? ????????? ??? ??????.
            // URL ????????? ??????????????? ??????????????? ?????? DoOutput ???????????? true??? ????????????, ????????? ?????? ????????? false??? ???????????? ??????. ???????????? false??????.

            con.setDoOutput(false);

            StringBuilder sb = new StringBuilder();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //Stream??? ??????????????? ?????? ???????????? ??????.
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                System.out.println("" + sb.toString());
            } else {
                System.out.println(con.getResponseMessage());
            }

        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }


    public StringBuffer post(String strUrl, String jsonMessage){
        StringBuffer response = new StringBuffer();

        try {
            URL url = new URL(strUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000); //????????? ???????????? Timeout ?????? ??????
            con.setReadTimeout(5000); // InputStream ?????? ?????? Timeout ?????? ??????
            //con.addRequestProperty("x-api-key", String.valueOf(R.string.EasyOCR_KEY)); //key??? ??????

            con.setRequestMethod("POST");

            //json?????? message??? ??????????????? ??? ???
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoInput(true);
            con.setDoOutput(true); //POST ???????????? OutputStream?????? ?????? ???????????? ??????
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);

            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(jsonMessage); //json ????????? message ??????
            wr.flush();

            StringBuilder sb = new StringBuilder();

            // ????????? ?????? ????????? ??????.
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line; // inputLine
                while ((line = br.readLine()) != null) {
                    response.append(line);//sb.append(line).append("\n");
                }
                br.close();
                System.out.println(response);
                return response;
                //System.out.println("" + sb.toString());
            } else {
                System.out.println(con.getResponseMessage());
            }
        } catch (Exception e){
            System.err.println(e.toString());
        }
        return response;
    }



    // Todo : EasyOCR call
    public synchronized StringBuffer EasyOCRAPI(ArrayList<Bitmap> bitmaps, ArrayList<String> result_titles) throws JSONException {
        StringBuffer response = new StringBuffer();

        String strUrl = "127.0.0.1/:8080";
        String jsonMessage;

        /*
        * json ??????
        * 1. result_title : ?????? ?????? ????????? ?????? ??????????????? (month, day, time)
        * 2. timestamp : ?????? ??????
        * 3. image : ????????? ?????????
        *   3-1. format : ????????? ???????????? ?????????
        *   3-2. data : ????????? ?????????
        *   3-3 name : ????????? ??????
        *
        * */

        // host - json ?????????
        JSONObject json = new JSONObject();
        json.put("", "V2");
        //json.put("requestId", UUID.randomUUID().toString());
        json.put("timestamp", System.currentTimeMillis());

        // image - json
        JSONObject image = new JSONObject();

        // bitmap - jpeg ??????
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Resources res= getResources();

        // ?????? ????????? ??????
        for (int i = 0; i < bitmaps.size(); i++) {

            bitmaps.get(i).compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            byte[] image2 = outStream.toByteArray();
            String profileImageBase64 = Base64.encodeToString(image2, 0);
            // ?????? ???

            image.put("format", "jpg");
            image.put("data", profileImageBase64); // buffer

            String name = result_titles.get(i) + "_" + Long.toString(timestamp);
            image.put("name", name);
        }
        jsonMessage = json.toString();

        response = post(strUrl, jsonMessage);

        /* response json ??????
        * 1. confidence_score : ????????? ?????? ?????? ??????
        * 2. pred : ?????? ??????
        * 3. time : ?????? ??????
        * 4. img_name : ????????? ???
        * */

        return response;
    }

    // *** Naver OCR
    public synchronized StringBuffer OCRGeneralAPIDemo(Bitmap bitmap) {

        // API invoke URL
        String apiURL = "https://cf667635821d4e27a149417a936140aa.apigw.ntruss.com/custom/v1/8069/4ee8c59db77aec20ccac405e431d4e27f0345fe1b12edb62281840e6670452cc/general";

        // API secretKey
        String secretKey = "Sk16UE5tUFltdlpyVmlCc1hGYlRaaWpUbHNrZWR5cHg=";

        try {
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            JSONObject json = new JSONObject();
            json.put("version", "V2");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", System.currentTimeMillis());
            JSONObject image = new JSONObject();
            image.put("format", "jpg");
            //image.put("url", "https://kr.object.ncloudstorage.com/ocr-ci-test/sample/1.jpg"); // image should be public, otherwise, should use data

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Resources res= getResources();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

            byte[] image2 = outStream.toByteArray();
            String profileImageBase64 = Base64.encodeToString(image2, 0);

            image.put("data", profileImageBase64); // buffer
            image.put("name", "demo");
            JSONArray images = new JSONArray();
            images.put(image);
            json.put("images", images);
            String postParams = json.toString();

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            System.out.println(response);


            return response;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static void copyInputStreamToFile(InputStream inputStream, File file) {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setCount(int count) {
        DetectorActivity.count = count;
    }

    // ??????????????? TTS??? ??????
    public void ExpirationDate(StringBuffer response) {
        // stringBuffer??? JsonArray??? ?????????
        String day1 = null;
        try {

            org.json.JSONObject jsonObject = new JSONObject(String.valueOf(response));
            JSONParser jParser = new JSONParser();

            org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray)  jParser.parse(String.valueOf(jsonObject.get("images")));

            org.json.simple.JSONObject jsonObject1 = (org.json.simple.JSONObject) jParser.parse(String.valueOf(jsonArray.get(0)));

            org.json.simple.JSONArray jsonArray2 = (org.json.simple.JSONArray)  jParser.parse(String.valueOf(jsonObject1.get("fields")));

            int month, day, time, min;
            month =0;
            day = 0;
            time = 0;
            min = 0;

            for (int i = 0; i < jsonArray2.size(); ++i) {
                org.json.simple.JSONObject jo = (org.json.simple.JSONObject) jsonArray2.get(i); // ????????? list??? ?????????
                String inferText = String.valueOf(jo.get("inferText"));
                String confidence = String.valueOf(jo.get("inferConfidence"));
                float con_f = Float.parseFloat(confidence);



                if (inferText.contains(".") && con_f >= 0.9) {
                    String[] arr = inferText.split("\\.", -1);

                    if(Integer.parseInt(arr[0]) > month)
                        month = Integer.parseInt(arr[0]);

                    if(Integer.parseInt(arr[1]) > day)
                        day = Integer.parseInt(arr[1]);



                } else if (inferText.contains(":") && con_f >= 0.9) {
                    String[] arr2 = inferText.split(":");

                    if(Integer.parseInt(arr2[0]) > time)
                        time = Integer.parseInt(arr2[0]);

                    if(Integer.parseInt(arr2[1]) > min)
                        min = Integer.parseInt(arr2[1]);
                    //day1 = day1 + String.valueOf() + "??? " + String.valueOf(Integer.parseInt(arr2[1])) + "???";

                }
            }
            day1 = String.valueOf(month) + "??? " + String.valueOf(day) + "???";

            if(time > 0 && min >0)
                day1 = day1 + String.valueOf(time) + "???" + String.valueOf(min) + "???";

            day1 = day1 + "???????????????.";

            speakOut(day1);
            //ttsState = 0;
            count = 1;


        } catch (ParseException | JSONException e) {
            e.printStackTrace();
        }



    }

    public static void execPython(String[] command) throws IOException, InterruptedException {
        CommandLine commandLine = CommandLine.parse(command[0]);
        for (int i = 1, n = command.length; i < n; i++) {
            commandLine.addArgument(command[i]);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(outputStream);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        int result = executor.execute(commandLine);
        System.out.println("result: " + result);
        System.out.println("output: " + outputStream.toString());

    }


}
