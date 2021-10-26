package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*import com.microsoft.signalr.Action1;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.OnClosedCallback;*/

import com.microsoft.signalr.Action1;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class admin extends AppCompatActivity {
    NotificationManagerCompat notificationManager;
    static HubConnection hub;
    static AdminRequestsBtn admiRequestClass;
    Boolean nogotiatedFlag = false;
    static Button ReqBtn;
    //static ReentrantLock mutex = new ReentrantLock(true);
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
        setContentView(R.layout.activity_admin);


        notificationManager = NotificationManagerCompat.from(this);
        Button AllCarsBtn = (Button)findViewById(R.id.AllCarsBtn);
        Button RegCarsBtn = (Button)findViewById(R.id.RegCarsBtn);
        ReqBtn = (Button)findViewById(R.id.ReqBtn);
        Button ShowUsersBtn = (Button)findViewById(R.id.ShowUsersBtn);
        Button ChangePassword = (Button)findViewById(R.id.AdminChangePass);
        final TextView AvailablePlaces = (TextView)findViewById(R.id.textView9);
        final String[] AccessKey = new String[1];
        final String[] SignalrURL = new String[1];

        // JSON Building:
        JSONObject json = new JSONObject();
        try {
            json.put("UserId", "admin");
        }catch (JSONException e){
            e.printStackTrace();
        }
        String jsonStr = json.toString();
        MediaType MEDIA_TYPE = MediaType.parse("application/json");
        String URL = "https://counterfunctions20200429005139.azurewebsites.net/api/negotiate";
        OkHttpClient Client = new OkHttpClient();
        RequestBody Body = RequestBody.create(MEDIA_TYPE, jsonStr);
        Request request = new Request.Builder()
                .url(URL)
                .post(Body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();
        Client.newCall(request).enqueue(new Callback() {
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
                nogotiatedFlag =true;
            }

        });
        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-places-number/?";
        Request request2 = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request2).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                if (response.isSuccessful()) {
                    final String MyResponse = response.body().string();
                    admin.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int numOfTry=0;
                            while(!nogotiatedFlag){ // if there is no nogotiation keep waiting
                                try {
                                    System.out.println("go to sleep");
                                    Thread.sleep(200);
                                    numOfTry++;
                                    if(numOfTry==50){
                                        throw new Exception("Waited for the request too much time !");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return;
                                }
                            }

                            try {
                                hub = HubConnectionBuilder.create(SignalrURL[0]).withAccessTokenProvider(Single.defer(() -> {
                                    return Single.just(AccessKey[0]);

                                }))
                                        .build();

                                hub.on("placesUpdate", new Action1<String>() {
                                    @Override
                                    public void invoke(String params) {

                                        AvailablePlaces.setText(params);
                                    }
                                }, String.class);
                                hub.on("adminNotify", new Action1<String>() {
                                    @Override
                                    public void invoke(String params) {
                                        String title = "New Update!";
                                        String message = params;

                                        Notification notification = new NotificationCompat.Builder(admin.this, App.CHANNEL_1)
                                                .setSmallIcon(R.drawable.ic_one)
                                                .setContentTitle(title)
                                                .setContentText(message)
                                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                                .build();

                                        notificationManager.notify(1, notification);

                                    }
                                }, String.class);

                                hub.start();


                                AvailablePlaces.setText(MyResponse);
                                //adding request
                                hub.on("AddRequestNotify", new Action1<String>() {
                                    @Override
                                    public void invoke(String params) {
                                        String title = "New Update!";
                                        String message = params + " made new request";

                                        Notification notification = new NotificationCompat.Builder(admin.this, App.CHANNEL_1)
                                                .setSmallIcon(R.drawable.ic_one)
                                                .setContentTitle(title)
                                                .setContentText(message)
                                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                                .build();

                                        notificationManager.notify(1, notification);
                                        if (admiRequestClass == null) {
                                            admiRequestClass = new AdminRequestsBtn();
                                        }
                                        admiRequestClass.getRequests(null, null, true, admin.this);


                                    }
                                }, String.class);
                                hub.on("RemoveRequestNotify", new Action1<String>() {
                                    @Override
                                    public void invoke(String params) {
                                        String title = "New Update!";
                                        String message = params + " removed his request";
                                        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                        Notification notification = new NotificationCompat.Builder(admin.this, App.CHANNEL_1)
                                                .setSmallIcon(R.drawable.ic_one)
                                                .setContentTitle(title)
                                                .setContentText(message)
                                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                                .build();
                                        long[] vibrate = {0, 100, 200, 300};
                                        notification.vibrate = vibrate;
                                        notification.sound = alarmSound;

                                        notificationManager.notify(1, notification);
                                        if (admiRequestClass == null) {
                                            admiRequestClass = new AdminRequestsBtn();
                                        }
                                        admiRequestClass.getRequests(null, null, true, admin.this);

                                    }
                                }, String.class);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    });

                }
            }
        });

        AllCarsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(admin.this, AllCarsBtn.class);
                startActivity(intent);
            }
        });

        RegCarsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(admin.this, AllRegisteredCarsBtn.class);
                startActivity(intent);
            }
        });
        if(admiRequestClass==null) {
            admiRequestClass = new AdminRequestsBtn();
        }
        try {
            admiRequestClass.getRequests(null,null,true,admin.this);
        }catch(Exception e){
            e.printStackTrace();
        }




        ReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(admin.this, AdminRequestsBtn.class);
                startActivity(intent);
            }
        });

        ShowUsersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(admin.this, AdminAllUsersBtn.class);
                startActivity(intent);
            }
        });

        ChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(admin.this, AdminChangePasswordBtn.class);
                startActivity(intent);
            }
        });

    }
}
