package com.example.testing223;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static final String ERROR_DETECTED = "No NFC Tag Detected";
    public static final String WRITE_SUCCESS = "Text Written Successfully!";
    public static final String WRITE_ERROR = "Error during Writing, Try Again";

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] writingTagFilters;
    boolean writeMode;
    Tag myTag;
    Context context;

    TextView editMessage;
    TextView nfccontent;
    TextView instructionText;
    Button activateButton;
    Button readButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editMessage = (TextView) findViewById(R.id.editTextPlantName);
        nfccontent = (TextView) findViewById(R.id.textViewContent);
        activateButton = (Button) findViewById(R.id.buttonWrite);
        instructionText = (TextView) findViewById(R.id.textView);
        instructionText.setText("When you water the plant, enter the date on the gap and then click the button I watered! If you wanna see the last time that the plant was watered click the second button. Remember to keep the back part of the phone close to the NFC tag in your plant so it can work properly.");


        context = this;

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            Toast.makeText(this,"This device does not support NFC",Toast.LENGTH_SHORT).show();
        }

        readfromIntent(getIntent());

        activateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    if(myTag == null){
                        Toast.makeText(context,ERROR_DETECTED,Toast.LENGTH_LONG).show();
                    }
                    else{
                        write(""+editMessage.getText().toString(),myTag);
                        Toast.makeText(context,WRITE_SUCCESS,Toast.LENGTH_LONG).show();
                    }
                }
                catch(IOException e){
                    Toast.makeText(context,WRITE_ERROR,Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                catch(FormatException e){
                    Toast.makeText(context,WRITE_ERROR,Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private void readfromIntent(Intent intent){
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    private void write(String text, Tag tag) throws IOException, FormatException{
        NdefRecord[] records = {createRecord(text)};
        NdefMessage message = new NdefMessage(records);

        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private NdefRecord createRecord(String content) throws UnsupportedEncodingException{
        String lang = "en";
        byte[] contentBytes = content.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int contentLength = contentBytes.length;
        int langLength =  langBytes.length;
        byte[] payload = new byte[1+contentLength+langLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes,0,payload,1,langLength);
        System.arraycopy(contentBytes,0,payload,1+langLength,contentLength);

        NdefRecord NFCRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT, new byte[0], payload);

        return NFCRecord;
    }


    @Override
    public void onPause(){
        stopForegroundDispatch(this, nfcAdapter);
        //WriteModeOff();
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        setupForegroundDispatch(this, nfcAdapter);
        //WriteModeOn();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        //setIntent(intent);
        readfromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                nfccontent.setText("Read content: " + result);
            }
        }
    }
}
