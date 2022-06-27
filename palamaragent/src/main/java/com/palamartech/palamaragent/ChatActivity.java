package com.palamartech.palamaragent;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatActivity extends AppCompatActivity {

    //region SOCKET LISTENER
    private final class PalamarSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(WebSocket webSocket, okhttp3.Response response){

        }

        @Override
        public void onMessage(WebSocket webSocket, String text){
            try {
                JSONObject jsonObject;

                //region CONVERT TO JSON
                String str = "";
                try {
                    str = new String(text.getBytes("ISO-8859-1"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                jsonObject = new JSONObject(str);
                //endregion

                //region PARSE MESSAGE
                String messageType = jsonObject.getString("type");
                JSONObject payload = jsonObject.getJSONObject("payload");

                if(messageType.equals("notification")){
                    if(payload.getString("type").equals("authentication")){
                        if(payload.getString("status").equals("SUCCESS")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    rlGetInfomations.setVisibility(View.INVISIBLE);
                                    rlChatScene.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                    else if(payload.getString("type").equals("queue_status")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rlChatScene.setVisibility(View.INVISIBLE);
                                rlQueueScene.setVisibility(View.VISIBLE);
                                try {
                                    txtQueueMsg.setText("Bekleyenler arasında "+payload.getString("order")+". sıradasınız");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else if(payload.getString("action_type").equals("connect")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rlQueueScene.setVisibility(View.INVISIBLE);
                                rlChatScene.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                    else if(payload.getString("action_type").equals("customer_session_end")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        });
                    }
                    else if(payload.getString("action_type").equals("direct_to_ticket")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rlGetInfomations.setVisibility(View.INVISIBLE);
                                rlChatScene.setVisibility(View.INVISIBLE);

                                rlTicketScene.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                    else if(payload.getString("action_type").equals("ticket_saved")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rlChatScene.setVisibility(View.INVISIBLE);
                                rlTicketScene.setVisibility(View.INVISIBLE);
                                rlGetInfomations.setVisibility(View.VISIBLE);

                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.ch_ts_ticket_successfull), Toast.LENGTH_LONG).show();

                                finish();
                            }
                        });
                    }
                }
                else if(messageType.equals("reply_message")){
                    if(payload.getString("type").equals("text")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String data = payload.getString("data");

                                    //region SHOW NOTIFICATION IF APP IN BACKGROUND
                                    if(isAppBackground(getApplicationContext())){
                                        Intent notificationIntent = new Intent(getApplicationContext(), ChatActivity.class);
                                        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(ChatActivity.this, "My Notification");
                                        builder.setContentTitle("Yeni Mesaj");
                                        builder.setContentText(data);
                                        builder.setSmallIcon(R.drawable.icon_bot);
                                        builder.setAutoCancel(false);
                                        builder.setContentIntent(contentIntent);

                                        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(ChatActivity.this);
                                        managerCompat.notify(1, builder.build());
                                    }
                                    //endregion

                                    //region MESSAGE ITEM
                                    RelativeLayout rlMessage = new RelativeLayout(getApplicationContext());

                                    llMessagesList.addView(rlMessage);

                                    LinearLayout.LayoutParams llLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    rlMessage.setLayoutParams(llLayoutParams);

                                    Drawable receiverDr = ContextCompat.getDrawable(getApplicationContext(), R.drawable.chat_receiver_bubble).mutate();
                                    rlMessage.setBackground(receiverDr);
                                    //endregion

                                    //region MESSAGE TEXT
                                    TextView txtData = new TextView(getApplicationContext());
                                    txtData.setText(data);
                                    txtData.setTextSize(TEXT_SIZE);
                                    txtData.setTextColor(Color.parseColor(TEXT_COLOR));

                                    Linkify.addLinks(txtData, Linkify.ALL);

                                    rlMessage.addView(txtData);
                                    //endregion

                                    scrollToBottom();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else if(payload.getString("type").equals("choice")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String data = payload.getString("text");
                                    if(data.equals("Aradığın soruya cevap verebildim mi?")){
                                        return;
                                    }

                                    //region MESSAGE ITEM
                                    LinearLayout llMessage = new LinearLayout(getApplicationContext());
                                    llMessage.setOrientation(LinearLayout.VERTICAL);

                                    llMessagesList.addView(llMessage);

                                    LinearLayout.LayoutParams llLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    llMessage.setLayoutParams(llLayoutParams);

                                    Drawable receiverDr = ContextCompat.getDrawable(getApplicationContext(), R.drawable.chat_receiver_bubble).mutate();
                                    llMessage.setBackground(receiverDr);
                                    //endregion

                                    //region MESSAGE TEXT
                                    TextView txtData = new TextView(getApplicationContext());
                                    txtData.setText(data);
                                    txtData.setTextSize(TEXT_SIZE);
                                    txtData.setTextColor(Color.parseColor(TEXT_COLOR));

                                    llMessage.addView(txtData);
                                    //endregion

                                    //region SET CHOICES
                                    JSONArray choices = payload.getJSONArray("choices");

                                    //region CREATE CHOICE VIEWS
                                    HashMap<Button, JSONObject> choiceList = new HashMap<Button, JSONObject>();
                                    for(int i=0; i < choices.length(); i++){
                                        JSONObject item = choices.getJSONObject(i);
                                        String text = item.getString("text");
                                        String value = item.getString("value");

                                        Button btnChoice = new Button(getApplicationContext());
                                        btnChoice.setText(text);
                                        llMessage.addView(btnChoice);

                                        choiceList.put(btnChoice, item);
                                    }
                                    //endregion

                                    //region REGISTER CHOICE CLICKS
                                    for (HashMap.Entry<Button, JSONObject> entry : choiceList.entrySet()) {
                                        Button clicked = entry.getKey();
                                        JSONObject item = entry.getValue();

                                        clicked.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                //region SEND MESSAGE TO SOCKET FOR CLICK
                                                try {
                                                    JSONObject messageBody = new JSONObject();
                                                    JSONObject payload = new JSONObject();

                                                    messageBody.put("type", "customer_message");

                                                    payload.put("type", "button_select");
                                                    payload.put("value", item.getString("value"));

                                                    messageBody.put("payload", payload);
                                                    webSocket.send(messageBody.toString());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                                //endregion

                                                for (HashMap.Entry<Button, JSONObject> entry : choiceList.entrySet()) {
                                                    Button key = entry.getKey();

                                                    //region SET VIEWS AS CLICKED
                                                    if (clicked == key){
                                                        key.setEnabled(false);
                                                    }else{
                                                        ((ViewManager)key.getParent()).removeView(key);
                                                    }
                                                    //endregion

                                                }
                                            }
                                        });
                                    }
                                    //endregion

                                    //endregion

                                    scrollToBottom();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }


                            }
                        });
                    }
                }
                else if(messageType.equals("customer_message")){

                    //region IF USER NOT ANSWERED CHOICE REPLY -> DISABLE ALL
                    for(int index = 0; index < ((ViewGroup) llMessagesList).getChildCount(); index++) {
                        View nextChild = ((ViewGroup) llMessagesList).getChildAt(index);

                        if(((ViewGroup)nextChild).getChildCount() > 2){
                            View buttonCandidate = ((ViewGroup) nextChild).getChildAt(1);
                            if(buttonCandidate instanceof Button){
                                for(int i = 0; i < ((ViewGroup) nextChild).getChildCount(); i++) {
                                    View deepChild = ((ViewGroup) nextChild).getChildAt(i);
                                    if(deepChild instanceof Button){
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                deepChild.setEnabled(false);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                    //endregion

                    if(payload.getString("type").equals("text")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String data = payload.getString("data");

                                    //region MESSAGE CONTAINER
                                    LinearLayout llMessageContainer = new LinearLayout(getApplicationContext());
                                    llMessageContainer.setOrientation(LinearLayout.HORIZONTAL);

                                    llMessagesList.addView(llMessageContainer);

                                    LinearLayout.LayoutParams rlLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    llMessageContainer.setLayoutParams(rlLayoutParams);
                                    //endregion

                                    //region MESSAGE ALIGNER
                                    RelativeLayout rlMessageAligner = new RelativeLayout(getApplicationContext());
                                    llMessageContainer.addView(rlMessageAligner);

                                    LinearLayout.LayoutParams llMessageAlignerLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    llMessageAlignerLP.weight = 1;
                                    rlMessageAligner.setLayoutParams(llMessageAlignerLP);
                                    //endregion

                                    //region MESSAGE ITEM
                                    RelativeLayout rlMessageItem = new RelativeLayout(getApplicationContext());
                                    llMessageContainer.addView(rlMessageItem);

                                    LinearLayout.LayoutParams llMessageItemLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    rlMessageItem.setLayoutParams(llMessageItemLP);

                                    Drawable receiverDr = ContextCompat.getDrawable(getApplicationContext(), R.drawable.chat_sender_bubble).mutate();
                                    rlMessageItem.setBackground(receiverDr);
                                    //endregion

                                    //region MESSAGE TEXT
                                    TextView txtData = new TextView(getApplicationContext());
                                    txtData.setText(data);
                                    txtData.setTextSize(TEXT_SIZE);
                                    txtData.setTextColor(Color.parseColor(TEXT_COLOR));
                                    Linkify.addLinks(txtData, Linkify.ALL);
                                    rlMessageItem.addView(txtData);
                                    //endregion

                                    scrollToBottom();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else if(payload.getString("type").equals("file")){
                        runOnUiThread(new Runnable() {
                            @SuppressLint("UseCompatLoadingForDrawables")
                            @Override
                            public void run() {
                                try {
                                    String fileName = payload.getJSONObject("data").getString("file_name");

                                    //region MESSAGE CONTAINER
                                    LinearLayout llMessageContainer = new LinearLayout(getApplicationContext());
                                    llMessageContainer.setOrientation(LinearLayout.HORIZONTAL);

                                    llMessagesList.addView(llMessageContainer);

                                    LinearLayout.LayoutParams rlLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    llMessageContainer.setLayoutParams(rlLayoutParams);
                                    //endregion

                                    //region MESSAGE ALIGNER
                                    RelativeLayout rlMessageAligner = new RelativeLayout(getApplicationContext());
                                    llMessageContainer.addView(rlMessageAligner);

                                    LinearLayout.LayoutParams llMessageAlignerLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    llMessageAlignerLP.weight = 1;
                                    rlMessageAligner.setLayoutParams(llMessageAlignerLP);
                                    //endregion

                                    //region MESSAGE ITEM
                                    LinearLayout rlMessageItem = new LinearLayout(getApplicationContext());
                                    rlMessageItem.setOrientation(LinearLayout.VERTICAL);
                                    llMessageContainer.addView(rlMessageItem);

                                    LinearLayout.LayoutParams llMessageItemLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    rlMessageItem.setLayoutParams(llMessageItemLP);

                                    Drawable receiverDr = ContextCompat.getDrawable(getApplicationContext(), R.drawable.chat_sender_bubble).mutate();
                                    rlMessageItem.setBackground(receiverDr);
                                    //endregion

                                    //region FILE ICON
                                    RelativeLayout rlImageContainer = new RelativeLayout(getApplicationContext());
                                    rlImageContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                    rlMessageItem.addView(rlImageContainer);

                                    ImageView imgFile = new ImageView(getApplicationContext());
                                    rlImageContainer.addView(imgFile);
                                    imgFile.setBackground(getResources().getDrawable(R.drawable.icon_document));
                                    imgFile.getLayoutParams().height = dpFormat(50);
                                    imgFile.getLayoutParams().width = dpFormat(50);

                                    RelativeLayout.LayoutParams layoutParams =
                                            (RelativeLayout.LayoutParams)imgFile.getLayoutParams();
                                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                                    imgFile.setLayoutParams(layoutParams);

                                    //endregion

                                    //region MESSAGE TEXT
                                    TextView txtData = new TextView(getApplicationContext());
                                    txtData.setText(fileName);
                                    txtData.setTextSize(12);
                                    txtData.setTextColor(Color.parseColor(TEXT_COLOR));
                                    Linkify.addLinks(txtData, Linkify.ALL);
                                    rlMessageItem.addView(txtData);

                                    txtData.setPadding(0, dpFormat(15), 0,0);
                                    txtData.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                    //endregion

                                    scrollToBottom();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
                //endregion

            }catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason){
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
    }
    //endregion

    //region COMPONENT HELPERS
    public static RequestQueue requestQueue;
    private OkHttpClient client;
    private WebSocket webSocket;
    //endregion

    //region CHAT BASE CONSTANTS
    private static String PROJECT_TOKEN = "";
    private static String SESSION_TOKEN = "";
    private static final String API_BASE_URL = "http://palmate.palamar.com.tr/api";
    private static final String SOCKET_URL = "ws://palmate.palamar.com.tr/ws/customerV2/";

    //region STYLE CONSTANTS
    private static final String PRIMARY_COLOR = "#144fff";
    private static final String TEXT_COLOR = "#212529";
    private static final float TEXT_SIZE = 18;
    private static final int DEFAULT_CHAT_BOX_MARGINS = 5;
    //endregion

    //endregion

    //region CHAT UI COMPONENTS

    //region BASE UI
    private LinearLayout llBaseToolbar;
    private TextView txtChatTitle;
    private ImageView imgCloseBtn;
    //endregion

    //region GET INFORMATION COMPONENTS
    private RelativeLayout rlUserIconContainer;
    private LinearLayout rlFormArea;
    private RelativeLayout rlGetInfomations;
    private Button btnStartChat;
    private ProgressBar pbFormLoading;
    //endregion

    //region CHAT SCENE
    private RelativeLayout rlChatScene;
    private ScrollView svMessagesList;
    private LinearLayout llMessagesList;
    private ImageView imgSend;
    private ImageView imgFileSelect;
    private EditText edtChatInput;
    //endregion

    //region TICKET SCENE
    private RelativeLayout rlTicketScene;
    private EditText edtTicketInput;
    private Button btnOpenTicket;
    //endregion

    //region QUEUE SCENE
    private RelativeLayout rlQueueScene;
    private TextView txtQueueMsg;
    //endregion

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("My Notification", "My Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        handleSSLHandshake();

        initUI();

        String customerData = getIntent().getStringExtra("customerData");
        String projectToken = getIntent().getStringExtra("projectToken");
        if(projectToken != null){
            PROJECT_TOKEN = projectToken;
        }
        if(customerData != null){
            try {
                JSONObject startSessionBody = new JSONObject();
                JSONObject customerJson = new JSONObject(customerData);

                startSessionBody.put("session_type", "RTM");
                startSessionBody.put("project_token", PROJECT_TOKEN);

                startSessionBody.put("customer_info", customerJson);

                createNewSession(startSessionBody);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            rlGetInfomations.setVisibility(View.VISIBLE);
        }

    }

    //region GENERAL COMPONENT HELPERS
    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void initUI(){
        //region BASE UI
        llBaseToolbar = findViewById(R.id.llBaseToolbar);
        llBaseToolbar.setBackgroundColor(Color.parseColor(PRIMARY_COLOR));

        txtChatTitle = findViewById(R.id.txtChatTitle);

        imgCloseBtn = findViewById(R.id.imgCloseBtn);
        imgCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rlChatScene.getVisibility() == View.VISIBLE || rlTicketScene.getVisibility() == View.VISIBLE){
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                    builder.setTitle(getResources().getString(R.string.ch_cs_end_session_title));
                    builder.setMessage(getResources().getString(R.string.ch_cs_end_session_message));

                    builder.setPositiveButton(getResources().getString(R.string.ch_cs_end_session_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                JSONObject endBody = new JSONObject();
                                endBody.put("type", "customer_session_end");

                                JSONObject payload = new JSONObject();
                                endBody.put("payload", payload);

                                webSocket.send(endBody.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    builder.setNegativeButton(getResources().getString(R.string.ch_cs_end_session_cancel), null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else {
                    finish();
                }
            }
        });
        //endregion

        //region GET INFORMATION
        rlGetInfomations = findViewById(R.id.rlGetInfomations);
        rlFormArea = findViewById(R.id.rlFormArea);
        pbFormLoading = findViewById(R.id.pbFormLoading);

        //region SET COLORS OF USER ICON
        rlUserIconContainer = findViewById(R.id.rlUserIconContainer);
        GradientDrawable userIconDrawable = (GradientDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_rounded_corners_bg).mutate();
        userIconDrawable.setColor(Color.parseColor(PRIMARY_COLOR));
        rlUserIconContainer.setBackground(userIconDrawable);
        //endregion

        //region SET COLORS OF START CHAT BUTTON
        btnStartChat = findViewById(R.id.btnStartChat);
        GradientDrawable startChatDrawable = (GradientDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_start_chat_btn).mutate();
        startChatDrawable.setColor(Color.parseColor(PRIMARY_COLOR));
        btnStartChat.setBackground(startChatDrawable);
        //endregion

        //endregion

        //region CHAT SCENE
        rlChatScene = findViewById(R.id.rlChatScene);

        svMessagesList = findViewById(R.id.svMessagesList);
        llMessagesList = findViewById(R.id.llMessagesList);

        edtChatInput = findViewById(R.id.edtChatInput);

        imgSend = findViewById(R.id.imgSend);
        imgSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edtChatInput.getText().toString();
                if(message.length() > 0){
                    try {
                        JSONObject messageBody = new JSONObject();
                        JSONObject payload = new JSONObject();

                        messageBody.put("type", "customer_message");
                        payload.put("type", "text");
                        payload.put("data", message);

                        messageBody.put("payload", payload);
                        webSocket.send(messageBody.toString());
                        edtChatInput.setText("");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }
        });

        imgFileSelect = findViewById(R.id.imgFileSelect);
        imgFileSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!checkPermissionForReadExtertalStorage()){
                    try {
                        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else{
                    openFileChooser();
                }
            }
        });

        //endregion

        //region TICKET SCENE
        rlTicketScene = findViewById(R.id.rlTicketScene);
        edtTicketInput = findViewById(R.id.edtTicketInput);
        btnOpenTicket = findViewById(R.id.btnOpenTicket);

        GradientDrawable openTicketDrawable = (GradientDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_start_chat_btn).mutate();
        openTicketDrawable.setColor(Color.parseColor(PRIMARY_COLOR));
        btnOpenTicket.setBackground(openTicketDrawable);

        btnOpenTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ticketMessage = edtTicketInput.getText().toString();
                if(ticketMessage.length() == 0){
                    return;
                }

                try {
                    JSONObject ticketBody = new JSONObject();
                    ticketBody.put("type", "ticket");

                    JSONObject payload = new JSONObject();
                    payload.put("text", ticketMessage);

                    ticketBody.put("payload", payload);

                    webSocket.send(ticketBody.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        //endregion

        //region QUEUE SCENE
        rlQueueScene = findViewById(R.id.rlQueueScene);
        txtQueueMsg = findViewById(R.id.txtQueueMsg);
        //endregion

        getProjectSettings();
        connectSocket();
    }

    private void connectSocket(){
        client = Http.client();

        okhttp3.Request request = new okhttp3.Request.Builder().url(SOCKET_URL).build();
        PalamarSocketListener listener = new PalamarSocketListener();
        webSocket = client.newWebSocket(request, listener);
    }

    private void closeKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static boolean isAppBackground(Context context){
        boolean isBackground=true;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH){
            List<ActivityManager.RunningAppProcessInfo> runningProcesses =activityManager.getRunningAppProcesses();
            for(ActivityManager.RunningAppProcessInfo processInfo:runningProcesses){
                if(processInfo.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                    for(String activeProcess:processInfo.pkgList){
                        if(activeProcess.equals(context.getPackageName())){
                            isBackground = false;
                        }
                    }
                }
            }
        }else{
            List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
            if(taskInfo.size()>0) {
                ComponentName componentName = taskInfo.get(0).topActivity;
                if(componentName.getPackageName().equals(context.getPackageName())){
                    isBackground = false;
                }
            }
        }
        return isBackground;
    }

    public int dpFormat(int dp) {
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }

    //region PERMISSION
    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = getApplicationContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    private void askForPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(ChatActivity.this, permission)) {
                Toast.makeText(getApplicationContext(), "Please grant the requested permission to get your task done!", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(ChatActivity.this, new String[]{permission}, requestCode);
            } else {
                ActivityCompat.requestPermissions(ChatActivity.this, new String[]{permission}, requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileChooser();
                }
                else {
                    //permission with request code 1 was not granted
                    Toast.makeText(this, getResources().getString(R.string.ch_cs_permission_not_granted) , Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    //endregion

    //region FILE SELECT
    public void openFileChooser(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] extraMimeTypes = {"application/pdf", "application/doc", "image/png", "image/jpeg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent returnIntent) {
        if (resultCode != RESULT_OK) {
            return;
        } else {
            //region GET SELECTED FILES

            Uri selectedFileUri = returnIntent.getData();

            StringBuilder sb = new StringBuilder();
            sb.append(getResources().getString(R.string.ch_cs_send_file_get_confirm));
            sb.append("\n");
            sb.append("\n");
            sb.append(displayName(selectedFileUri));


            new AlertDialog.Builder(ChatActivity.this)
                    .setTitle(getResources().getString(R.string.ch_cs_send_file))
                    .setPositiveButton(getResources().getString(R.string.ch_cs_end_session_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final ProgressDialog[] progressDialog = new ProgressDialog[1];
                                    try {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog[0] = ProgressDialog.show(ChatActivity.this, null, null, true, false);
                                                progressDialog[0].setContentView(R.layout.progress_dialog);
                                                progressDialog[0].show();
                                            }
                                        });

                                        //region SEND REQUEST
                                        Uri fileUri = getFilePathFromUri(selectedFileUri, getApplicationContext());

                                        String charset = "UTF-8";
                                        File uploadFile1 = new File(fileUri.getPath());
                                        String requestURL = API_BASE_URL + "/customer/new-file/";

                                        MultipartUtility multipart = new MultipartUtility(requestURL, charset);

                                        multipart.addFormField("session_token", SESSION_TOKEN);
                                        multipart.addFilePart("file", uploadFile1);

                                        List<String> response = multipart.finish();
                                        //endregion

                                        for (String line : response) {
                                            JSONObject fileUpResponse = new JSONObject(line);
                                            if(fileUpResponse.has("uuid")){
                                                JSONObject fileSendBody = new JSONObject();
                                                fileSendBody.put("type", "customer_message");

                                                JSONObject payload = new JSONObject();
                                                payload.put("type", "file");
                                                payload.put("text", "");
                                                payload.put("data", fileUpResponse);

                                                fileSendBody.put("payload", payload);
                                                webSocket.send(fileSendBody.toString());
                                            }
                                        }

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog[0].cancel();
                                            }
                                        });

                                    } catch (Exception e) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog[0].cancel();
                                            }
                                        });
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.ch_cs_end_session_cancel), null)
                    .setMessage(sb.toString())
                    .show();
        }
        super.onActivityResult(requestCode, resultCode, returnIntent);
    }

    public static String getFileNameFromCursor(Uri uri, Context myContext) {
        Cursor fileCursor = myContext.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        String fileName = null;
        if (fileCursor != null && fileCursor.moveToFirst()) {
            int cIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (cIndex != -1) {
                fileName = fileCursor.getString(cIndex);
            }
        }
        return fileName;
    }

    public static String getFileExtension(Uri uri, Context myContext) {
        String fileType = myContext.getContentResolver().getType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType);
    }

    public static String getFileName(Uri uri, Context myContext) {
        String fileName = getFileNameFromCursor(uri, myContext);
        if (fileName == null) {
            String fileExtension = getFileExtension(uri, myContext);
            fileName = "temp_file" + (fileExtension != null ? "." + fileExtension : "");
        } else if (!fileName.contains(".")) {
            String fileExtension = getFileExtension(uri, myContext);
            fileName = fileName + "." + fileExtension;
        }
        return fileName;
    }

    public static Uri getFilePathFromUri(Uri uri, Context myContext) throws IOException {
        String fileName = getFileName(uri, myContext);
        File file = new File(myContext.getExternalCacheDir(), fileName);
        file.createNewFile();
        try (OutputStream outputStream = new FileOutputStream(file);
             InputStream inputStream = myContext.getContentResolver().openInputStream(uri)) {
            /*FileUtil.copyStream(inputStream, outputStream); //Simply reads input to output stream
            outputStream.flush();*/
        }
        return Uri.fromFile(file);
    }

    private String displayName(Uri uri) {

        Cursor mCursor =
                getApplicationContext().getContentResolver().query(uri, null, null, null, null);
        int indexedname = mCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        mCursor.moveToFirst();
        String filename = mCursor.getString(indexedname);
        mCursor.close();
        return filename;
    }

    //endregion

    //endregion

    //region GET INFORMATION HELPERS
    private void getProjectSettings(){
        String url = API_BASE_URL + "/project_model/welcome_form/?token=" + PROJECT_TOKEN;

        StringRequest sr = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onResponse(String response) {

                rlFormArea.removeView(pbFormLoading);

                try {
                    //region CONVERT TO JSON
                    String str = "";
                    try {
                        str = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    JSONObject jsonObject = new JSONObject(str);
                    //endregion

                    //region SET TITLE
                    txtChatTitle.setText(jsonObject.getString("popup_title"));
                    //endregion

                    //region ADD FORM ITEMS
                    JSONArray formItems = jsonObject.getJSONArray("form_items");
                    HashMap<EditText, JSONObject> editTexts = new HashMap<EditText, JSONObject>();

                    for(int i=0; i < formItems.length(); i++)
                    {
                        JSONObject item = formItems.getJSONObject(i);

                        TextView txtItem = new TextView(getApplicationContext());
                        txtItem.setText(item.getString("form_label"));

                        EditText edtItem = new EditText(getApplicationContext());

                        edtItem.setPadding(dpFormat(20), dpFormat(10),dpFormat(10), dpFormat(10));
                        Drawable receiverDr = ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_edit_text_border).mutate();
                        edtItem.setBackground(receiverDr);

                        if(item.getString("form_item_type").equals("ON")){
                            edtItem.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        }

                        rlFormArea.addView(txtItem);
                        rlFormArea.addView(edtItem);

                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) edtItem.getLayoutParams();
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        params.topMargin = dpFormat(10);
                        params.bottomMargin = dpFormat(5);


                        editTexts.put(edtItem, item);
                    }

                    //endregion

                    //region REGISTER START CHAT BUTTON ACTION
                    btnStartChat.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            JSONObject jsonBody = new JSONObject();
                            try {
                                jsonBody.put("session_type", "RTM");
                                jsonBody.put("project_token", PROJECT_TOKEN);

                                JSONObject customerInfo = new JSONObject();

                                for (HashMap.Entry<EditText, JSONObject> entry : editTexts.entrySet()) {
                                    EditText key = entry.getKey();
                                    JSONObject value = entry.getValue();

                                    customerInfo.put(value.getString("form_key"), key.getText().toString());
                                    if(value.getBoolean("is_identifier")){
                                        if(key.getText().toString().length() == 0){
                                            return;
                                        }
                                        customerInfo.put("identifier", value.getString("form_key"));
                                    }
                                }

                                jsonBody.put("customer_info", customerInfo);

                                createNewSession(jsonBody);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    //endregion

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error while getting project settings, contact system admin.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        requestQueue.add(sr);
    }

    private void createNewSession(JSONObject jsonBody){
        String url = API_BASE_URL + "/customer/new-session/";

        closeKeyboard();
        ProgressDialog progressDialog = ProgressDialog.show(this, null, null, true, false);
        progressDialog.setContentView(R.layout.progress_dialog);

        progressDialog.show();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                progressDialog.cancel();

                try {
                    SESSION_TOKEN = response.getString("session_token");

                    JSONObject loginBody = new JSONObject();
                    loginBody.put("type", "login");

                    JSONObject payload = new JSONObject();
                    payload.put("token", SESSION_TOKEN);

                    loginBody.put("payload", payload);
                    webSocket.send(loginBody.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.cancel();
                if(error.networkResponse.statusCode == 401){
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.ch_gi_incorrect_form_results), Snackbar.LENGTH_SHORT).setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    });
                    snackbar.setActionTextColor(getResources().getColor(R.color.holo_red_dark));
                    snackbar.show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Error while creating new session", Toast.LENGTH_SHORT).show();
                }
            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );
        requestQueue.add(request);

    }
    //endregion

    //region CHAT SCENE HELPERS
    private void scrollToBottom(){
        svMessagesList.postDelayed(new Runnable() {
            @Override
            public void run() {
                svMessagesList.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 100);
    }
    //endregion
}
