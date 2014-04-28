package com.a5.androidviewer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FilechooserActivity extends Activity {

	protected static int REQUEST_CODE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filechooser);
	}

	@Override
	protected void onPostCreate(Bundle savedInstance) {
		super.onPostCreate(savedInstance);

		ListView fileList = ((ListView) findViewById(R.id.fileList));
		File file = new File(Environment.getExternalStorageDirectory()
				.getPath());

		String files[] = file.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String fileName) {
				return fileName.toLowerCase(Locale.US).endsWith(".json");
			}
		});

		if (files.length == 0) {
			files = new String[1];
			files[0] = "No JSON Files";
			fileList.setEnabled(false);
		}

		fileList.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, files));

		fileList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String fileName = (String) parent.getItemAtPosition(position);

				if (!fileName.equals("No JSON Files")) {
					fileName = Environment.getExternalStorageDirectory()
							.getPath() + File.separator + fileName;

					Intent output = new Intent();
					output.putExtra(MainActivity.FILE_REQUEST, fileName);
					FilechooserActivity.this.setResult(RESULT_OK, output);
					FilechooserActivity.this.finish();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}
