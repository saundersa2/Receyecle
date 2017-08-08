package com.receyecle.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CapturedImage extends Activity {

    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/rounded_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/retrained_labels.txt";

    private Classifier classifier;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private ResultsView resultsView;
    private String pictureType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_image);


        pictureType = getIntent().getStringExtra("pictureType");
        final String fileName = getIntent().getStringExtra("filename");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(fileName, options);



        previewWidth = bitmap.getWidth();
        previewHeight = bitmap.getHeight();



        final Bitmap croppedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        ImageView pic = (ImageView) findViewById(R.id.imageView1);

        classifier =
                TensorFlowImageClassifier.create(
                        getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);

        //resultsView = (ResultsView) findViewById(R.id.results);



        final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);

        // Create testing data
        List<ListItem> list = new ArrayList<ListItem>();


        System.out.println(results.size());

        for(int i = 0; i< results.size(); i++){
            ListItem item1 = new ListItem();
            System.out.println("i= " + i + " " + results.get(i).getTitle());
            item1.number = i + 1 + ". ";
            item1.classifier = results.get(i).getTitle();
            list.add(item1);

        }

        // Create ListItemAdapter
        ListItemAdapter adapter;
        adapter = new ListItemAdapter(CapturedImage.this, 0, list);

        // Assign ListItemAdapter to ListView
        ListView listView = (ListView)findViewById(R.id.results);
        listView.setAdapter(adapter);



        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        pic.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true));

       // pic.setImageBitmap(bitmap);
        //resultsView.setResults(results);

        listView.setOnItemClickListener(new ItemList());

        TextView sendImage = (TextView) findViewById(R.id.send_photo);
        sendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File filelocation = null;

                if(pictureType.equals("camera")) {
                    String[] fileSmall = fileName.split("/");
                    int index = fileSmall.length;
                    filelocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), "RecEYEcle/" + fileSmall[index - 1]);
                    //File filelocation = new File("/Internal_storage/Pictures/camera2Image/IMAGE_20170717_153947_1210428744.JPG");
                    Log.v("FILENAME CAM", fileName);
                    Log.v("FILE LOCATION CAM", filelocation + "");
                }else if(pictureType.equals("gallery")){
                    filelocation = new File(fileName);
                    Log.v("FILENAME GAL", fileName);
                    Log.v("FILE LOCATION GAL", filelocation + "");
                }

                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("application/image");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"receyecle@gmail.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Unidentifiable Image Detected");
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "This image went unrecognized by the RecEYEcle application. \n Feel free to add a description of your image to help the developers classify your image and make RecEYEcle the best that it can be! Thank you!");
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(filelocation));
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));

            }
        });
    }

    class ItemList implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){
            ViewGroup vg = (ViewGroup) view;
            TextView tv = (TextView)vg.findViewById(R.id.classifier);
            final String classifier_chosen = tv.getText().toString();
            //Query to determine if classifier has multiple materials or just one
            Response.Listener<String> responseListener = new Response.Listener<String>(){

                @Override
                public void onResponse(String response) {

                    try {
                        JSONArray jsonResponse = new JSONArray(response);
                        Log.d("LENGTH:", "" + jsonResponse.length());
                        int length = jsonResponse.length();

                        //if multi materials
                        if (length > 1){

                            AlertDialog.Builder builderSingle = new AlertDialog.Builder(CapturedImage.this);
                            builderSingle.setTitle("Select Material:");

                            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(CapturedImage.this, android.R.layout.select_dialog_singlechoice);

                            for(int i =0; i <length; i++) {

                                arrayAdapter.add(jsonResponse.getJSONObject(i).getString("material"));
                            }


                            builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String strName = arrayAdapter.getItem(which);

                                    Response.Listener<String> responseListener = new Response.Listener<String>(){

                                        @Override
                                        public void onResponse(String response) {

                                            try {
                                                JSONArray jsonResponse = new JSONArray(response);
                                                Log.d("LENGTH:", "" + jsonResponse.length());

                                                String classifier = jsonResponse.getJSONObject(0).getString("class_category");
                                                String material = jsonResponse.getJSONObject(0).getString("material");
                                                String recycleable = jsonResponse.getJSONObject(0).getString("recycleable");
                                                String instructions = jsonResponse.getJSONObject(0).getString("instruct");
                                                String other_op = jsonResponse.getJSONObject(0).getString("other_options");
                                                int classifier_id = jsonResponse.getJSONObject(0).getInt("classifier_id");

                                                String concatClass = material + " " + classifier;

                                                Log.v("mult" , "YAY");
                                                Log.v("recycleable", recycleable);
                                                Log.v("instructions", "B" + instructions + "E");
                                                Log.v("other_op", "B" + other_op + "E");

                                                Intent intent = new Intent(CapturedImage.this, HowToRecycle.class);

                                                intent.putExtra("classifier_id", classifier_id);
                                                intent.putExtra("classifier", concatClass);
                                                intent.putExtra("recycleable", recycleable);
                                                intent.putExtra("instructions", instructions);
                                                intent.putExtra("other_op", other_op);

                                                CapturedImage.this.startActivity(intent);

                                            }catch (JSONException e) {
                                                e.printStackTrace();
                                            } }
                                    };

                                    MaterialRequest materialRequest = new MaterialRequest(classifier_chosen, strName, responseListener);
                                    RequestQueue queue = Volley.newRequestQueue(CapturedImage.this);
                                    queue.add(materialRequest);

                                }
                            });
                            builderSingle.show();


                        }
                        //if not multi_mater
                        else{
                            //get the classifier_id, class_category, recycleable, instruct, other_options
                            String classifier = jsonResponse.getJSONObject(0).getString("class_category");
                            String recycleable = jsonResponse.getJSONObject(0).getString("recycleable");
                            String instructions = jsonResponse.getJSONObject(0).getString("instruct");
                            String other_op = jsonResponse.getJSONObject(0).getString("other_options");
                            int classifier_id = jsonResponse.getJSONObject(0).getInt("classifier_id");
                            Log.v("not mult" , "YAY");
                            Log.v("recycleable", recycleable);
                            Log.v("instructions", "B" + instructions + "E");
                            Log.v("other_op", "B" + other_op + "E");

                            Intent intent = new Intent(CapturedImage.this, HowToRecycle.class);

                            intent.putExtra("classifier_id", classifier_id);
                            intent.putExtra("classifier", classifier);
                            intent.putExtra("recycleable", recycleable);
                            intent.putExtra("instructions", instructions);
                            intent.putExtra("other_op", other_op);

                            CapturedImage.this.startActivity(intent);



                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            CapturedImageRequest capturedImageRequest = new CapturedImageRequest(classifier_chosen, responseListener);
            RequestQueue queue = Volley.newRequestQueue(CapturedImage.this);
            queue.add(capturedImageRequest);


        }


    }

    //On back pressed overrides last activity (in the case that it was uploaded image)
    // so it goes back to camera screen rather than the temp gallery activity
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(CapturedImage.this, Camera2VideoImageActivity.class));
        finish();

    }
}
