package MyApp.com;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.microsoft.signalr.Action1;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdminRequestsBtn extends AppCompatActivity {
    static ArrayList<String> Requests = new ArrayList<String>();
    static ArrayList<String> Users_Requesting = new ArrayList<String>();
    static ArrayList<String> CarPlates_Requesting = new ArrayList<String>();
    static Boolean isEmpty=true;
    static Boolean isHere=false;


    @Override
    public void onBackPressed() {
        getRequests(null,null,true,AdminRequestsBtn.this);
        super.onBackPressed();
        isHere=false;
    }


    //private ReentrantLock mutex = new ReentrantLock(true);
    EditText input_CarNum;
    EditText input_Owner;
    HashMap<String,String> rowUsers = new HashMap<String,String>();
    ArrayList<String> CheckBeforeAdd = new ArrayList<String>();
    ArrayList<String> CheckBeforeRemove = new ArrayList<String>();
    ProgressDialog progressDialog;
    static ReentrantLock mutex = new ReentrantLock(true);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_requests_btn);
        final ListView ListView1 = (ListView) findViewById(R.id.LstView2);
        Button ApproveBtn = (Button) findViewById(R.id.ApproveBtn);
        Button RejectBtn = (Button) findViewById(R.id.RejectBtn);
        input_CarNum=(EditText)findViewById(R.id.editText2);
        input_Owner = (EditText)findViewById(R.id.editText1);
        final ArrayAdapter MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Requests);
        isHere=true;

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

        getRequests(ListView1,MyAdapter1,false,AdminRequestsBtn.this);

        admin.hub.on("requestUpdate", new Action1<String>() {
            @Override
            public void invoke(String param) {
                if(isHere) {
                    System.out.println(param.equals("add"));
                    getRequests(ListView1, MyAdapter1, false,AdminRequestsBtn.this);
                }
            }
        }, String.class);
        ApproveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = 0;
                mutex.lock();
                progressDialog.show();
                progressDialog.setContentView(R.layout.progress_dialog);
                progressDialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent
                );
                int size =Requests.size();
                int i=0;
                for (i = 0; i < size ; i++) {
                    final String CarNum = input_CarNum.getText().toString();
                    final String Owner = input_Owner.getText().toString();
                    final String opitinalRow = rowUsers.get(Requests.get(i)) + " : {O: " + Owner + " , C_plate: " + CarNum + " }";
                    if (Requests.contains(opitinalRow)) {
                        final String row = opitinalRow;
                        final String getUser = rowUsers.get(row);
                        String approve_url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-approve-request/true/"+getUser+"/"+Owner+"/"+CarNum;
                        OkHttpClient client3 = new OkHttpClient();
                        Request request3 = new Request.Builder()
                                .url(approve_url)
                                .build();
                        final String finalI = row;
                        client3.newCall(request3).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                e.printStackTrace();
                            }
                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                if (response.isSuccessful()) {

                                    AdminRequestsBtn.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                mutex.lock();
                                                String row = getUser + " : {O: " + Owner + " , C_plate: " + CarNum + " }";
                                                Requests.remove(finalI);
                                                ListView1.setAdapter(MyAdapter1);
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        final Toast toast = Toast.makeText(getApplicationContext(), "Request Approved Successfully", Toast.LENGTH_LONG);
                                                        toast.show();
                                                    }
                                                });
                                                System.out.println(AdminRequestsBtn.Requests.size());
                                                if (AdminRequestsBtn.Requests.size() > 0) {
                                                    admin.ReqBtn.setText("Check Requests!");
                                                    //ReqBtn.setBackgroundResource(R.color.ReqColor);
                                                    admin.ReqBtn.setBackgroundColor(Color.RED);
                                                } else {
                                                    admin.ReqBtn.setText("Requests");
                                                    admin.ReqBtn.setBackgroundResource(android.R.drawable.btn_default);
                                                }
                                                MyAdapter1.notifyDataSetChanged();
                                                progressDialog.dismiss();
                                            }finally{ mutex.unlock();}
                                        }
                                    });
                                }
                            }
                        });

                       // MyAdapter1.notifyDataSetChanged();
                        input_Owner.setText("");
                        input_CarNum.setText("");
                        c++;
                        break;
                    }
                }
                if (i==Requests.size()){
                    progressDialog.dismiss();
                }
                mutex.unlock();
                if (c == 0) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "There's no such request!", Toast.LENGTH_LONG);
                            toast.show();
                        }});
                }

            }});

        RejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = 0;
                mutex.lock();

                progressDialog.show();
                progressDialog.setContentView(R.layout.progress_dialog);
                progressDialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent
                );
                int size = Requests.size();
                int i=0;
                for (i = 0; i < size; i++) {
                    final String CarNum = input_CarNum.getText().toString();
                    final String Owner = input_Owner.getText().toString();
                    final String opitinalRow = rowUsers.get(Requests.get(i)) + " : {O: " + Owner + " , C_plate: " + CarNum + " }";
                    if (Requests.contains(opitinalRow)) {
                        final String row = opitinalRow;
                        final String getUser = rowUsers.get(row);
                        String reject_url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-approve-request/false/"+getUser+"/"+Owner+"/"+CarNum;
                        OkHttpClient client3 = new OkHttpClient();
                        Request request3 = new Request.Builder()
                                .url(reject_url)
                                .build();
                        final String finalI = row;
                        client3.newCall(request3).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                e.printStackTrace();
                            }
                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    AdminRequestsBtn.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                mutex.lock();
                                                String row = getUser + " : {O: " + Owner + " , C_plate: " + CarNum + " }";
                                                Requests.remove(finalI);
                                                ListView1.setAdapter(MyAdapter1);
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        final Toast toast = Toast.makeText(getApplicationContext(), "Request Rejected Successfully", Toast.LENGTH_LONG);
                                                        toast.show();
                                                    }
                                                });

                                                if (AdminRequestsBtn.Requests.size() > 0) {
                                                    admin.ReqBtn.setText("Check Requests!");
                                                    //ReqBtn.setBackgroundResource(R.color.ReqColor);
                                                    admin.ReqBtn.setBackgroundColor(Color.RED);
                                                } else {
                                                    admin.ReqBtn.setText("Requests");
                                                    admin.ReqBtn.setBackgroundResource(android.R.drawable.btn_default);
                                                }
                                                MyAdapter1.notifyDataSetChanged();
                                                progressDialog.dismiss();
                                            }finally{
                                                mutex.unlock();
                                            }
                                        }
                                    });
                                }
                            }
                        });


                        input_Owner.setText("");
                        input_CarNum.setText("");
                        c++;
                        break;
                    }
                }
                if (i==Requests.size()){
                    progressDialog.dismiss();
                }
                mutex.unlock();
                if (c == 0) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "There's no such request!", Toast.LENGTH_LONG);
                            toast.show();
                        }});
                }
            }});

    }




    public void getRequests(ListView listView1, ArrayAdapter myAdapter1, Boolean reutrnIsEmpty, Context context) {

        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-requests/admin/true";
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
                    AdminRequestsBtn.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mutex.lock();
                            //if(progressDialog==null){
                            progressDialog= new ProgressDialog(context);
                           // }
                            progressDialog.show();
                            progressDialog.setContentView(R.layout.progress_dialog);
                            progressDialog.getWindow().setBackgroundDrawableResource(
                                    android.R.color.transparent
                            );

                            JSONArray jsonArr = null;
                            try {
                                jsonArr = new JSONArray(MyResponse);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(reutrnIsEmpty) {
                                if (jsonArr.length() > 0) {
                                    isEmpty = false;

                                } else if ( jsonArr.length() == 0) {
                                    isEmpty = true;

                                }


                            }
                            //Drawable background = admin.ReqBtn.getBackground();
                            System.out.println("******AdminRequestsBtn.Requests.size() ="+AdminRequestsBtn.Requests.size());
                            if(!AdminRequestsBtn.isEmpty){
                                admin.ReqBtn.setText("Check Requests!");
                                //ReqBtn.setBackgroundResource(R.color.ReqColor);
                                admin.ReqBtn.setBackgroundColor(Color.RED);
                            }
                            else{
                                admin.ReqBtn.setText("Requests");
                                admin.ReqBtn.setBackgroundResource(android.R.drawable.btn_default);
                            }
                            if(reutrnIsEmpty){
                                progressDialog.dismiss();
                                mutex.unlock();
                                return;
                            }
                            Requests.clear();
                            rowUsers.clear();
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
                                String User= "";
                                try {
                                    User = jsonObj.getString("partitionKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(RegCar);
                                String row = User + " : {O: " + Owner +" , C_plate: "+RegCar+" }";
                                Requests.add(row);
                                CheckBeforeAdd.add(RegCar);
                                CheckBeforeRemove.add(Owner);
                                rowUsers.put(row,User);
                            }
                            listView1.setAdapter(myAdapter1);
                            progressDialog.dismiss();
                            mutex.unlock();
                        }
                    });
                }

            }
        });


    }
}
