package com.a5.androidviewer;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class SettingActivity extends Activity {

	protected static int REQUEST_CODE = 1;

	private int color;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// obtain saved values
		SharedPreferences setting = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		int fps = setting.getInt("fps", MainActivity.DEFAULT_FPS);
		color = setting.getInt("color", MainActivity.DEFAULT_COLOR);

		// set max rgb value on seekbar
		((SeekBar) this.findViewById(R.id.redSeek)).setMax(255);
		((SeekBar) this.findViewById(R.id.greenSeek)).setMax(255);
		((SeekBar) this.findViewById(R.id.blueSeek)).setMax(255);
		updateRGBSeek();

		// set fps value
		((EditText) this.findViewById(R.id.fps)).setText(fps + "");

		// set change listeners to update color on drag
		((SeekBar) this.findViewById(R.id.blueSeek))
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar bar, int progress,
							boolean fromUser) {
						int red = Color.red(color), green = Color.green(color);
						if (fromUser) {
							color = Color.rgb(red, green, progress);
							findViewById(R.id.previewArea).setBackgroundColor(
									color);
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}
				});

		((SeekBar) this.findViewById(R.id.greenSeek))
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar bar, int progress,
							boolean fromUser) {
						int red = Color.red(color), blue = Color.blue(color);
						if (fromUser) {
							color = Color.rgb(red, progress, blue);
							findViewById(R.id.previewArea).setBackgroundColor(
									color);
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}
				});

		((SeekBar) this.findViewById(R.id.redSeek))
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar bar, int progress,
							boolean fromUser) {
						int blue = Color.blue(color), green = Color
								.green(color);
						if (fromUser) {
							color = Color.rgb(progress, green, blue);
							findViewById(R.id.previewArea).setBackgroundColor(
									color);
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}
				});

		findViewById(R.id.previewArea).setBackgroundColor(color);
	}

	// save settings
	public void applyClicked(View button) {
		// obtain value
		int fps = Integer.parseInt(((EditText) this.findViewById(R.id.fps))
				.getText().toString());

		if (fps <= 0 || fps > 100) {
			Toast.makeText(getApplicationContext(),
					"FPS needs to be between 1 and 100!", Toast.LENGTH_LONG)
					.show();
			return;
		}

		// obtain editor to save values
		SharedPreferences setting = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = setting.edit();

		// set values
		editor.putInt("fps", fps);
		editor.putInt("color", color);

		// save
		editor.commit();

		Toast.makeText(getApplicationContext(), "Settings saved",
				Toast.LENGTH_SHORT).show();
	}

	// set rgb seek bar based on color
	private void updateRGBSeek() {
		((SeekBar) this.findViewById(R.id.redSeek)).setProgress(Color
				.red(color));
		((SeekBar) this.findViewById(R.id.greenSeek)).setProgress(Color
				.green(color));
		((SeekBar) this.findViewById(R.id.blueSeek)).setProgress(Color
				.blue(color));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	// Process color button clicks
	public void colorChanged(View button) {
		// detect which button was clicked
		switch (button.getId()) {
		case R.id.black:
			color = Color.BLACK;
			break;
		case R.id.blue:
			color = Color.BLUE;
			break;
		case R.id.white:
			color = Color.WHITE;
			break;
		case R.id.brown:
			color = Color.rgb(185, 122, 87);
			break;
		case R.id.green:
			color = Color.GREEN;
			break;
		case R.id.orange:
			color = Color.rgb(255, 128, 0);
			break;
		case R.id.red:
			color = Color.RED;
			break;
		case R.id.yellow:
			color = Color.YELLOW;
			break;
		}

		// set preview and update seek bars
		findViewById(R.id.previewArea).setBackgroundColor(color);
		updateRGBSeek();
	}
}
