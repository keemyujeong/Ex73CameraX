package com.kyjsoft.ex73camerax;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    PreviewView previewView;
    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 액티비티의 영역을 외부 상태표시줄이랑 물리버튼 영역까지 확대하는 플래그(설정) 값이 있음
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        previewView =findViewById(R.id.preview_view);
        iv = findViewById(R.id.iv);

        findViewById(R.id.fab_image_capture).setOnClickListener(view -> clickCapture()); // 카메라가 액티비티 생명주기랑 연결된 후 버튼 누르면 캡쳐본 뜨게

        // 동적 퍼미션들
        String[] permissions = null; //28 버전이상인거랑 낮은 거랑 구분 할라고 미리 안주는 거임.-> 28 이상은 write 안줘도 됨.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ) permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        else permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if( checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_DENIED) { // 원래는 3개 다 해야함 -> 구글에 다 물어보는 메소드 있음.
            requestPermissions(permissions, 10);
        }else{
            // 프리뷰를 시작한다.
            startPreview();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==10) {

            for(int grantresult: grantResults){
                if(grantresult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "카메라 사용 기능 불가", Toast.LENGTH_SHORT).show();
                    finish(); // 앱이 꺼지고
                    return; // 끝남
                }
            }

            startPreview(); // 여기까지 오면 퍼미션이 다 허용되었다는 것이므로

        }
    }

    void startPreview(){

        // 1. 카메라의 셔터 조리개가 open/close되는 순간이 액티비티의 라이프사이클과 같도록 연결하는 작업 -> 앱이 꺼지고 카메라도 꺼지게
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this); // 연결결과를 들을 수 있는 녀석
        // 2. preview 준비가 가능함을 알려주는 리스너(별도 스레드로 작업은 하되 -> main이 작업하게 하는 거임.)
//        listenableFuture.addListener( () -> { // 별도 스레드는 파라미터가 없어서 걍 ()

//        }, ContextCompat.getMainExecutor(this));
        listenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // 카메라 기능 객체 가져오기 . get은 try catch문있어야함.
                    ProcessCameraProvider cameraProvider = listenableFuture.get();

                    // 프리뷰 객체 생성
                    Preview preview = new Preview.Builder().build();
                    // 프리뷰 객체가 사용할 고속 버퍼 뷰(SurfaceView, 화면을 빠르게 잡아내는 뷰) 설정
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    // 디바이스에 있는 카메라 중 하나를 선택
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    cameraProvider.unbindAll(); // 혹시 기존에 연결되어있는 카메라 기능들을 제거하고 내 앱 생명주기에 맞춰서 카메라 preview를 제어하도록

                    // bind 하기 전에 이미지 캡쳐를 하는 객체 생성하기
                    imageCapture = new ImageCapture.Builder().build();

                    // preview랑 imageCapture 둘다 생명 주기 에 붙여줌
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageCapture);




                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));


    }

    // 이미지 캡쳐 객체 참조변수(안드로이드 권장) -> 얘 객체를 preview하는 곳에서 생성함.
    ImageCapture imageCapture = null;

    void clickCapture(){
        if(imageCapture == null) return;

        // 캡쳐한 사진을 파일로 저장할 파일명만들기 -> 날짜를 이용한
        String filename = new SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis()); // 자바 방법
        // 사진 정보가 저장된 MediaStore(미디어 저장소)의 DB에 넣을 값들(1개의 record)를 가진 객체
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename); // db 특정 하나의 칸에 filename이라고 값 넣기
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");// 확장자 타입 정해주기
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Image");

        // 이미지 캡쳐가 찍은 사진을 저장하는 경로를 관리하는 객체 만들기
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();
        // 다른 앱에서 준거(db)를 쓸 때 getContentResolver() -> 원래 있는 db에서 가져오는 거니까


        // 이미지 캡쳐에게 사진을 취득하라고 명령하기
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Toast.makeText(MainActivity.this, "촬영 성공", Toast.LENGTH_SHORT).show();

                // 저장될 파일의 uri
                Uri uri = outputFileResults.getSavedUri();
                Glide.with(MainActivity.this).load(uri).into(iv);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(MainActivity.this, "캡쳐 실패 :"+ exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    } // 실디바이스밖에 안됨.

}