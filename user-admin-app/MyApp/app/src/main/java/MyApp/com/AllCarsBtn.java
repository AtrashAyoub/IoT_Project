package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.microsoft.signalr.Action1;
import com.microsoft.signalr.HubConnectionState;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllCarsBtn extends AppCompatActivity {
    static ArrayList<String> CarsInTheGarageNow = new ArrayList<String>();
    ReentrantLock mutex = new ReentrantLock(true);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_cars_btn);

        final ListView ListView3 = (ListView) findViewById(R.id.ListView3);
        final ArrayAdapter MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, CarsInTheGarageNow);
        ListView3.setAdapter(MyAdapter1);

        getCarsNow(ListView3,MyAdapter1);
        while(admin.hub.getConnectionState().equals(HubConnectionState.DISCONNECTED)){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    admin.hub.on("garageUpdate", new Action1<String>() {
        @Override
        public void invoke(String param) {
            getCarsNow(ListView3,MyAdapter1);
        }
    }, String.class);
    }
    private void getCarsNow(ListView listView3, ArrayAdapter myAdapter1) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-garage-content/?";
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
                    AllCarsBtn.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mutex.lock();
                            CarsInTheGarageNow.clear();
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
                                String RegCar = "";
                                try {
                                    RegCar = jsonObj.getString("partitionKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String Owner = "";
                                try {
                                    Owner = jsonObj.getString("ownerName");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String row = Owner + "'s Car: " + RegCar;
                                CarsInTheGarageNow.add(row);
                            }
                            mutex.unlock();
                            listView3.setAdapter(myAdapter1);

                        }
                    });
                }
            }
        });
    }
}
