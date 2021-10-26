package MyApp.com;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RowsListAdapter extends ArrayAdapter<AllRegisteredCarsBtnRow> {
    public static final String TAG = "RowsListAdapter";
    private Context mContext;
    int mResource;

    public RowsListAdapter(@NonNull Runnable context, int resource, @NonNull ArrayList<AllRegisteredCarsBtnRow> objects) {
        super((Context) context, resource, objects);
        mContext = mContext;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String owner = getItem(position).getOwner();
        String regCar = getItem(position).getRegistered_car();

        AllRegisteredCarsBtnRow row = new AllRegisteredCarsBtnRow(regCar,owner);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent,false);

        TextView tvRegCar = (TextView) convertView.findViewById(R.id.textView1);
        TextView tvOwner = (TextView) convertView.findViewById(R.id.textView2);
        tvRegCar.setText(regCar);
        tvOwner.setText(owner);

        return convertView;
    }
}
