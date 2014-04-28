package com.a5.androidviewer;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class SketchView extends View {

	Paint color;

	public SketchView(Context context, AttributeSet attrs) {
		super(context, attrs);

		color = new Paint();
		color.setStyle(Paint.Style.STROKE);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		MainActivity activity = (MainActivity) this.getContext();
		Model model = activity.getModel();

		canvas.drawColor(activity.getColor());

		ArrayList<GraphicData> drawable = model.getObjects();

		for (Iterator<GraphicData> it = drawable.iterator(); it.hasNext();) {
			GraphicData curr = it.next();
			color.setColor(curr.getColor());

			canvas.drawPath(curr.getPolygon(), color);
		}

		((SeekBar) this.getRootView().findViewById(R.id.frameBar)).setMax(model
				.getMaximumFrame());
		((SeekBar) this.getRootView().findViewById(R.id.frameBar))
				.setProgress(model.getFrame());
	}
}
