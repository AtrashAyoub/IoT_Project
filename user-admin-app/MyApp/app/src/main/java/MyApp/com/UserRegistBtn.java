package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserRegistBtn extends AppCompatActivity {
    static ArrayList<String> UserRegistedCars = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_regist_btn);
        final ListView ListView1 = (ListView) findViewById(R.id.MyID);
        final ArrayAdapter MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, UserRegistedCars);
        ListView1.setAdapter(MyAdapter1);
        String UserID = (getIntent().getStringExtra("NAME"));
        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-regestered-cars/"+UserID;
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
                    UserRegistedCars.clear();
                    final String MyResponse = response.body().string();
                    UserRegistBtn.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
                                System.out.println("in for "+i);
                                System.out.println(jsonObj);
                                String RegCar = "";
                                try {
                                    RegCar = jsonObj.getString("rowKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String Owner = "";
                                try {
                                    Owner = jsonObj.getString("partitionKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(RegCar);
                                String row = RegCar + " ; Owner is : " + Owner;
                                UserRegistedCars.add(row);
                            }
                            ListView1.setAdapter(MyAdapter1);
                        }
                    });

                }
            }
        });




    }
}
