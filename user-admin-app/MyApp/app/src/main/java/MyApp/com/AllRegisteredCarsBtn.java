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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllRegisteredCarsBtn extends AppCompatActivity {

    ArrayList<String> RegisteredCars = new ArrayList<String>();
    Button AddBtn;
    Button RemoveBtn;
    EditText input_CarNum;
    EditText input_Owner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_registered_cars_btn);
        input_CarNum=(EditText)findViewById(R.id.editText2);
        input_Owner = (EditText)findViewById(R.id.editText1);
        AddBtn=(Button)findViewById(R.id.AddBtn);
        RemoveBtn=(Button)findViewById(R.id.RemoveBtn);
        final ArrayList<String> CheckBeforeAdd = new ArrayList<String>();
        final ArrayList<String> CheckBeforeRemove = new ArrayList<String>();
        final ListView ListView1 = (ListView) findViewById(R.id.LstView2);
        final ArrayAdapter MyAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, RegisteredCars);
        ListView1.setAdapter(MyAdapter1);
        final HashMap<String,String> rowUsers = new HashMap<String,String>();

        ListView1.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                String[] splitedItem = item.split(" ");
                input_CarNum.setText(splitedItem[0]);
                input_Owner.setText(splitedItem[splitedItem.length-1]);
            }

        });

        // Request :
        OkHttpClient client = new OkHttpClient();
        String url = "https://counterfunctions20200429005139.azurewebsites.net/api/get-regestered-cars/admin";
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
                    RegisteredCars.clear();
                    rowUsers.clear();
                    final String MyResponse = response.body().string();
                    AllRegisteredCarsBtn.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Rows.clear();
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
                                    Owner = jsonObj.getString("partitionKey");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String User= "";
                                try {
                                    User = jsonObj.getString("userName");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String row = RegCar + " ; Owner is : " + Owner;
                                //System.out.println(Owner);
                                //AllRegisteredCarsBtnRow row = new AllRegisteredCarsBtnRow(RegCar,Owner);
                                //Rows.add(row);
                                RegisteredCars.add(row);
                                rowUsers.put(row,User);
                                CheckBeforeAdd.add(RegCar);
                                CheckBeforeRemove.add(Owner);
                            }
                            /*RowsListAdapter adapter = new RowsListAdapter(this,R.layout.adapter_view_layout,Rows);
                            ListView1.setAdapter(adapter);*/
                            ListView1.setAdapter(MyAdapter1);

                        }
                    });
                }
            }
        });

        AddBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                int c = 0;
                final String CarNum = input_CarNum.getText().toString();
                final String Owner = input_Owner.getText().toString();
                for (int i = 0; i < CheckBeforeAdd.size(); i++) {
                    if (CheckBeforeAdd.get(i).equals(CarNum)) {
                        c++;
                    }
                }
                boolean isNumeric = CarNum.chars().allMatch( Character::isDigit );
                if(!isNumeric || (CarNum.length()!=7 && CarNum.length()!=8)) { c=-1;}
                for(int j=0; j<Owner.length() ; j++){
                    if(Owner.length()<4){
                        c=-3;
                        break;
                    }
                    if(Character.isDigit(Owner.charAt(j)) && !(Character.compare((Owner.charAt(j)),'_')==-1) && !(Character.compare((Owner.charAt(j)),'-')==-1)){
                        c=-2;
                    }
                }

                    if((c==0) && !CarNum.equals("") && !Owner.equals("") ) {
                        String add_url = "https://counterfunctions20200429005139.azurewebsites.net/api/add-PlateNumber/admin/"+CarNum+"/"+Owner;
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
                                    AllRegisteredCarsBtn.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String row = CarNum + " ; Owner is : " + Owner;
                                            RegisteredCars.add(row);
                                            ListView1.setAdapter(MyAdapter1);
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    final Toast toast = Toast.makeText(getApplicationContext(), "Added Successfully", Toast.LENGTH_LONG);
                                                    toast.show();
                                                }});
                                            MyAdapter1.notifyDataSetChanged();
                                        }
                                    });

                                }
                            }
                        });

                        //MyAdapter1.notifyDataSetChanged();
                        input_Owner.setText("");
                        input_CarNum.setText("");
                    } else if (c > 0) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                final Toast toast = Toast.makeText(getApplicationContext(), "Car already exists!", Toast.LENGTH_LONG);
                                toast.show();
                            }
                        });
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

        RemoveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = 0;

                final String CarNum = input_CarNum.getText().toString();
                final String Owner = input_Owner.getText().toString();
                final String opitinalRow = CarNum + " ; Owner is : " + Owner;
                if (RegisteredCars.contains(opitinalRow)) {
                    final String row = opitinalRow;
                    String getUser = rowUsers.get(row);
                    String remove_url = "https://counterfunctions20200429005139.azurewebsites.net/api/remove-PlateNumber/"+getUser+"/"+CarNum+"/"+Owner;
                    OkHttpClient client3 = new OkHttpClient();
                    Request request3 = new Request.Builder()
                                .url(remove_url)
                                .build();
                    //final int finalI = i;
                    client3.newCall(request3).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            e.printStackTrace();
                        }
                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                AllRegisteredCarsBtn.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String row = CarNum + " ; Owner is : " + Owner;
                                        RegisteredCars.remove(row);
                                        ListView1.setAdapter(MyAdapter1);
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

                }

                if (c == 0) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Toast toast = Toast.makeText(getApplicationContext(), "There's no such user!", Toast.LENGTH_LONG);
                            toast.show();
                        }});
                }
                    }});
        }
                    }







