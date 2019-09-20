package com.erg.reco;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    Socket connection = null;
    public static ObjectInputStream in;
    public static ObjectOutputStream out;
    public static String ip = "192.168.1.17";
    public static int port = 4200;

    private static  GoogleMap mMap;
    public Button go;
    public Button ipgo;
    public int user;
    public EditText number;
    public EditText iptxt;
    private ArrayList<LatLng> latlngs = new ArrayList<>();
    private ArrayList<Marker> markers = new ArrayList<>();
    public ArrayList<Poi> pois ;
    private MarkerOptions options = new MarkerOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        go = (Button) findViewById(R.id.go);
        number = (EditText) findViewById(R.id.number);
        ipgo = (Button) findViewById(R.id.ipgo);
        iptxt = (EditText) findViewById(R.id.iptxt);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED
                )
        {
            return;
        }

        mMap.setMyLocationEnabled(true); //gps on ?
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        ipgo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ip = iptxt.getText().toString();

                Context context = getApplicationContext();
                CharSequence text = "Setting ip: " + ip + " .";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = number.getText().toString();
                if (!value.equalsIgnoreCase("")) //not empty
                {
                    user = Integer.parseInt(value);
                } else {
                    user = 0;
                }
                AsyncTaskRunner runner = new AsyncTaskRunner();
                runner.execute(user);

                Context context = getApplicationContext();
                CharSequence text = "Getting Pois for user: " + user + " .";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });


    }

    public void onResume(ArrayList<Poi> pois) {
        super.onResume();
        System.out.println("Updating map =======================================");
        latlngs.clear();
        markers.clear();
        mMap.clear();
        LatLng tmp;
        for (int i = 0; i < pois.size(); i++) {
            tmp = new LatLng(pois.get(i).getLatitude(), pois.get(i).getLongitude());
            latlngs.add(tmp);
            options.position(tmp);
            options.title(pois.get(i).getName());
            options.snippet(pois.get(i).getCategory());
            markers.add(mMap.addMarker(options));
        }

        Log.e("debug","Before");
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlngs.get(0)));
        Log.e("debug","after");

        mMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
        mMap.setTrafficEnabled(true);
    }

    private class AsyncTaskRunner extends AsyncTask<Integer,String , ArrayList<Poi>> {

        @Override
        protected ArrayList<Poi> doInBackground(Integer... user) {
            pois = new ArrayList<Poi>();
            try {
                connection = new Socket(ip, port); // Server's ip,port
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());

                //TYPE
                //0 is worker
                //1 is client
                out.writeInt(1);
                out.flush();

                out.writeInt(user[0]);//todo
                Log.e("debug",String.valueOf(user[0]));
                out.flush();

                pois = (ArrayList<Poi>)in.readObject();

                out.close();
                in.close();


            } catch (Exception e) {
                System.err.println("Error -- openClient.");
                System.err.println(e.getMessage()+ " "+ e);
            }

            return pois;//TODO
        }


        @Override
        protected void onPostExecute(ArrayList<Poi> result) {
            // execution of result of Long time consuming operation
            onResume(result);
        }


        @Override
        protected void onPreExecute() {

        }


        @Override
        protected void onProgressUpdate(String... text) {

        }
    }
}


