package MyApp.com;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class user extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        TextView ThisUserName = (TextView)findViewById(R.id.PassUserID);
        ThisUserName.setText(getIntent().getStringExtra("NAME"));
        Button UserRegistBtn = findViewById(R.id.UserRegistBtn);
        Button UserRequestsBtn = findViewById(R.id.UserReqBtn);
        Button ChangeMyPasswordBtn = findViewById(R.id.ChangePasswordBtn);



        UserRegistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(user.this, UserRegistBtn.class);
                intent.putExtra("NAME",getIntent().getStringExtra("NAME"));
                startActivity(intent);
            }
        });

        UserRequestsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(user.this, UserRequestsBtn.class);
                intent.putExtra("NAME",getIntent().getStringExtra("NAME"));
                intent.putExtra("AccessKey",(getIntent().getStringExtra("AccessKey")));
                intent.putExtra("URL",(getIntent().getStringExtra("URL")));
                startActivity(intent);
            }
        });

        ChangeMyPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(user.this, UserChangePasswordBtn.class);
                intent.putExtra("NAME",getIntent().getStringExtra("NAME"));
                startActivity(intent);
            }
        });

    }

}
