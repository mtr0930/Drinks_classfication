package com.example.custommodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private static final int CAMERA_PERMISSION_CODE=100;
    private static final int STORAGE_PERMISSION_CODE=101;
    private static final int SELECT_PICTURE = 200;
    // Text를 음성으로 출력하기 위한 모듈 TTS
    TextToSpeech TTS;
    // Firebase Firestore Cloud와 연결
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ImageView imgView;
    private Button select, predict, camera;
    private TextView tv;
    private Bitmap img;
    private float[] results = new float[9];
    private String answer = "";
    float max = 0;
    int max_index = 0;
    String filePath;
    File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Text To Speech 모듈 객체 선언
        TTS = new TextToSpeech(this, this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 권한 요청함수(권한내용: 카메라, 내부 저장소 접근)
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
        
        // 버튼, 이미지뷰 객체 
        imgView = (ImageView) findViewById(R.id.imageView);
        select = (Button) findViewById(R.id.btn_select);
        predict = (Button) findViewById(R.id.btn_predict);
        tv = (TextView) findViewById(R.id.tv_result);
        camera = (Button) findViewById(R.id.btn_camera);


        // select 버튼 클릭시 동작.
        select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                imageChooser();
            }
        });
        // camera 버튼 클릭시 동작.
        camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // 촬영한 이미지 Image view에 출력
                dispatchTakePictureIntent();
                try {
                    imageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("image", e.toString());
                }
            }
        });
        
        // predict 버튼 클릭시 동작.
        predict.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // image를 Bitmap형식으로 변환.
                BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
                img = drawable.getBitmap();
                img = Bitmap.createScaledBitmap(img, 150, 150, true);
                try {
                    // 여기서부터는 tflite 모델에 사진을 입력으로 넣고 결과를 받는 과정
                    TeamDrinksModel model = TeamDrinksModel.newInstance(getApplicationContext());

                    // 모델의 입력 형식에 맞는 입력 객체 생성
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);


                    //converBitmapToByteBuffer를 통해서 bitmap정보를 정규화를 진행해서 byteBuffer에 넣어준다.
                    ByteBuffer byteBuffer = convertBitmapToByteBuffer(img);
                    inputFeature0.loadBuffer(byteBuffer);

                    // 모델을 실행시키고 결과를 받음
                    TeamDrinksModel.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // 모델을 더이상 사용하지 않으므로 close함
                    model.close();
                    // 확률이 가장 높은 것을 인덱스로 가져옴 max는 확률을 비교하기 위한 변수, max_index는 최대값 인덱스 저장위한 변수
                    max = 0;
                    max_index = 0;
                    for(int i=0; i<9; i++){

                        results[i] = outputFeature0.getFloatArray()[i];
                        if (max < results[i]){
                            max = results[i];
                            max_index = i;
                        }
                    }

                    // 최대값 인덱스에 해당하는 음료수 정보 출력
                    // searchDrink함수는 firebase db에서 예측한 음료수(answer)정보를 가져와서 출력하는 
                    switch(max_index){
                        case 0:
                            answer = "cider";
                            searchDrink(answer);
                            break;
                        case 1:
                            answer = "coke";
                            searchDrink(answer);
                            break;
                        case 2:
                            answer = "fanta";
                            searchDrink(answer);
                            break;
                        case 3:
                            answer = "milkis";
                            searchDrink(answer);
                            break;
                        case 4:
                            answer = "monster";
                            searchDrink(answer);
                            break;
                        case 5:
                            answer = "mtdew";
                            searchDrink(answer);
                            break;
                        case 6:
                            answer = "pepsi";
                            searchDrink(answer);
                            break;
                        case 7:
                            answer = "soda";
                            searchDrink(answer);
                            break;
                        case 8:
                            answer = "sprite";
                            searchDrink(answer);
                            break;

                    }

                } catch (IOException e) {
                    Log.d("error", e.toString());
                }


            }
        });


    }// Oncreate끝.


    // 모델의 출력결과로 나온 음료수 정보를 db를 조회해서 가져옴
    // 현재는 db 테이블에 음료수정보 name(음료수 이름), flavor(맛)의 2개 필드값이 있음
    private void searchDrink(String answer){
        // firebase db를 DocumentReference 형식으로 동적으로 입력받은 answer에 해당하는 정보를 가져옴
        DocumentReference drinksRef = db.collection("drinks").document(answer);
        drinksRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    if(doc.exists()){
                        Log.d("Document", doc.getData().toString());
                        Log.d("Document", doc.get("name").toString());
                        // db 쿼리를 통해 가져온 name(음료수 이름)과 확률을 출력
                        tv.setText("음료수 이름  " + doc.get("name").toString() + "\n" + "확률 "+ String.valueOf(max*100) + "%");
                        speak();
                    }else{
                        Log.d("Document", "No data");
                    }
                }
            }
        });
    }
    
    // camera버튼 클릭시 실행되는 함수
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, CAMERA_PERMISSION_CODE);
    }
    
    // 사진 촬영한 image file을 생성.(저장은 따로 구현하지 않았음)
    private File createImageFile() throws IOException {
        // 이미지 파일 이름 생성
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // image의 파일 경로를 가져오고 image를 반환
        filePath = image.getAbsolutePath();
        return image;
    }

    // tflite모델 입력을 위해 정규화 bitmap을 bytebuffer로 변환하고 정규화 실행
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bp) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES*150*150*3);
        imgData.order(ByteOrder.nativeOrder());
        //입력된 이미지를 원하는 사이즈의 bitmap으로 변환 filter: true를 통해 저화질 사진 보정가능
        Bitmap bitmap = Bitmap.createScaledBitmap(bp,150,150,true);
        int [] intValues = new int[150*150];
        //bitmap으로 부터 픽셀 정보를 가져와서 intValues에 넣어줌.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // 이미지 픽셀값 정규화
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
    
    // 권한 요청 함수
    public void checkPermission(String permission, int requestCode){
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {permission}, requestCode);
        }
        else{
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }
    
    // select 버튼 클릭시 실행되는 함수
    void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"),SELECT_PICTURE);
    }
    
    // 권한 요청이 허용되면 toast message 출력
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
    
    // 접근 권한이 있고, 사진을 성공적으로 intent로 가져왔으면 실행되는 부분
    // 촬영하거나 갤러리에서 선택한 이미지를 화면에 출력하는 함수
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
                img = photo;
                imgView.setImageBitmap(photo);
            }
        }
    }
    
    // TTS 객체를 initialization하기 위한 함수
    @Override
    public void onInit(int i) {
        // 객체가 성공적으로 초기화 됬다면 실행
        if(i == TextToSpeech.SUCCESS){
            int result = TTS.setLanguage(Locale.KOREAN);
            TTS.setSpeechRate(1);
            TTS.setPitch(1);
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.d("TTS", "Language not supported");
            }
            else {
                predict.setEnabled(true);
                speak();
            }
        }
        else{
            Log.d("TTS", "Initialization failed");
        }
    }
    // Text를 음성으로 출력하는 함수
    private void speak(){
        String message = tv.getText().toString();
        Log.d("TTS", message);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            TTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}// Activity 끝
