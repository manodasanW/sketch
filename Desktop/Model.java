
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Model {

    enum STATE {

        DRAW, ERASE, SELECT, PLAY
    };
    private ArrayList<ObjectData> objects;
    private ArrayList<ObjectData> selectedObjects;
    private ArrayList<ViewInterface> observers;
    private ObjectData selection;
    private STATE state;
    private int frame, endFrame;
    private boolean insert, scale;

    public Model() {
        observers = new ArrayList();
        resetDisplay();
    }

    private void resetDisplay() {
        objects = new ArrayList();
        selection = null;
        frame = 0;
        endFrame = 0;
        insert = false;
        scale = false;
        state = STATE.DRAW;
    }

    // add views to be updated upon change
    public void addObserver(ViewInterface observer) {
        observers.add(observer);
        notifyView();
    }

    public void setState(STATE state) {
        // reset instance objects if state is changed
        if (this.state != state) {
            selectedObjects = null;
            removeSelection();
        }

        this.state = state;
    }

    // sets whether frames are to be inserted or replaced when changes made in middle of playback
    public void setInsertMode(boolean insert) {
        this.insert = insert;
    }

    // retrieve whether being inserted or replaced
    public boolean getInsertMode() {
        return insert;
    }

    // sets whether selection drags perform a scale transformation
    public void setScaleTransform(boolean scale) {
        this.scale = scale;
    }

    // retrieves whether selection drags perform a scale transformation
    public boolean getScaleTransform() {
        return scale;
    }

    public int getMaximumFrame() {
        return endFrame;
    }

    // adds framesToAdd frames from the current location in the playback
    public void insertFrames(int framesToAdd) {
        for (int a = 0; a < objects.size(); a++) {
            for (int b = 0; b < framesToAdd; b++) {
                objects.get(a).shiftTransformation(frame);
            }
        }

        if (frame == endFrame || getInsertMode()) {
            endFrame += framesToAdd;
        }
    }

    // retrieves state
    public STATE getState() {
        return state;
    }

    // Builds up object being drawn, and adds it to list of object if it is a new one
    public void buildObject(Point point, Color color, boolean newObject) {
        if (newObject == true) {
            ObjectData obj = new ObjectData(frame, color);
            objects.add(obj);
        }

        objects.get(objects.size() - 1).addPoint(point);
        notifyView();
    }

    // builds up the lassoe drawn using that tool
    public void buildSelection(Point point, boolean newObject) {
        if (newObject == true) {
            ObjectData obj = new ObjectData(frame, Color.red);
            selection = obj;
        }

        selection.addPoint(point);
        notifyView();
    }

    // selects any object selected using the lassoe tool
    public boolean selectObject() {
        selectedObjects = new ArrayList();
        Polygon selectionPolygon = selection.getObject(frame).getPolygon();

        for (int a = 0; a < objects.size(); a++) {
            GraphicData obj = objects.get(a).getObject(frame);
            // check to see if lassoe selection contains the obj fully
            if (obj != null && contains(selectionPolygon, obj.getPolygon())) {
                selectedObjects.add(objects.get(a));
            }
        }

        // if no objects selected, we don't need to display selection
        if (selectedObjects.isEmpty()) {
            removeSelection();
        }

        return !selectedObjects.isEmpty();
    }

    // erases any object at given point
    public void erase(Point p) {
        boolean erased = false;
        for (int a = 0; a < objects.size(); a++) {
            erased = erased | objects.get(a).erase(frame, p);
        }

        // checks if anything got erased to determine if view should be notified
        if (erased) {
            notifyView();
        }
    }

    // check if p1 fully contains p2
    private boolean contains(Polygon p1, Polygon p2) {
        int x[] = p2.xpoints;
        int y[] = p2.ypoints;

        for (int a = 0; a < p2.npoints; a++) {
            if (!p1.contains(new Point(x[a], y[a]))) {
                return false;
            }
        }
        return true;
    }

    public void addTransformation(Point p1, Point p2) {
        // check whether to replace current frame or append
        if (insert) {
            insertFrames(1);
        }

        AffineTransform transform = new AffineTransform();
        double delta_x = p2.x - p1.x, delta_y = p2.y - p1.y;
        if (scale) {
            delta_x = Math.abs(delta_x * 0.1);
            delta_y = Math.abs(delta_y * 0.1);
        } else {
            transform.setToTranslation(delta_x, delta_y);
        }

        for (int a = 0; a < selectedObjects.size(); a++) {
            if (scale) {
                double scale_x = delta_x, scale_y = delta_y;
                Rectangle rect = selectedObjects.get(a).getObject(frame).getPolygon().getBounds();
                if (p2.distance(rect.getCenterX(), rect.getCenterY()) < p1.distance(rect.getCenterX(), rect.getCenterY())) {
                    scale_x *= -1;
                    scale_y *= -1;
                }
                scale_x += 1;
                scale_y += 1;
                transform.setToScale(scale_x, scale_y);
                AffineTransform translationScale = new AffineTransform(transform), translation = new AffineTransform();
                translation.setToTranslation((1 - scale_x) * rect.getCenterX(), (1 - scale_y) * rect.getCenterY());
                translationScale.preConcatenate(translation);
                selectedObjects.get(a).addTransformation(frame, translationScale, insert);
            } else {
                selectedObjects.get(a).addTransformation(frame, transform, insert);
            }
        }
        notifyView();
    }

    public ArrayList<GraphicData> getObjects() {
        ArrayList<GraphicData> polygons = new ArrayList();

        for (int a = 0; a < objects.size(); a++) {
            GraphicData obj = objects.get(a).getObject(frame);
            if (obj != null) {
                polygons.add(obj);
            }
        }
        return polygons;
    }

    // get the data for the lassoe selection
    public GraphicData getSelection() {
        if (selection == null) {
            return null;
        }
        return selection.getObject(frame);
    }

    // remove the selection lassoe tool
    public void removeSelection() {
        selection = null;
        notifyView();
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

    public boolean save(File file, int screenWidth, int screenHeight) {
        JSONObject outObject = new JSONObject();

        outObject.put("screen_x", screenWidth);
        outObject.put("screen_y", screenHeight);

        outObject.put("end_frame", endFrame);

        JSONArray objectArray = new JSONArray();
        for (int a = 0; a < objects.size(); a++) {
            objectArray.add(objects.get(a).getJSON());
        }
        outObject.put("objects", objectArray);

        try {
            PrintWriter out = new PrintWriter(new FileWriter(file));
            out.println(outObject.toJSONString());
            out.close();
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    public boolean load(File file) {
        JSONObject object = null;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            object = (JSONObject) JSONValue.parseWithException(reader);
        } catch (Exception ex) {
            return false;
        }

        if (object == null) {
            return false;
        }

        resetDisplay();
        Dimension screenSize = new Dimension(((Number) object.get("screen_x")).intValue(), ((Number) object.get("screen_y")).intValue());
        endFrame = ((Number) object.get("end_frame")).intValue();
        JSONArray objectArray = (JSONArray) object.get("objects");

        for (Iterator i = objectArray.iterator(); i.hasNext();) {
            JSONObject objectData = (JSONObject) i.next();
            objects.add(new ObjectData(objectData));
        }

        notifyView();
        return true;
    }
}

// private class to store information
class ObjectData {

    private ArrayList<Point> points;
    private Color color;
    private Map<Integer, AffineTransform> transforms;
    private int startFrame, endFrame;

    public ObjectData(int frame, Color color) {
        points = new ArrayList();
        transforms = new HashMap();
        this.startFrame = frame;
        this.endFrame = Integer.MAX_VALUE;
        this.color = color;
    }

    private static int parseInt(Number number) {
        return number.intValue();
    }

    public ObjectData(JSONObject objectData) {
        this(parseInt((Number) objectData.get("start")), new Color(parseInt((Number) objectData.get("color"))));
        this.endFrame = parseInt((Number) objectData.get("end"));

        JSONArray pointsArray = (JSONArray) objectData.get("points");
        for (Iterator it = pointsArray.iterator(); it.hasNext();) {
            JSONArray pointArray = (JSONArray) it.next();
            points.add(new Point(parseInt((Number) pointArray.get(0)), parseInt((Number) pointArray.get(1))));
        }

        JSONArray transformsArray = (JSONArray) objectData.get("transforms");
        for (Iterator it = transformsArray.iterator(); it.hasNext();) {
            JSONObject transformObject = (JSONObject) it.next();

            int frame = parseInt((Number) transformObject.get("frame"));
            JSONArray transform = (JSONArray) transformObject.get("transform");

            double matrix[] = new double[6];
            for (int a = 0; a < transform.size(); a++) {
                matrix[a] = ((Number) transform.get(a)).doubleValue();
            }

            transforms.put(frame, new AffineTransform(matrix));
        }
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public void addTransformation(int frame, AffineTransform transform, boolean insert) {
        transforms.put(frame, transform);
        Set<Integer> keys = transforms.keySet();

        if (!insert) {
            for (Iterator<Integer> it = keys.iterator(); it.hasNext();) {
                if (it.next() > frame) {
                    it.remove();
                }
            }
        }
    }

    public void shiftTransformation(int frame) {
        Map<Integer, AffineTransform> newTransforms = new HashMap();
        Set<Integer> keys = transforms.keySet();

        for (Iterator<Integer> it = keys.iterator(); it.hasNext();) {
            int i = it.next();
            if (i < frame) {
                newTransforms.put(i, transforms.get(i));
            } else {
                newTransforms.put(i + 1, transforms.get(i));
            }
        }

        if (frame < startFrame) {
            startFrame += 1;
        }
        if (frame < endFrame && endFrame != Integer.MAX_VALUE) {
            endFrame += 1;
        }
        transforms = newTransforms;
    }

    public boolean erase(int frame, Point point) {
        GraphicData object = getObject(frame);
        if (object == null) {
            return false;
        }

        Polygon poly = object.getPolygon();
        int x[] = poly.xpoints, y[] = poly.ypoints;

        for (int a = 1; a < poly.npoints; a++) {
            if (Line2D.ptSegDist(x[a - 1], y[a - 1], x[a], y[a], point.x, point.y) < 5) {
                endFrame = frame - 1;
                return true;
            }
        }

        return false;
    }

    public GraphicData getObject(int frame) {
        if (this.startFrame > frame || this.endFrame < frame) {
            return null;
        }

        AffineTransform transform = new AffineTransform();
        transform.setToIdentity();
        for (int a = 0; a <= frame; a++) {
            if (transforms.containsKey(a)) {
                transform.preConcatenate(transforms.get(a));
            }
        }

        Polygon poly = new Polygon();
        for (int a = 0; a < points.size(); a++) {
            Point point = new Point(points.get(a));
            transform.transform(point, point);
            poly.addPoint(point.x, point.y);
        }

        return new GraphicData(poly, color);
    }

    public JSONObject getJSON() {
        JSONObject object = new JSONObject();
        object.put("start", startFrame);
        object.put("end", endFrame);
        object.put("color", color.getRGB());

        JSONArray pointsArray = new JSONArray();
        for (int a = 0; a < points.size(); a++) {
            JSONArray pointArray = new JSONArray();
            pointArray.add(points.get(a).x);
            pointArray.add(points.get(a).y);
            pointsArray.add(pointArray);
        }
        object.put("points", pointsArray);

        JSONArray transformsArray = new JSONArray();
        for (int frameIndex : transforms.keySet()) {
            JSONObject transformObject = new JSONObject();
            transformObject.put("frame", frameIndex);

            JSONArray transformArray = new JSONArray();
            double matrix[] = new double[6];
            transforms.get(frameIndex).getMatrix(matrix);

            for (int b = 0; b < matrix.length; b++) {
                transformArray.add(matrix[b]);
            }
            transformObject.put("transform", transformArray);
            transformsArray.add(transformObject);
        }
        object.put("transforms", transformsArray);
        return object;
    }
}

class GraphicData {

    private Polygon polygon;
    private Color color;

    public GraphicData(Polygon polygon, Color color) {
        this.polygon = polygon;
        this.color = color;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public Color getColor() {
        return color;
    }
}
