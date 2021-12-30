package com.patrick.vaccinetracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.example.vaccinetracker.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlLineString;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlPoint;
import org.osmdroid.bonuspack.kml.KmlPolygon;
import org.osmdroid.bonuspack.kml.KmlTrack;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static List<NationalData> statesData;
    public static List<NationalData> countiesData;
    boolean filled = false;
    MapView map;
    int selected = 0;
    int currentMode = 1;
    boolean usingStates = false;
    List<Overlay> stateContainer;
    List<Overlay> countyContainer;
    List<TextMarker> textMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //fetch data
        Intent dataRetrieval = new Intent(getApplicationContext(), RetriverActivity.class);
        startActivity(dataRetrieval);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!filled && statesData != null) {
            finishConfig();
            filled = true;
        }
    }

    private void finishConfig() {
        //do this so map properly loads
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_map);

        //setup map
        map = findViewById(R.id.map_mainView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(5.0);
        map.setMaxZoomLevel(11.0);
        map.setScrollableAreaLimitLongitude(-180.0,  -45.5, 1);
        map.setScrollableAreaLimitLatitude(84.2 ,-10, 1);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        IMapController mapController = map.getController();
        mapController.setCenter(new GeoPoint(39.1,-98.6));
        mapController.setZoom(5.0);

        //add overlay to detect touches outside of states
        Overlay touch = new Overlay() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                super.onSingleTapConfirmed(e, mapView);
                select(-1);
                showButton(false);
                return true;
            }
        };
        map.getOverlays().add(touch);

        //add states/counties
        generateKML();
        switchStateCountyMode();

        //configure button
        showButton(false);
        Button goToState = findViewById(R.id.map_goToState);
        goToState.setOnClickListener(v -> {
            Intent sDetails = new Intent(this, DetailsActivity.class);
            sDetails.putExtra("StateIndex", selected);
            startActivity(sDetails);
        });

        //setup Autofill
        AutoCompleteTextView ac = findViewById(R.id.map_autofill);
        ArrayAdapter<String> acOptions = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, NationalData.namesList);
        ac.setAdapter(acOptions);
        ac.setOnItemClickListener((parent, view, position, id) -> select(Arrays.asList(NationalData.namesList).indexOf(parent.getItemAtPosition(position))));

        //setup spinner
        Spinner mode = findViewById(R.id.map_currentMode);
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = mode.getSelectedItem().toString();
                //update mode
                switch(item) {
                    case "Single Dose": currentMode = 1;
                        break;
                    case "Fully Vaccinated": currentMode = 2;
                        break;
                    case "Booster Shot": currentMode = 3;
                        break;
                }
                //re-color states
                updatePolys(getData(), getVacColor());

                //refresh!
                map.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });

        //setup leaderboard button
        ImageButton toLeader = findViewById(R.id.map_toLeaderboard);
        toLeader.setOnClickListener(v -> {
            Intent leaderAct = new Intent(this, LeaderboardActivity.class);
            startActivity(leaderAct);
        });

        //Maybe add dark mode filter in the future?
    }

    private void switchStateCountyMode() {
        //change mode
        usingStates = !usingStates;

        //disable unneeded polys, enable needed polys.
        List<Overlay> disable = usingStates ? countyContainer : stateContainer;
        List<Overlay> enable = usingStates ? stateContainer : countyContainer;
        for(Overlay o : disable) {
            o.setEnabled(false);
        }
        for(Overlay o : enable) {
            o.setEnabled(true);
        }

        //re-color polys
        updatePolys(getData(), getVacColor());

        //refresh map
        map.invalidate();
    }


    //depending on current mode, create list of all percentages per state
    private List<Double> getData() {
        ArrayList<Double> data = new ArrayList<>();
        List<NationalData> pull = usingStates ? statesData : countiesData;

        switch(currentMode) {
            case 1: //dose
                for (NationalData s : pull) {
                    data.add(s.getPDose()/100);
                }
                break;
            case 2: //full
                for (NationalData s : pull) {
                    data.add(s.getPVax()/100);
                }
                break;
            case 3: //boost
                for (NationalData s : pull) {
                    data.add(s.getPBoost()/100);
                }
                break;
        }
        return data;
    }

    //depending on mode, get data for a single state
    private Double getSingleData(int id) {
        switch(currentMode) {
            case 1: return statesData.get(id).getPDose()/100;
            case 2: return statesData.get(id).getPVax()/100;
            case 3: return statesData.get(id).getPBoost()/100;
        }
        return 0.0;
    }

    //get respective color per current mode
    private int getVacColor() {
        switch(currentMode) {
            case 1: return getResources().getColor(R.color.dose);
            case 2: return getResources().getColor(R.color.both);
            case 3: return getResources().getColor(R.color.boost);
        }
        return 0;
    }

    //parse KML and add polys
    private void generateKML() {
        //Begin with states
        //load in KML file
        KmlDocument file = new KmlDocument();
        file.parseKMLStream(getResources().openRawResource(R.raw.states), null);

        //create the custom KMLStyler
        KMLStyler styler = new KMLStyler();

        //create the actual polys, then add to map.
        FolderOverlay overlay = (FolderOverlay) file.mKmlRoot.buildOverlay(map, null, styler, file);
        stateContainer = ((FolderOverlay) overlay.getItems().get(0)).getItems();
        map.getOverlays().add(overlay);

        //add state labels
        textMarkers = addLabels();

        /*
        //Add counties -- TBA
        file.parseKMLStream(getResources().openRawResource(R.raw.counties), null);
        overlay = (FolderOverlay) file.mKmlRoot.buildOverlay(map, null, styler, file);
        countyContainer = ((FolderOverlay) overlay.getItems().get(0)).getItems();
        map.getOverlays().add(overlay);*/
        countyContainer = new ArrayList<Overlay>();
    }

    private List<TextMarker> addLabels() {
        //create text abbreviations
        List<TextMarker> markers = new ArrayList<TextMarker>();
        for(int x = 0; x < 52; x++) {
            Overlay o = stateContainer.get(x);
            //make it BBox center for best approximation
            GeoPoint center = o.getBounds().getCenterWithDateLine();
            String abb = statesData.get(x).getAbbreviation();
            //change position so it fits inside state for certain ones
            switch(abb) {
                case "FL":  center.setCoords(28.5, -81.7);
                    break;
                case "MD":  center.setCoords(39.35, -76.9);
                    break;
                case "LA":  center.setCoords(31.3, -92.4);
                    break;
                case "MI":  center.setCoords(43.6, -84.7);
                    break;
                case "ID":  center.setCoords(43.8, -115.7);
                    break;
                case "AK":  center.setCoords(65.0, -150.7);
                    break;
                case "MA":  center.setCoords(42.3, -72.1);
                    break;
            }

            //create the label with the abbreviation
            TextMarker text1 = new TextMarker(map);
            text1.setTextLabelBackgroundColor(Color.TRANSPARENT);
            text1.setPosition(center);
            text1.setTextLabelFontSize(32);
            text1.setTextLabelForegroundColor(Color.WHITE);
            text1.setTextIcon(abb);
            text1.setInfoWindow(null);
            text1.setPanToView(false);
            text1.setOnMarkerClickListener(null);

            //add to map
            map.getOverlays().add(text1);

            markers.add(text1);
        }
        return markers;
    }

    //re-color a single state
    private BoundingBox updatePoly(int id, double pct, int color, float width, List<Overlay> polyContainer) {
        Overlay poly = polyContainer.get(id);
        int thisColor = getColorFromPercent(pct,color & 0xFFFFFF, 0xFF000000 + color);
        //if it is a folder, all of its children are polys of the same state.
        if(poly instanceof FolderOverlay) {
            List<Overlay> subItems = ((FolderOverlay) poly).getItems();
            for(Overlay o : subItems) {
                //get poly and update
                Polygon p = (Polygon)o;
                p.getFillPaint().setColor(thisColor);
                p.getOutlinePaint().setColor(0xBF3F3F3F);
                p.getOutlinePaint().setStrokeWidth(width);
                p.setOnClickListener(new SelectionListener(id));
            }
        } else {
            //same procedure as above
            Polygon p = (Polygon)poly;
            p.getFillPaint().setColor(thisColor);
            p.getOutlinePaint().setColor(0xBF3F3F3F);
            p.getOutlinePaint().setStrokeWidth(width);
            p.setOnClickListener(new SelectionListener(id));
        }

        //update respective marker
        TextMarker t = textMarkers.get(id);
        t.setOnMarkerClickListener(new SelectionListener(id));

        //return bounds for selection
        return poly.getBounds();
    }

    //update color of every state
    private void updatePolys(List<Double> percents, int color) {
        List<Overlay> pCont = usingStates ? stateContainer : countyContainer;
        for(int index = 0; index < pCont.size(); index++) {
            updatePoly(index, percents.get(index), color, 1.0f, pCont);
        }
    }

    //update selected variable and color polys accordingly
    public void select(int id) {
        List<Overlay> pCont = usingStates ? stateContainer : countyContainer;
        if(selected >= 0)
            updatePoly(selected, getSingleData(selected), getVacColor(), 1.0f, pCont);
        if (id >= 0) {
            BoundingBox box = updatePoly(id, .8, 0xFFFF8F, 11.0f, pCont);
            if(id == 1) {
                map.getController().animateTo(new GeoPoint(64.2, -149.49), 7.0, 700L);
            } else {
                map.getController().animateTo(box.getCenterWithDateLine(), 7.0, 700L);
            }
            showButton(true);
        }
        selected = id;
        map.invalidate();
        Log.d("SELECTED", String.valueOf(id));
    }

    //animate selection
    private void showButton(boolean show) {
        Transition transition = new Slide(Gravity.BOTTOM);
        transition.setDuration(600L);
        transition.addTarget(R.id.map_goToState);
        View button = findViewById(R.id.map_goToState);

        TransitionManager.beginDelayedTransition(findViewById(R.id.map_root), transition);
        button.setVisibility(!show ? View.GONE : View.VISIBLE);
        button.bringToFront();
    }

    //change color based on gradient. May rewrite
    public static int getColorFromPercent(double data, int start, int end) {
        int newAlpha = (int)((convert(end,24) - convert(start,24))*data) + convert(start,24);
        int newRed = (int)((convert(end,16) - convert(start,16))*data) + convert(start,16);
        int newGreen = (int)((convert(end,8) - convert(start,8))*data) + convert(start,8);
        int newBlue = (int)((convert(end,0) - convert(start,0))*data) + convert(start,0);

        return (newAlpha << 24) + (newRed << 16) + (newGreen << 8) + newBlue;

    }

    //extract a color from a color int
    public static int convert(int color, int shiftAmt) {
        return (color >> shiftAmt) & 0xff;
    }

    //custom styler (mainly to disable features not wanted)
    class KMLStyler implements KmlFeature.Styler {

        @Override
        public void onFeature(Overlay overlay, KmlFeature kmlFeature) {

        }

        @Override
        public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {

        }

        @Override
        public void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString) {

        }

        @Override
        public void onPolygon(Polygon polygon, KmlPlacemark kmlPlacemark, KmlPolygon kmlPolygon) {
            polygon.getOutlinePaint().setStrokeWidth(1.0f);
        }

        @Override
        public void onTrack(Polyline polyline, KmlPlacemark kmlPlacemark, KmlTrack kmlTrack) {

        }
    }

    //listener to select states when tapped
    class SelectionListener implements Polygon.OnClickListener, Marker.OnMarkerClickListener {
        int thisId;
        public SelectionListener(int id) {
            thisId = id;
        }

        @Override
        public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
            Log.d("POLYTAPPED", String.valueOf(thisId));
            select(thisId);
            return true;
        }

        @Override
        public boolean onMarkerClick(Marker marker, MapView mapView) {
            Log.d("MARKERTAPPED", String.valueOf(thisId));
            select(thisId);
            return true;
        }
    }

    //custom marker that allows for text to be configured
    class TextMarker extends Marker {
        public TextMarker(MapView mapView) {
            super(mapView);
        }

        public void setTextIcon(final String pText) {
            final Paint background = new Paint();
            background.setColor(mTextLabelBackgroundColor);
            final Paint p = new Paint();
            p.setTextSize(mTextLabelFontSize);
            p.setColor(Color.BLACK);
            p.setAntiAlias(true);
            p.setTypeface(Typeface.SANS_SERIF);
            p.setTextAlign(Paint.Align.LEFT);
            final int width = (int) (p.measureText(pText) + 0.5f);
            final float baseline = (int) (-p.ascent() + 0.5f);
            final int height = (int) (baseline + p.descent() + 0.5f);
            final Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            final Canvas c = new Canvas(image);
            p.setStrokeWidth(6f);
            p.setStyle(Paint.Style.STROKE);
            c.drawPaint(background);
            c.drawText(pText, 0, baseline, p);
            p.setColor(mTextLabelForegroundColor);
            p.setStrokeWidth(0f);
            p.setStyle(Paint.Style.FILL);
            c.drawText(pText,0,baseline,p);
            mIcon = new BitmapDrawable(mResources, image);
            setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        }
    }
}