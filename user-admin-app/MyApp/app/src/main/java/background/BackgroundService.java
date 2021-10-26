package background;
import android.app.IntentService;
import android.content.Intent;

import java.io.IOException;

import MyApp.com.AdminAllUsersBtn;
import MyApp.com.MainActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BackgroundService extends IntentService {
    public BackgroundService(){
        super("BackgroundService");
    }

    protected void onHandleIntent(Intent intent){
        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-All-Users/";
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response.isSuccessful()) {
            try {
                System.out.println(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
            AdminAllUsersBtn.Users.clear();
            MainActivity.users_cars.clear();
            MainActivity.users_passwords.clear();
            String MyResponse = response.body().toString();

            String[] parts = MyResponse.split(",");
            String str = "";
            for (int i = 0; i < parts.length; i++) {
                String s = parts[i];
                String part = "";
                for (int j = 0; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (Character.isLetterOrDigit(c)) {
                        part = part + c;
                    }
                }
                AdminAllUsersBtn.Users.add(part);
                str = str + part + " ";
            }
        }
    }

}

