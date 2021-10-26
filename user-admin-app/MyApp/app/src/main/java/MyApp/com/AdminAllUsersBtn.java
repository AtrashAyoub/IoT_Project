package MyApp.com;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdminAllUsersBtn extends AppCompatActivity {

    public static ArrayList<String> Users = new ArrayList<String>(); //adding static makes problem! -- solved
    ReentrantLock mutex = new ReentrantLock(true);
    public void updateUserView( final ListView ListView1, final ArrayAdapter MyAdapter1) throws IOException {
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
            final String MyResponse = response.body().string();
            AdminAllUsersBtn.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                        try {
                            mutex.lock();

                        AdminAllUsersBtn.Users.clear();
                        MainActivity.users_cars.clear();
                        MainActivity.users_passwords.clear();
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
                            Users.add(part);
                            str = str + part + " ";
                        }
                        if(Users.contains("admin")) {
                            Users.remove("admin");
                        }
                        ListView1.setAdapter(MyAdapter1);
                        MyAdapter1.notifyDataSetChanged();
                    }catch(Exception e){
                            e.printStackTrace();
                        }finally{
                            mutex.unlock();
                        }
                }
            });

        }
    }
});
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_all_users_btn);
        final ListView ListView1 = (ListView) findViewById(R.id.LstView1);
        Button AddBtn = (Button) findViewById(R.id.AddBtn);
        Button RemoveBtn = (Button) findViewById(R.id.RemoveBtn);
        final EditText InputText = (EditText) findViewById(R.id.editText2);


        final ArrayAdapter MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Users);

        ListView1.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                InputText.setText(item);
            }

        });



        try {
            updateUserView( ListView1,MyAdapter1);
        } catch (IOException e) {
            e.printStackTrace();
        }
            //ADD Button
            AddBtn.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onClick(View v) {


                    int c = 0;
                    final String stringval;
                    try {
                        mutex.lock();
                        stringval = InputText.getText().toString();
                        for (int i = 0; i < Users.size(); i++) {
                            if (Users.get(i).equals(stringval)) {
                                c++;
                            }
                        }
                    }finally{
                        mutex.unlock();
                    }
                    if(stringval.equals("admin")){
                        c=-1;
                    }
                    if(stringval.length()<4 || !(stringval.matches("^[a-zA-Z]*$"))){
                        c=-2;
                    }
                    if (!stringval.equals("") && (c == 0) && (c!=-1) &&(c != -2)) {
                        // Update azure table:
                        String add_url = "https://counterfunctions20200429005139.azurewebsites.net/api/update-User/add/" + stringval;
                        OkHttpClient client2 = new OkHttpClient();
                        Request request2 = new Request.Builder()
                                .url(add_url)
                                .build();
                        client2.newCall(request2).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    AdminAllUsersBtn.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                updateUserView( ListView1,MyAdapter1);
                                                MyAdapter1.notifyDataSetChanged();
                                                Toast.makeText(getApplicationContext(), "User Added Successfully", Toast.LENGTH_LONG).show();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                }
                            }
                        });

                        InputText.setText("");
                    } else if (c > 0) {
                        Toast.makeText(getApplicationContext(), "User already exists!", Toast.LENGTH_LONG).show();
                    }else if(c==-1){
                        Toast.makeText(getApplicationContext(), "Invalid input!", Toast.LENGTH_LONG).show();
                    }else if(c==-2){
                        Toast.makeText(getApplicationContext(), "User must be 4 CHARS at least without numbers!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "User can't be empty!", Toast.LENGTH_LONG).show();
                    }
                }
            });

            //REMOVE Button
            RemoveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int c = 0;
                    for (int i = 0; i < Users.size(); i++) {
                        String getname = InputText.getText().toString();
                        if(getname.equals("admin")){
                            c=-1;
                            break;
                        }
                        if (Users.contains(getname)) {
                            // Update azure table:
                            String remove_url = "https://counterfunctions20200429005139.azurewebsites.net/api/update-User/remove/" + getname;
                            OkHttpClient client3 = new OkHttpClient();
                            Request request3 = new Request.Builder()
                                    .url(remove_url)
                                    .build();
                            client3.newCall(request3).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    e.printStackTrace();
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                    if (response.isSuccessful()) {
                                        AdminAllUsersBtn.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    updateUserView( ListView1,MyAdapter1);
                                                    MyAdapter1.notifyDataSetChanged();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                }
                            });

                            //MyAdapter1.notifyDataSetChanged();
                            InputText.setText("");
                            c++;
                            Toast.makeText(getApplicationContext(), "User has been removed!", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                    if (c == 0) {
                        Toast.makeText(getApplicationContext(), "There's no such user!", Toast.LENGTH_LONG).show();
                    }
                    if (c == -1) {
                        Toast.makeText(getApplicationContext(), "Invalid input!", Toast.LENGTH_LONG).show();
                    }
                }
            });




    }
}

