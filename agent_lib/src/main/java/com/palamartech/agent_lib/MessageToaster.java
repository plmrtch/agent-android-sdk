package com.palamartech.agent_lib;

import android.content.Context;
import android.widget.Toast;

public class MessageToaster {
    public static void ToastMessage(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
