package com.sierracharter.myapplication;


import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFilenameFilter;

public class FileChooserActivity extends ListActivity {

    private static final String TAG = "FileChooserActivity";

    public static final String INTENT_ARGS = "url";
    public static final int ACTION_PICK_FILE = 33;
    public static final String INTENT_RESULT = "result";

    private ArrayAdapter arrayAdapter;
    private MyTask myTask;
    private String startingPath;
    private List listOfFilesFoundInDirectory = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);

        startingPath = getIntent().getStringExtra(INTENT_ARGS);

        myTask = new MyTask();
        myTask.execute(startingPath);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
//        String chosenFile = (String) listOfPaths.get(position);
        String chosenFile = (String)l.getItemAtPosition(position);
        Log.d(TAG, chosenFile);

        //This is a directory.
        if(chosenFile.endsWith("/")){
            String fullPath = startingPath + chosenFile;
            Intent intent = new Intent(this, FileChooserActivity.class);
            intent.putExtra(INTENT_ARGS, fullPath);
            startActivityForResult(intent, ACTION_PICK_FILE);
            //This is a file.
        }else{
            Intent intent = getIntent();
            String foundFile = startingPath + chosenFile;
            intent.putExtra(INTENT_RESULT, foundFile);
            setResult(RESULT_OK, intent);
            finish();
        }

    }

    private class MyTask extends AsyncTask<String, Void, List>{

        @Override
        protected List doInBackground(String... strings) {
            List listOfPathsToReturn = new ArrayList();

            String dir = strings[0];

            CIFSContext baseContext = SingletonContext.getInstance();
            CIFSContext context = baseContext.withCredentials(new NtlmPasswordAuthenticator("Benji", "attimn1em"));

            try(SmbFile file = new SmbFile(dir, context)){

                try{
                    String[] filesInDirectory = null;

                    //Don't try to list files if we actually returned a file and not a directory.
                    if(file.isDirectory()){
                        filesInDirectory = file.list(new MyFilter());
                    }

                    for(String s : filesInDirectory){
                        listOfPathsToReturn.add(s);
                    }

                    Collections.sort(listOfPathsToReturn);
                    return listOfPathsToReturn;

                }catch(SmbException e){
                    e.printStackTrace();
                }

            }catch(MalformedURLException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List list) {
            if(list != null){
                listOfFilesFoundInDirectory = new ArrayList(list);
                arrayAdapter = new ArrayAdapter(FileChooserActivity.this, android.R.layout.simple_list_item_2, android.R.id.text2, listOfFilesFoundInDirectory);
                ListView listView = getListView();
                listView.setAdapter(arrayAdapter);
            }
        }
    }

    private class MyFilter implements SmbFilenameFilter {

        @Override
        public boolean accept(SmbFile dir, String name) throws SmbException {
            if(name.endsWith(".txt")){
                return false;
            }

            String[] undesiredFileExtensions = {".xlsx", ".bmp", ".png", ".jpg", "jpeg", ".doc", ".docx", ".txt"};

            for(String ext : undesiredFileExtensions){
                if(name.endsWith(ext)){
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTION_PICK_FILE){
            if(resultCode == RESULT_OK){
                String foundFile = data.getStringExtra(INTENT_RESULT);
                Intent intent = getIntent();
                intent.putExtra(INTENT_RESULT, foundFile);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }
}
