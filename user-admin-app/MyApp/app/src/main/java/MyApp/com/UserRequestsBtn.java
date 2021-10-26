package MyApp.com;

import android.app.Notification;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserRequestsBtn extends AppCompatActivity {

    static ArrayList<String> RequestsList = new ArrayList<String>();
    EditText input_CarNum;
    EditText input_Owner;
    String UserID;
    static int ReqListSize;
    ArrayAdapter MyAdapter1;
    HubConnection hub;
    ProgressDialog progressDialog;

    ReentrantLock mutex = new ReentrantLock(true);

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(hub.getConnectionState().equals(HubConnectionState.DISCONNECTED)){
            hub.stop();
        }

    }

    private void connectHub(final ListView ListView1, final ArrayList<String> CheckBeforeAdd, final ArrayList<String> CheckBeforeRemove){
        hub = HubConnectionBuilder.create((getIntent().getStringExtra("URL"))).withAccessTokenProvider(Single.defer(() -> {
            return Single.just((getIntent().getStringExtra("AccessKey")));

        }))
                .build();
        hub.on(UserID+"NotifyFeeds", new Action1<String>() {
            @Override
            public void invoke(String params) {
                try {
                    updateUserRequests(ListView1,CheckBeforeAdd,CheckBeforeRemove);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, String.class);
        hub.start();
    }
    public void updateUserRequests(final ListView ListView1, final ArrayList<String> CheckBeforeAdd, final ArrayList<String> CheckBeforeRemove)throws IOException {
        UserID = (getIntent().getStringExtra("NAME"));
       /* mutex.lock();
        if(hub==null || hub.getConnectionState().equals(HubConnectionState.DISCONNECTED)){
            connectHub(ListView1,CheckBeforeAdd,CheckBeforeRemove);
        }
        mutex.unlock();*/
       if(MyAdapter1==null) {
           MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, RequestsList);
           ListView1.setAdapter(MyAdapter1);
       }
        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-requests/" + UserID + "/false";
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
                    UserRequestsBtn.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                            mutex.lock();
                            RequestsList.clear();
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
                                String RegCar = "";
                                try {
                                    RegCar = jsonObj.getString("rowKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String Owner = "";
                                try {
                                    Owner = jsonObj.getString("ownerName");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String User = "";
                                try {
                                    User = jsonObj.getString("partitionKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(RegCar);
                                String row = User + " : {O: " + Owner + " , C_plate: " + RegCar + " }";
                                RequestsList.add(row);
                                CheckBeforeAdd.add(RegCar);
                                CheckBeforeRemove.add(Owner);
                            }
                        }finally{
                                ListView1.setAdapter(MyAdapter1);
                                ReqListSize =RequestsList.size();
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
        setContentView(R.layout.activity_user_requests_btn);
        final ListView ListView1 = (ListView) findViewById(R.id.LstView1);
        Button AddRequestBtn = (Button) findViewById(R.id.AddBtn);
        Button RemoveRequestBtn = (Button) findViewById(R.id.RemoveBtn);
        final EditText InputText = (EditText) findViewById(R.id.editText2);
        input_CarNum=(EditText)findViewById(R.id.editText2);
        input_Owner = (EditText)findViewById(R.id.editText1);
        final ArrayList<String> CheckBeforeAdd = new ArrayList<String>();
        final ArrayList<String> CheckBeforeRemove = new ArrayList<String>();
        progressDialog= new ProgressDialog(UserRequestsBtn.this);
        ListView1.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                String[] splitedItem = item.split(" ");
                String carNum=splitedItem[splitedItem.length-2];
                input_CarNum.setText(carNum);
                input_Owner.setText(splitedItem[3]);
            }

        });

        try {
            updateUserRequests( ListView1, CheckBeforeAdd,CheckBeforeRemove);
            mutex.lock();
            if(hub==null || hub.getConnectionState().equals(HubConnectionState.DISCONNECTED)){
                Thread t = new Thread (new Runnable(){
                    public void run() {
                        mutex.lock();
                        hub = HubConnectionBuilder.create((getIntent().getStringExtra("URL"))).withAccessTokenProvider(Single.defer(() -> {
                            return Single.just((getIntent().getStringExtra("AccessKey")));

                        }))
                                .build();
                        hub.on(UserID+"NotifyFeeds", new Action1<String>() {
                            @Override
                            public void invoke(String params) {
                                try {
                                    updateUserRequests(ListView1,CheckBeforeAdd,CheckBeforeRemove);
                                } catch (IOException e) {
                                    System.out.println("hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh mas5ra");
                                    e.printStackTrace();
                                }
                            }
                        }, String.class);
                        hub.start();
                        mutex.unlock();
                    }
                });
                t.start();
               // connectHub(ListView1,CheckBeforeAdd,CheckBeforeRemove);
            }
            mutex.unlock();
        } catch (IOException e) {
            e.printStackTrace();
        }


        AddRequestBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                progressDialog.show();
                progressDialog.setContentView(R.layout.progress_dialog);
                progressDialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent
                );
                int c = 0;
                final String CarNum = input_CarNum.getText().toString();
                final String Owner = input_Owner.getText().toString();

                    if (CheckBeforeAdd.contains(CarNum)) {
                        c++;
                    }
                boolean isNumeric = CarNum.chars().allMatch( Character::isDigit );
                if(!isNumeric || (CarNum.length()!=7 && CarNum.length()!=8)) { c=-1;}
                for(int j=0; j<Owner.length() ; j++){
                    if(Owner.length()<4){
                        c=-3;
                        progressDialog.dismiss();
                        break;
                    }
                    if(Character.isDigit(Owner.charAt(j)) && !(Character.compare((Owner.charAt(j)),'_')==-1) && !(Character.compare((Owner.charAt(j)),'-')==-1)){
                        c=-2;
                        progressDialog.dismiss();
                        break;
                    }
                }
                if((c==0) && !CarNum.equals("") && !Owner.equals("")){
                    String add_url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-action-request/add/"+UserID+"/"+Owner+"/"+CarNum;
                    OkHttpClient client2 = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(add_url)
                            .build();
                    client2.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                final String MyResponse = response.body().string();
                                if(!MyResponse.equals("add")){
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            final Toast toast = Toast.makeText(getApplicationContext(), MyResponse, Toast.LENGTH_LONG);
                                            toast.show();
                                        }});
                                    if(progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                    return;
                                }
                                UserRequestsBtn.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            updateUserRequests( ListView1, CheckBeforeAdd,CheckBeforeRemove);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        if(progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                final Toast toast = Toast.makeText(getApplicationContext(), "Added Successfully", Toast.LENGTH_LONG);
                                                toast.show();
                                            }});
                                    }
                                });

                            }
                        }
                    });

                    MyAdapter1.notifyDataSetChanged();
                    input_Owner.setText("");
                    input_CarNum.setText("");
                } else if (c > 0) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Car already exists!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                    if(progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }else if (c==-1) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Car number size must be 7 or 8!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }else if (c==-2) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Owner must contains only alphabet letters,-,_ ", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }else if (c==-3) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Owner must be 4 letters at least!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_LONG);
                            toast.show();
                        }});
                }
            }
        });

        RemoveRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = 0;
                progressDialog.show();
                progressDialog.setContentView(R.layout.progress_dialog);
                progressDialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent
                );
                    final String CarNum = input_CarNum.getText().toString();
                    final String Owner = input_Owner.getText().toString();
                    String row = UserID + " : {O: " + Owner + " , C_plate: " + CarNum + " }";
                    if (RequestsList.contains(row)) {
                        String remove_url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-action-request/remove/"+UserID+"/"+Owner+"/"+CarNum;
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
                                    UserRequestsBtn.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            try {
                                                updateUserRequests( ListView1, CheckBeforeAdd,CheckBeforeRemove);
                                            } catch (IOException e) {
                                                e.printStackTrace();

                                            }finally{
                                                if(progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                                }
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    final Toast toast = Toast.makeText(getApplicationContext(), "Removed Successfully", Toast.LENGTH_LONG);
                                                    toast.show();
                                                }});
                                        }
                                    });
                                }
                            }
                        });

                        MyAdapter1.notifyDataSetChanged();
                        input_Owner.setText("");
                        input_CarNum.setText("");
                        c++;

                    }else{
                        if(progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    }

                if (c == 0) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "There's no such a request!", Toast.LENGTH_LONG);
                            toast.show();
                        }});
                }
            }});
    }

}

