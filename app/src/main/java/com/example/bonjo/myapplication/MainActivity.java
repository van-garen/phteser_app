package com.example.bonjo.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class MainActivity extends AppCompatActivity {

    private Uri resultUri;
    private static final int REQUEST_CAMERA = 1001;
    private final static int REQUEST_PERMISSION = 1002;
    private static final int REQUEST_GALLERY = 1004;
    private String filePath;
    private Uri cameraUri;
    private File cameraFile;

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!OpenCVLoader.initDebug()){
            Log.i("OpenCV", "Failed");
        }else{
            Log.i("OpenCV", "successfully built !");
        }
        setViews();
    }

    private void setViews(){
        imageView = findViewById(R.id.imageView);
        Button buttonCamera = (Button)findViewById(R.id.button);
        buttonCamera.setOnClickListener(onClick_button);
        Button buttonGallery = (Button)findViewById(R.id.button2);
        buttonGallery.setOnClickListener(onClick_button);
        Button buttonCheck = (Button)findViewById(R.id.button3);
        buttonCheck.setOnClickListener(onClick_button);

    }


    private View.OnClickListener onClick_button = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            switch(v.getId()){

                case R.id.button :

                    Log.d("debug","button1");
                    showCamera();
                    break;
                case R.id.button2 :
                    Log.d("debug","button2");
                    showGallery();
                    break;

                case R.id.button3:
                    Log.d("debug","button3"+resultUri.getPath());

                    pictureDecision(resultUri);
                    break;

            }
        }
    };

    private void showCamera(){


        if (Build.VERSION.SDK_INT >= 23) {
            Log.d("debug","23>SDK");
            checkPermission();

        }
        else {
            cameraIntent(); //cameraIntentというIntentを返す
            Log.d("debug","23<SDK");
        }
    }

    private void showGallery(){

        //ギャラリー用のINTENT作成
        Intent intentGallery;
        if(Build.VERSION.SDK_INT  < 19){
            intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
            intentGallery.setType("image/*");

        }else {
            intentGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
            intentGallery.setType("image/png");

        }
/*
        //chooserにギャラリーのIntentとCameraのIntentを登録
        Intent intent = Intent.createChooser(intentGallery,"画像選択");
        if(intentCamera != null){
            intent.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[] {intentCamera});
        }
*/
        startActivityForResult(intentGallery,REQUEST_GALLERY);
    }


    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        Log.d("debug","onActivityResult");
        //super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == REQUEST_GALLERY || requestCode == REQUEST_CAMERA){
            if(resultCode != RESULT_OK) {
                //キャンセル
                Log.d("debug","CANCEL");
                return;
            }
            Log.d("debug",Integer.toString(requestCode));
            //画像を取得
            if(requestCode == REQUEST_CAMERA) {
                resultUri = cameraUri;
                imageView.setImageURI(resultUri);
                Log.d("debug","REQUEST_CAMERA");
                imageView = (ImageView) findViewById(R.id.imageView);
            }else{
                resultUri = data.getData();
                // ギャラリーへスキャンを促す
                MediaScannerConnection.scanFile(
                        this,
                        new String[]{resultUri.getPath()},
                        new String[]{"image/png"},
                        null
                );
                Log.d("debug","REQUEST_GALLERY");
                imageView.setImageURI(resultUri);
                Log.d("debug","ファイルクリック時"+resultUri.getPath());
            }

            if(resultUri == null) {
                // 取得失敗
                Toast.makeText(this, "Error.Try again.", Toast.LENGTH_LONG).show();
                return;
            }

        }

    }

    private void cameraIntent(){
     //保存先フォルダーを作成する場合
        File cameraFolder = new File(
          Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES
          ),"IMG");
        cameraFolder.mkdirs();
        /*
        //保存先のフォルダーをカメラに指定した場合
        File cameraFolder = new File(
        Environment.getExternalStrangePublicDirectory(
        Environment.DIRECTORY_DCIM),"Camera")
         */


        //保存ファイル名
        String fileName = new SimpleDateFormat("ddHHmmss", Locale.JAPAN).format(new Date());
        filePath = String.format("%s/%s.png", cameraFolder.getPath(),fileName);
        //filePath = cameraFolder.getPath() +"/" + fileName + ".jpg";
        Log.d("debug","filePath"+filePath);

        //capture画像のファイルパス
        cameraFile = new File(filePath);
        //cameraUri =Uri.fromFile(cameraFile);

        cameraUri = FileProvider.getUriForFile(
                MainActivity.this,
                getApplicationContext().getPackageName()+".fileprovider",cameraFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        Log.d("debug","cameraIntent");
        startActivityForResult(intent,REQUEST_CAMERA);

    }

    //パーミッションのチェック
    private void checkPermission(){
        //すでに許可済み
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            cameraIntent();
            Log.d("debug","checkPermission");
        }

        else{
            requestPermission();
        }
    }

    private void requestPermission(){
        Log.d("debug","requestPermission");
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            ActivityCompat.requestPermissions(MainActivity.this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_PERMISSION);
        }else{
            Toast toast = Toast.makeText(this,"Camera function is disables",Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,},REQUEST_PERMISSION);
        }
    }

    //画像判定

    private void pictureDecision(Uri resultUri){
        //画像が選択させていないとき

        if (resultUri == null){
            Toast.makeText(this, "画像を選択してください。", Toast.LENGTH_LONG).show();
            return;
         //画像が選択されたときの処理
        }else {
            //画像の読み込み

            // Decode it for real
            //BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            //bmpFactoryOptions.inJustDecodeBounds = false;

            //imageFilePath image path which you pass with intent
            Bitmap bp = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

            Mat img_scr = new Mat();
            Utils.bitmapToMat(bp,img_scr);
            Log.d("debug",43+resultUri.getPath());

            Mat img_hsv = Mat.zeros(img_scr.width(), img_scr.height(), CV_8UC3);

            List<Mat> channels= new ArrayList<>(3);
            //Log.d("debug","4"+bp.toString());

            if(img_scr.empty()==true){
                Log.d("debug","4.5");
                Toast.makeText(this, "画像が対応していません。", Toast.LENGTH_LONG).show();
                imageView.setImageBitmap(bp);
                return;
            }
            //変換
            Imgproc.cvtColor(img_scr, img_hsv, Imgproc.COLOR_BGR2HSV);
            Log.d("debug","5");
            //画像ファイルの高幅読み取り
            int height = img_hsv.height();
            int width = img_hsv.width();

            Log.d("debug","たかさん"+height+"のりさん"+width);


            List<Double> pixels = new ArrayList<>();
            List<Double> saturation = new ArrayList<>();

            double[] data = new double[3];



            for(int x = height/4; x < 3*height/4 ; x++){
                for(int y = width/4 ; y < 3*width/4 ; y++){
                    data = img_hsv.get(y,x);
                    pixels.add(data[0]);
                    saturation.add(data[1]);
                }
            }
            double sum = 0 ;
            for(int i = 0 ; i < saturation.size() ; i++) {
                sum += saturation.get(i);
            }
            double mean = sum/saturation.size();





            if(mean > 65.0){
                System.out.println("E");
                Log.d("debug","Eeffective"+mean);
            }else{
                System.out.println("NO");
                Log.d("debug","No_Eeffective"+mean);
            }






            return;
        }

    }

}
