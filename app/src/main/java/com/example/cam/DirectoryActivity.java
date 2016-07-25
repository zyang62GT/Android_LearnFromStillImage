package com.example.cam;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;

public class DirectoryActivity extends Activity {
    public final static String EXTRA_MESSAGE = "com.example.cam.MESSAGE";

    String path = Environment.getExternalStorageDirectory().toString()+"/Pictures";
    //Log.d("Files", "Path: " + path);
    File f = new File(path);
    File file[] = f.listFiles();
    int len = file.length;
    String wee[] = new String[len];
    Spinner spinner;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory);

        convertFiletoString();

        spinner = (Spinner) findViewById(R.id.directory_spinner);


        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, wee);
        spinnerArrayAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );

// Spinner spinYear = (Spinner)findViewById(R.id.spin);
        spinner.setAdapter(spinnerArrayAdapter);

    }

    protected void convertFiletoString(){
        for(int i=0;i<file.length;i++){
            wee[i]=file[i].getName().toString();
        }
    }

    public void directoryClick(View view){
        Intent intent = new Intent(this,LearnActivity.class);

        String message = spinner.getSelectedItem().toString();

        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
