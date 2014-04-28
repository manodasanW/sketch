package com.a5.androidviewer;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

public class MainActivity extends Activity implements ViewInterface {

	protected static int DEFAULT_FPS = 40;
	protected static int DEFAULT_COLOR = Color.WHITE;
	protected static String FILE_REQUEST = "file_name";

	private Model model;
	private boolean playing;
	private int fps;
	private int color;

	private Timer frameUpdaterTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		model = new Model();
		model.addObserver(this);

		playing = false;
		updateFromSettings();

		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		((SeekBar) this.findViewById(R.id.frameBar)).setMax(model
				.getMaximumFrame());

		((SeekBar) this.findViewById(R.id.frameBar))
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar seekbar,
							int progress, boolean fromUser) {
						if (fromUser) {
							model.setFrame(progress);
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem settingItem = menu.findItem(R.id.action_settings);
		settingItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				return settingsClicked(menuItem);
			}
		});

		MenuItem openFileItem = menu.findItem(R.id.action_load);
		openFileItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				return loadFileClicked(menuItem);
			}
		});

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SettingActivity.REQUEST_CODE) {
			updateFromSettings();
			updateView();
		} else if (requestCode == FilechooserActivity.REQUEST_CODE
				&& data != null) {
			String file = data.getStringExtra(FILE_REQUEST);
			View sketchView = findViewById(R.id.sketchView);

			if (file != null) {
				model.load(new File(file), sketchView.getWidth(),
						sketchView.getHeight());
			}
		}
	}

	protected int getColor() {
		return color;
	}

	protected Model getModel() {
		return model;
	}

	private void updateFromSettings() {
		SharedPreferences setting = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		fps = setting.getInt("fps", DEFAULT_FPS);
		color = setting.getInt("color", DEFAULT_COLOR);
	}

	public void playClicked(View button) {
		if (playing) {
			((ImageButton) button).setImageResource(R.drawable.av_play);
			playing = false;
			if (frameUpdaterTimer != null) {
				frameUpdaterTimer.cancel();
				frameUpdaterTimer.purge();
				frameUpdaterTimer = null;
			}
		} else {
			((ImageButton) button).setImageResource(R.drawable.av_pause);
			playing = true;
			frameUpdaterTimer = new Timer();
			frameUpdaterTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					model.setFrame(model.getFrame() + 1);

					if (model.getFrame() == model.getMaximumFrame()) {
						frameUpdaterTimer.cancel();
						frameUpdaterTimer.purge();
						frameUpdaterTimer = null;

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								playClicked(MainActivity.this
										.findViewById(R.id.play));
							}
						});
					}
				}
			}, 1, 1000 / fps);
		}
	}

	public void stopClicked(View button) {
		stopActions();

		model.setFrame(0);
	}

	public boolean loadFileClicked(MenuItem actionItem) {
		stopActions();

		Intent intent = new Intent(this, FilechooserActivity.class);
		this.startActivityForResult(intent, FilechooserActivity.REQUEST_CODE);

		return true;
	}

	public boolean settingsClicked(MenuItem menuItem) {
		stopActions();

		Intent intent = new Intent(this, SettingActivity.class);
		this.startActivityForResult(intent, SettingActivity.REQUEST_CODE);

		return true;
	}

	private void stopActions() {
		if (playing) {
			playClicked(this.findViewById(R.id.play));
		}
	}

	@Override
	public void updateView() {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				MainActivity.this.findViewById(R.id.sketchView).invalidate();
			}
		});
	}
}