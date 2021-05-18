package com.example.custommodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.custommodel.ml.CustomModel;
import com.example.custommodel.ml.Modeltf;
import com.example.custommodel.ml.NewDrinksModel;
import com.example.custommodel.ml.TeamDrinksModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE=100;
    private static final int STORAGE_PERMISSION_CODE=101;
    private static final int SELECT_PICTURE = 200;
    private ImageView imgView;
    private Button select, predict, camera;
    private TextView tv;
    private Bitmap img;
    private float[] results = new float[9];
    private String answer = "";
    float max = 0;
    int max_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);

        imgView = (ImageView) findViewById(R.id.imageView);
        select = (Button) findViewById(R.id.btn_select);
        predict = (Button) findViewById(R.id.btn_predict);
        tv = (TextView) findViewById(R.id.tv_result);
        camera = (Button) findViewById(R.id.btn_camera);

        select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                imageChooser();
            }
        });
        camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera_intent, CAMERA_PERMISSION_CODE);
            }
        });
        predict.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
//                img = Bitmap.createScaledBitmap(img, 150, 150, true);
                BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
                img = drawable.getBitmap();
                img = Bitmap.createScaledBitmap(img, 150, 150, true);
                try {
                    TeamDrinksModel model = TeamDrinksModel.newInstance(getApplicationContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);

                    //TensorImage tensorImage= new TensorImage(DataType.FLOAT32);
                    //tensorImage.load(img);
                    //converBitmapToByteBuffer를 통해서 bitmap정보를 정규화를 진행해서 byteBuffer에 넣어준다.
                    ByteBuffer byteBuffer = convertBitmapToByteBuffer(img);
                    inputFeature0.loadBuffer(byteBuffer);

                    // Runs model inference and gets result.
                    TeamDrinksModel.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Releases model resources if no longer used.
                    model.close();
                    max = 0;
                    max_index = 0;
                    for(int i=0; i<9; i++){

                        results[i] = outputFeature0.getFloatArray()[i];
                        if (max < results[i]){
                            max = results[i];
                            max_index = i;
                        }
                    }


                    switch(max_index){
                        case 0:
                            answer = "cider";
                            break;
                        case 1:
                            answer = "coke";
                            break;
                        case 2:
                            answer = "fanta";
                            break;
                        case 3:
                            answer = "milkis";
                            break;
                        case 4:
                            answer = "monster";
                            break;
                        case 5:
                            answer = "mtdew";
                            break;
                        case 6:
                            answer = "pepsi";
                            break;
                        case 7:
                            answer = "soda";
                            break;
                        case 8:
                            answer = "sprite";
                            break;

                    }
                    tv.setText(answer+"일 확률 "+ String.valueOf(max*100) + "%");
                } catch (IOException e) {
                    Log.d("error", e.toString());
                }
            }
        });
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bp) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES*150*150*3);
        imgData.order(ByteOrder.nativeOrder());
        //입력된 이미지를 원하는 사이즈의 bitmap으로 변환 filter: true를 통해 저화질 사진 보정가능
        Bitmap bitmap = Bitmap.createScaledBitmap(bp,150,150,true);
        int [] intValues = new int[150*150];
        //bitmap으로 부터 픽셀 정보를 가져와서 intValues에 넣어줌.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convert the image to floating point.
        int pixel = 0;

        for (int i = 0; i < 150; ++i) {
            for (int j = 0; j < 150; ++j) {
                final int val = intValues[pixel++];
                //0~255의 값은 8비트의 값 0xFF는 11111111을 의미한다.
                //val의 값으로 들어오는 값은 R,G,B 세가지 필터에서 8개 비트씩 총 24개의 비트가 입력으로 들어온다.
                //오른쪽으로 16번 shift하면 제일 앞에있던 8개 비트가 남게되는데 이를 0xFF와 and연산을 8비트의 결과로 나오게 해준다.
                imgData.putFloat(((val>> 16) & 0xFF) / 255.f);
                imgData.putFloat(((val>> 8) & 0xFF) / 255.f);
                imgData.putFloat((val & 0xFF) / 255.f);
            }
        }
        return imgData;
    }
    public void checkPermission(String permission, int requestCode){
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {permission}, requestCode);
        }
        else{
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }
    void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"),SELECT_PICTURE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode == STORAGE_PERMISSION_CODE){

            if(grantResults.length > 0 &&  grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                imgView.setImageURI(data.getData());

                Uri uri = data.getData();
                if (null != uri){
                    imgView.setImageURI(uri);
                }

            }
            if (requestCode == CAMERA_PERMISSION_CODE) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imgView.setImageBitmap(photo);
            }
        }
    }
//    void connectServer(Bitmap bitmap){
//        String postUrl = "http://222.108.117.234:5000";
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//        byte[] byteArray = stream.toByteArray();
//        RequestBody postBodyImage = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
//                .build();
//        postRequest(postUrl, postBodyImage);
//    }
}