package org.feup.bondpoint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends Activity {
	static final LatLng HAMBURG = new LatLng(53.558, 9.927);
	static final LatLng KIEL = new LatLng(53.551, 9.993);
	private GoogleMap map;

	private String[] namesStr = null;
	private String[] idsStr = null;
	private String[] latitudesStr = null;
	private String[] longitudesStr = null;
	private UiLifecycleHelper uiHelper;

	private Bitmap userBmp = null;
	private Bitmap mask = null;
	private Bitmap tMask = null;
	private Bitmap result = null;
	private Bitmap newMarkerPic = null;
	private Bitmap friendMarker = null;

	private byte[] byteArray = null;

	private Session session;

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		setContentView(R.layout.activity_map);

		View view = findViewById(android.R.id.content).getRootView();
		Button logoutBtn = (Button) view.findViewById(R.id.logout);

		logoutBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				session = Session.getActiveSession();
				session.closeAndClearTokenInformation();
				MainFragment.setMapStarted(false);
				finish();
			}
		});

		Intent intent = getIntent();

		namesStr = intent.getStringArrayExtra("names");
		idsStr = intent.getStringArrayExtra("ids");
		latitudesStr = intent.getStringArrayExtra("latitudes");
		longitudesStr = intent.getStringArrayExtra("longitudes");

		byteArray = intent.getByteArrayExtra("userPicture");
		userBmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

		int nFriends = namesStr.length - 1;

		LatLng userLocation = new LatLng(
				Double.parseDouble(latitudesStr[nFriends]),
				Double.parseDouble(longitudesStr[nFriends]));

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();

		View marker = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.custom_marker, null);

		Resources resources = view.getResources();

		// bitmap para o marker dos amigos
		friendMarker = Bitmap.createScaledBitmap(
				BitmapFactory.decodeResource(resources, R.drawable.bp_group),
				50, 50, true);

		for (int i = 0; i < nFriends; i++) {
			Log.i("PEOPLE", namesStr[i]);

			map.addMarker(new MarkerOptions()
					.anchor((float) 0.5, (float) 0.5)
					.position(
							new LatLng(Double.parseDouble(latitudesStr[i]),
									Double.parseDouble(longitudesStr[i])))
					.title(namesStr[i]).snippet(idsStr[i])
					.icon(BitmapDescriptorFactory.fromBitmap(friendMarker)));
		}

		// ----------------------
		// user marker with round mask
		// ----------------------
		ImageView markerPic = (ImageView) marker.findViewById(R.id.marker_pic);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float logicalDensity = metrics.density;
		int px = (int) (110 * logicalDensity + 0.5);
		// int dp = (int) (50 / logicalDensity + 0.5);

		newMarkerPic = Bitmap.createScaledBitmap(userBmp, px, px, true);

		tMask = BitmapFactory.decodeResource(resources, R.drawable.av_mask);
		mask = Bitmap.createScaledBitmap(tMask, px, px, true);
		result = Bitmap.createBitmap(px, px, Config.ARGB_8888);

		Canvas c = new Canvas(result);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		c.drawBitmap(newMarkerPic, 0, 0, null);
		c.drawBitmap(mask, 0, 0, paint);
		// reset Xfermode
		paint.setXfermode(null);

		markerPic.setImageBitmap(result);
		// ----------------------

		// User Marker
		map.addMarker(new MarkerOptions()
				.anchor((float) 0.5, (float) 0.5)
				.position(userLocation)
				.title(namesStr[nFriends])
				.snippet(idsStr[nFriends])
				.icon(BitmapDescriptorFactory
						.fromBitmap(createDrawableFromView(this, marker))));

		ImageView img = (ImageView) view.findViewById(R.id.photo);
		img.setImageBitmap(userBmp);

		TextView username = (TextView) view.findViewById(R.id.username);
		username.setText(namesStr[nFriends]);

		// Move the camera instantly with a zoom of 15.
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));

		// Zoom in, animating the camera.
		map.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
	}

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		// TODO Auto-generated method stub
		return super.onCreateView(name, context, attrs);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// Convert a view to bitmap
	public static Bitmap createDrawableFromView(Context context, View view) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay()
				.getMetrics(displayMetrics);
		view.setLayoutParams(new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
		view.layout(0, 0, displayMetrics.widthPixels,
				displayMetrics.heightPixels);
		view.buildDrawingCache();
		Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
				view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);

		return bitmap;
	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (state.isClosed()) {
			Intent intent = new Intent(this.getApplicationContext(),
					MainActivity.class);
			startActivity(intent);
		}
	}
}
