package com.example.end_project.etc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GetAuth_MIC extends AppCompatActivity {

    private static final int REQ_MIC_PERMISSION =  2003; // 요청 실행 후 전달 받을 임의 코드


    public GetAuth_MIC() {
    }

    public void Request_MIC_Permission(Activity activity, Context context) {

        if ( Build.VERSION.SDK_INT >= 23 ) {
            int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO); // 권한이 이미 있는지 확인

            if (permission == PackageManager.PERMISSION_DENIED) {
                // 권한 없어서 요청
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(activity, "앱 실행을 위해서는 권한을 설정해야 합니다", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC_PERMISSION);
                } else { // 권한 있음
                    //startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_IMAGE_CAPTURE);
                    Activate_MIC();
                    System.out.println("마이크 권한 있음. ");
                }
            }
        }
    }

    @Override // 요청에 대한 응답
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_MIC_PERMISSION:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // 권한 허가된 경우 처리
                    //Toast.makeText(this, "앱 실행을 위한 권한이 설정 되었습니다", Toast.LENGTH_LONG).show();
                    System.out.println("카메라 권한 설정 완료");

                } else {

                    // 권한 거절된 경우 처리
                    //Toast.makeText(this, "앱 실행을 위한 권한이 취소 되었습니다", Toast.LENGTH_LONG).show();
                    System.out.println("카메라 권한 설정 취소");
                }

                break;

        }

    }

    public void Activate_MIC() {

    }
}