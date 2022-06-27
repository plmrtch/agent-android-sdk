# Palmate Android SDK

![https://jitpack.io/v/plmrtch/agent-android-sdk.svg](https://jitpack.io/v/plmrtch/agent-android-sdk.svg)

It is an SDK developed to implement [Palmate.ai](http://palmate.ai/) on native Android applications. Basically, the implementation of the module implemented in a new Activity logic is as follows.

## Implementation

### 1. Installing SDK

You need to add this line on your `build.gradle` file at module level in `dependencies` section

```
implementation 'com.github.plmrtch:agent-android-sdk:1.0.7'

```

After making changes to the relevant file, click on Sync now, which will appear in the upper right.

![https://user-images.githubusercontent.com/94033525/164439499-627ca999-199c-4b4b-8786-670828f34cc2.png](https://user-images.githubusercontent.com/94033525/164439499-627ca999-199c-4b4b-8786-670828f34cc2.png)

### Important Notice

If an error is received in this step that the relevant library cannot be found, it means that the project cannot search on JitPack io. To overcome this situation, the following code block should be added to the `repositories` section of the `settings.gradle` file.

```
maven { url "<https://jitpack.io>" }

```

### 2. Adding Activity to your Manifest File

The corresponding chat module works as an android activity. Therefore, the following code should be added to the `application` tag of the `AndroidManifest.xml` file under the `manifest` folder of the application.

```
<activity
  android:name="com.palamartech.palamaragent.ChatActivity"
  android:screenOrientation="portrait">
</activity>

```

### 3. Starting Chat Activity

You can start the relevant activity as follows. An example is the start chat when a button is clicked.

```
btnStartChat.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, com.palamartech.palamaragent.ChatActivity.class);
        intent.putExtra("projectToken", PROJECT_TOKEN_HERE);
        startActivity(intent);
    }
});

```

After this button click, your customers will see screen as below:

![https://user-images.githubusercontent.com/94033525/164443227-95ff19f5-65fd-47d1-af06-af6009eafba0.png](https://user-images.githubusercontent.com/94033525/164443227-95ff19f5-65fd-47d1-af06-af6009eafba0.png)

### 3.1 Starting Chat Activity with credentials

If you already know the information that the user will enter in the mobile application and you do not want to re-enter the user in concern of UX, you can start the conversation with these credentials as follows.
```
btnStartChat.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, com.palamartech.palamaragent.ChatActivity.class);
        intent.putExtra("projectToken", PROJECT_TOKEN_HERE);

        JSONObject customerData = new JSONObject();
        try {
            customerData.put("data1", "DATA_1");
            customerData.put("data2", "DATA_2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        intent.putExtra("customerData", customerData.toString());

        startActivity(intent);
    }
});

```
