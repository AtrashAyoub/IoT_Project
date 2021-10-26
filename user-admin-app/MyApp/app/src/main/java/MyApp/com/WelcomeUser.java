package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.microsoft.signalr.Action1;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WelcomeUser extends AppCompatActivity {
    NotificationManagerCompat notificationManager;
    public static ArrayList<String> Notifications = new ArrayList<String>();
    HubConnection hub;
    Boolean nogotiatedFlag = false;
    ReentrantLock mutex = new ReentrantLock(true);

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(hub.getConnectionState().equals(HubConnectionState.CONNECTED)){
            hub.stop();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_user);
        Button MainPage = findViewById(R.id.mainpage);
        final ListView ListView1 = (ListView) findViewById(R.id.NotificationList);
        notificationManager = NotificationManagerCompat.from(this);

        final String[] AccessKey = new String[1];
        final String[] SignalrURL = new String[1];
        final ArrayAdapter MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Notifications);
        String UserID = (getIntent().getStringExtra("NAME"));

        updateFeeds(UserID,ListView1,MyAdapter1);
        // JSON Building:
        JSONObject json = new JSONObject();
        try {
            json.put("UserId", UserID);
        }catch (JSONException e){
            e.printStackTrace();
        }
        String jsonStr = json.toString();
        MediaType MEDIA_TYPE = MediaType.parse("application/json");
        String URL = "https://counterfunctions20200429005139.azurewebsites.net/api/negotiate";
        OkHttpClient Client = new OkHttpClient();
        RequestBody Body = RequestBody.create(MEDIA_TYPE, jsonStr);
        Request request2 = new Request.Builder()
                .url(URL)
                .post(Body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();
        Client.newCall(request2).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
                //call.cancel();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String mMessage = response.body().string();
                JSONObject jsonObj = null;
                try {
                    jsonObj = new JSONObject(mMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    SignalrURL[0] = jsonObj.getString("url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    AccessKey[0] = jsonObj.getString("accessToken");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                hub = HubConnectionBuilder.create(SignalrURL[0]).withAccessTokenProvider(Single.defer(() -> {
                    return Single.just(AccessKey[0]);

                }))
                        .build();
                System.out.println("exampleRunnable thread build");
                hub.on(UserID+"Notify", new Action1<String>() {
                    @Override
                    public void invoke(String params) {
                        String title = "New Update!";
                        String message = params;

                        Notification notification = new NotificationCompat.Builder(WelcomeUser.this,App.CHANNEL_1)
                                .setSmallIcon(R.drawable.ic_one)
                                .setContentTitle(title)
                                .setContentText(message)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                .build();

                        notificationManager.notify(1,notification);

                    }
                }, String.class);
                hub.on(UserID+"NotifyFeeds", new Action1<String>() {
                    @Override
                    public void invoke(String params) {
                        System.out.println(params);
                        updateFeeds(UserID,ListView1,MyAdapter1);
                        String title = "New Update!";
                        String message = "There is a new feeds , go check it out :)";

                        Notification notification = new NotificationCompat.Builder(WelcomeUser.this,App.CHANNEL_1)
                                .setSmallIcon(R.drawable.ic_one)
                                .setContentTitle(title)
                                .setContentText(message)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                .build();

                        notificationManager.notify(1,notification);

                    }
                }, String.class);
                System.out.println("exampleRunnable thread hubon");
                hub.start();
                nogotiatedFlag =true;
            }

        });

        MainPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(WelcomeUser.this, user.class);
                intent.putExtra("NAME",getIntent().getStringExtra("NAME"));
                intent.putExtra("AccessKey",AccessKey[0]);
                intent.putExtra("URL",SignalrURL[0]);
                startActivity(intent);
            }
        });
    }

    private void updateFeeds(String userID, ListView listView1, ArrayAdapter myAdapter1) {

        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-requests/"+userID+"/true";
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {

                    final String MyResponse = response.body().string();
                    WelcomeUser.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mutex.lock();
                                Notifications.clear();
                                JSONArray jsonArr = null;
                                try {
                                    jsonArr = new JSONArray(MyResponse);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                for (int i = 0; i < jsonArr.length(); i++) {
                                    JSONObject jsonObj = null;
                                    try {
                                        jsonObj = jsonArr.getJSONObject(i);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println(jsonObj);
                                    String CarNum = "";
                                    try {
                                        CarNum = jsonObj.getString("rowKey");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String Owner = "";
                                    try {
                                        Owner = jsonObj.getString("ownerName");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String isApproved = "";
                                    try {
                                        isApproved = jsonObj.getString("approved");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String row = "";
                                    if (isApproved.equals("true")) {
                                        row = "Request to add " + Owner + "'s car approved!";
                                    } else if (isApproved.equals("false")) {
                                        row = "Request to add " + Owner + "'s car rejected!";
                                    } else {
                                        row = "Check the function!!";
                                    }
                                    Notifications.add(row);
                                }
                            }finally{
                                mutex.unlock();
                            }
                            if(Notifications.size()==0){
                                Notifications.add("There's no notifications at the time!");
                            }
                            listView1.setAdapter(myAdapter1);
                        }
                    });

                }
            }
        });

    }
}
