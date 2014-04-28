package com.a5.androidviewer;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.SparseArray;

import org.json.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Model {

	private ArrayList<ObjectData> objects;
	private ArrayList<ViewInterface> observers;
	private int frame, endFrame;

	public Model() {
		observers = new ArrayList<ViewInterface>();
		resetDisplay();
	}

	// reset all the visual graphics
	private void resetDisplay() {
		objects = new ArrayList<ObjectData>();
		frame = 0;
		endFrame = 0;
	}

	// add views to be updated upon change
	public void addObserver(ViewInterface observer) {
		observers.add(observer);
	}

	public int getMaximumFrame() {
		return endFrame;
	}

	// obtain all objects on screen at current frame
	public ArrayList<GraphicData> getObjects() {
		ArrayList<GraphicData> polygons = new ArrayList<GraphicData>();

		for (int a = 0; a < objects.size(); a++) {
			GraphicData obj = objects.get(a).getObject(frame);
			if (obj != null) {
				polygons.add(obj);
			}
		}
		return polygons;
	}

	// sets current frame
	public void setFrame(int frame) {
		this.frame = frame;
		endFrame = Math.max(endFrame, this.frame);
		notifyView();
	}

	// get current frame
	public int getFrame() {
		return frame;
	}

	// notify all registered views of update to model
	private void notifyView() {
		for (int a = 0; a < observers.size(); a++) {
			observers.get(a).updateView();
		}
	}

	// load from file and scale all objects and transformations based on
	// resolution
	public boolean load(File file, int screen_x, int screen_y) {
		JSONObject object = null;

		try {
			// read in JSON file
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuilder in = new StringBuilder();
			String temp = "";

			while ((temp = reader.readLine()) != null) {
				in.append(temp);
			}
			reader.close();
			object = new JSONObject(in.toString());

			resetDisplay();

			// calculate scale ratios for loading on this resolution
			int saved_screen_x = object.getInt("screen_x"), saved_screen_y = object
					.getInt("screen_y");
			double ratio_x = screen_x / (double) saved_screen_x, ratio_y = screen_y
					/ (double) saved_screen_y;

			endFrame = object.getInt("end_frame");
			JSONArray objectArray = object.getJSONArray("objects");

			// add all the objects
			for (int a = 0; a < objectArray.length(); a++) {
				JSONObject objectData = objectArray.getJSONObject(a);
				objects.add(new ObjectData(objectData, ratio_x, ratio_y));
			}
		} catch (Exception ex) {
			return false;
		}

		notifyView();
		return true;
	}
}

// private class to store information
class ObjectData {

	private Path polygon;
	private int color;
	private SparseArray<Matrix> transforms;
	private int startFrame, endFrame;

	public ObjectData(int frame, int color) {
		polygon = new Path();
		transforms = new SparseArray<Matrix>();
		this.startFrame = frame;
		this.endFrame = Integer.MAX_VALUE;
		this.color = color;
	}

	public ObjectData(JSONObject objectData, double ratio_x, double ratio_y)
			throws JSONException {
		// set up start, end frame and color
		this(objectData.getInt("start"), objectData.getInt("color"));
		this.endFrame = objectData.getInt("end");

		// add all the points
		JSONArray pointsArray = objectData.getJSONArray("points");
		for (int a = 0; a < pointsArray.length(); a++) {
			JSONArray point = pointsArray.getJSONArray(a);
			// scale down point based on resolution
			float x = (float) (point.getInt(0) * ratio_x), y = (float) (point
					.getInt(1) * ratio_y);

			// set starting location if first point
			if (a == 0)
				polygon.moveTo(x, y);
			else
				polygon.lineTo(x, y);
		}

		// add all the transforms
		JSONArray transformsArray = objectData.getJSONArray("transforms");
		for (int a = 0; a < transformsArray.length(); a++) {
			JSONObject transformObject = transformsArray.getJSONObject(a);

			int frame = transformObject.getInt("frame");
			JSONArray transform = transformObject.getJSONArray("transform");

			// formats the array such that AffineTransform from java maps to
			// matrix in android and scales to resolution
			float matrixArr[] = new float[9];

			// if it is a scale transformation, the translations need to be
			// recalculated
			if (transform.getDouble(0) != 1 || transform.getDouble(3) != 1) {
				RectF bounds = new RectF();
				polygon.computeBounds(bounds, true);

				matrixArr[0] = (float) transform.getDouble(0);
				matrixArr[1] = 0;
				matrixArr[2] = (1 - matrixArr[0]) * bounds.centerX();
				matrixArr[3] = 0;
				matrixArr[4] = (float) transform.getDouble(3);
				matrixArr[5] = (1 - matrixArr[4]) * bounds.centerY();
			} else {// if not a scale transformation, it is a translation
					// transformation which means all values can just be
					// multiplied by a ratio
				matrixArr[0] = 1;
				matrixArr[1] = (float) (transform.getDouble(2) * ratio_x);
				matrixArr[2] = (float) (transform.getDouble(4) * ratio_x);
				matrixArr[3] = (float) (transform.getDouble(1) * ratio_y);
				matrixArr[4] = 1;
				matrixArr[5] = (float) (transform.getDouble(5) * ratio_y);
			}
			matrixArr[6] = 0;
			matrixArr[7] = 0;
			matrixArr[8] = 1;

			// add the transformation at the appropriate frame in which it
			// occurs
			Matrix matrix = new Matrix();
			matrix.setValues(matrixArr);
			transforms.append(frame, matrix);
		}
	}

	public GraphicData getObject(int frame) {
		// check for if object is define at this frame
		if (this.startFrame > frame || this.endFrame < frame) {
			return null;
		}

		// combine all transformations to apply
		Matrix transform = new Matrix();
		for (int a = 0; a <= frame; a++) {
			if (transforms.get(a) != null) {
				transform.preConcat(transforms.get(a));
			}
		}

		// apply transformation
		Path transformedPolyon = new Path();
		polygon.transform(transform, transformedPolyon);

		return new GraphicData(transformedPolyon, color);
	}
}

class GraphicData {

	private Path polygon;
	private int color;

	public GraphicData(Path polygon, int color) {
		this.polygon = polygon;
		this.color = color;
	}

	public Path getPolygon() {
		return polygon;
	}

	public int getColor() {
		return color;
	}
}
