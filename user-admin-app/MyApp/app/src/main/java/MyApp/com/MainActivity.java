package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    EditText _txtUser, _txtPass;
    Button _btnLogin;
    Spinner _spinner;
    public static ArrayList<String> users_cars=new ArrayList<String>();
    public static ArrayList<String> users_passwords=new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _txtPass=(EditText)findViewById(R.id.txtPass);
        _txtUser=(EditText)findViewById(R.id.txtUser);
        _btnLogin=(Button)findViewById(R.id.btnLogin);
        //_spinner=(Spinner)findViewById(R.id.spinner);

        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-All-Users/";
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
                    String MyResponse = response.body().string();

                    String str = MyResponse.replaceAll("[^a-zA-Z0-9]", " ");
                    String[] parts = str.split("   ");
                    System.out.println(str);
                    for (int i = 0; i < parts.length; i++) {
                        parts[i] = parts[i].replace(" ", "");
                        users_cars.add(parts[i]);
                        users_passwords.add("user"); //default password ~> Ask Obaida if there's passwords list in the azure
                    }

                }
            }
        });

        //ArrayAdapter <CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.usertype, R.layout.support_simple_spinner_dropdown_item);
        //_spinner.setAdapter(adapter);
        _btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String PassUserName = PassUser.getText().toString();
                //String item = _spinner.getSelectedItem().toString();
                //admin login
                /*if(_txtUser.getText().toString().equals("admin")&& _txtPass.getText().toString().equals("admin")) {
                    Intent intent = new Intent(MainActivity.this, admin.class);
                    startActivity(intent);
                    //user login
                }else{*/
                    // JSON Building:
                    JSONObject json = new JSONObject();
                    try {
                        json.put( "PartitionKey", (_txtUser.getText().toString()));
                        json.put("Password", (_txtPass.getText().toString()));
                        json.put("RowKey", "");
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    String jsonStr = json.toString();
                    System.out.println(jsonStr);
                    MediaType MEDIA_TYPE = MediaType.parse("application/json");
                    String URL = "https://counterfunctions20200429005139.azurewebsites.net/api/post-login";
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
                            System.out.println(mMessage);
                       if(mMessage.equals("\"user\"")){
                                //Intent intent = new Intent(MainActivity.this, user.class);
                                Intent intent = new Intent(MainActivity.this, WelcomeUser.class);
                                intent.putExtra("NAME",_txtUser.getText().toString()); // to pass the username to another layout
                                startActivity(intent);
                            }
                            else if(mMessage.equals("\"admin\"")){
                                Intent intent = new Intent(MainActivity.this, admin.class);
                                intent.putExtra("NAME",_txtUser.getText().toString()); // to pass the username to another layout
                                startActivity(intent);
                            }else{
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        final Toast toast = Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                });
                            }
                        }
                    });
            }
        });
    }
}
