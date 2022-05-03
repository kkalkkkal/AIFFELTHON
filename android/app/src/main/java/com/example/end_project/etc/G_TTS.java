package com.example.end_project.etc;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import android.speech.tts.TextToSpeech;

public class G_TTS extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private Intent recognizerIntent;
    private final int RESULT_SPEECH = 1000;
    final int PERMISSION = 1;
    private SpeechRecognizer speech;
    private TextView textView;
    private Button sttbtn, ttsbtn;
    public TextToSpeech tts = new TextToSpeech(this, this::onInit);
    private EditText ttsText;
    private int status;

    public G_TTS() {

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        tts = new TextToSpeech(this, this);

    }

    // 구글 TTS api
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void Information() {    //tts speakout 함수 : 입력된 텍스트를 음성으로 출력하는 함수


        String str = "";
        try {
            //파일 객체 생성
            File file = new File("raw/inform.txt");
            //입력 스트림 생성
            FileReader filereader = new FileReader(file);
            BufferedReader bufrd = new BufferedReader(filereader) ;
            str = bufrd.readLine();
            /*int singleCh = 0;
            while ((singleCh = filereader.read()) != -1) {
                System.out.print((char) singleCh);
            }*/
            bufrd.close() ;
            filereader.close();
        } catch (FileNotFoundException e) {
            // TODO: handle exception
        } catch (IOException e) {
            System.out.println(e);
        }
        CharSequence text = str;//ttsText.getText(); // 여기에 원하는 것
        tts.setPitch((float) 0.6);
        tts.setSpeechRate((float) 1);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1");
    }



    @Override
    protected void onDestroy() {    //tts 후 destroy
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onInit(int status) {
        this.status = status;        //tts 수행 성공시 초기화 정보
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.KOREA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                ttsbtn.setEnabled(true);
                Information();
            }
        } else {
            Log.e("TTS", "initalization Failed");
        }
    }

    @RequiresApi(api=Build.VERSION_CODES.LOLLIPOP)
    public void speakOut(String str){    //tts speakout 함수 : 입력된 텍스트를 음성으로 출력하는 함수

        CharSequence text = str;//ttsText.getText();
        tts.setPitch((float)0.6);
        tts.setSpeechRate((float)1);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH,null,"id1");
    }


}