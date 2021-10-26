package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminChangePasswordBtn extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_change_password_btn);
        final EditText InputText = (EditText) findViewById(R.id.NewPasswordTxt);
        Button ChangeBtn = (Button)findViewById(R.id.ChangeBtn);

        ChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stringval = InputText.getText().toString();
                if (stringval.length()<4) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Password must have at least 4 characters!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }else if (stringval.contains(" ") || stringval.contains("   ")) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Password can't include spaces/tabs!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                } else {
                    // JSON Building:
                    JSONObject json = new JSONObject();
                    try {
                        json.put("PartitionKey", "admin");
                        json.put("Password", stringval);
                        json.put("RowKey", "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String jsonStr = json.toString();
                    System.out.println(jsonStr);
                    MediaType MEDIA_TYPE = MediaType.parse("application/json");
                    String URL = "https://counterfunctions20200429005139.azurewebsites.net/api/post-changePass";
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
                            if (mMessage.equals("\"changed\"")) {
                                InputText.setText("");
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        final Toast toast = Toast.makeText(getApplicationContext(), "Password changed successfully!", Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                });
                                ;

                            } else {
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
            }
        });
    }
}
