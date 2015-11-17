/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Draws a graticule overlay.
 */
public class MapGraticule extends Parent implements IMapNode {

    private static final StyleablePropertyFactory<MapGraticule> propertyFactory
            = new StyleablePropertyFactory<>(Parent.getClassCssMetaData());

    private static final CssMetaData<MapGraticule, Font> fontCssMetaData
            = propertyFactory.createFontCssMetaData("-fx-font", s -> s.fontProperty);

    private static final CssMetaData<MapGraticule, Paint> textFillCssMetaData
            = propertyFactory.createPaintCssMetaData("-fx-text-fill", s -> s.textFillProperty);

    private static final CssMetaData<MapGraticule, Paint> strokeCssMetaData
            = propertyFactory.createPaintCssMetaData("-fx-stroke", s -> s.strokeProperty);

    private static final CssMetaData<MapGraticule, Number> strokeWidthCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-stroke-width", s -> s.strokeWidthProperty);

    private static final CssMetaData<MapGraticule, Number> minLineDistanceCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-min-line-distance", s -> s.minLineDistanceProperty);

    private final StyleableObjectProperty<Font> fontProperty
            = new SimpleStyleableObjectProperty<>(fontCssMetaData, this, "font", Font.getDefault());

    private final StyleableObjectProperty<Paint> textFillProperty
            = new SimpleStyleableObjectProperty<>(textFillCssMetaData, this, "textFill", Color.BLACK);

    private final StyleableObjectProperty<Paint> strokeProperty
            = new SimpleStyleableObjectProperty<>(strokeCssMetaData, this, "stroke", Color.BLACK);

    private final StyleableDoubleProperty strokeWidthProperty
            = new SimpleStyleableDoubleProperty(strokeWidthCssMetaData, this, "strokeWidth", 0.5);

    private final StyleableDoubleProperty minLineDistanceProperty
            = new SimpleStyleableDoubleProperty(minLineDistanceCssMetaData, this, "minLineDistance", 150d);

    private final MapNodeHelper mapNode = new MapNodeHelper(e -> viewportTransformChanged());

    public MapGraticule() {
        getStyleClass().add("map-graticule");
        setMouseTransparent(true);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public final MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public final void setMap(MapBase map) {
        mapNode.setMap(map);
        if (map != null) {
            viewportTransformChanged();
        }
    }

    public final ObjectProperty<Font> fontProperty() {
        return fontProperty;
    }

    public final Font getFont() {
        return fontProperty.get();
    }

    public final void setFont(Font font) {
        fontProperty.set(font);
    }

    public final ObjectProperty<Paint> textFillProperty() {
        return textFillProperty;
    }

    public final Paint getTextFill() {
        return textFillProperty.get();
    }

    public final void setTextFill(Paint textFill) {
        textFillProperty.set(textFill);
    }

    public final ObjectProperty<Paint> strokeProperty() {
        return strokeProperty;
    }

    public final Paint getStroke() {
        return strokeProperty.get();
    }

    public final void setStroke(Paint stroke) {
        strokeProperty.set(stroke);
    }

    public final DoubleProperty strokeWidthProperty() {
        return strokeWidthProperty;
    }

    public final double getStrokeWidth() {
        return strokeWidthProperty.get();
    }

    public final void setStrokeWidth(double strokeWidth) {
        strokeWidthProperty.set(strokeWidth);
    }

    public final DoubleProperty minLineDistanceProperty() {
        return minLineDistanceProperty;
    }

    public final double getMinLineDistance() {
        return minLineDistanceProperty.get();
    }

    public final void setMinLineDistance(double minLineDistance) {
        minLineDistanceProperty.set(minLineDistance);
    }

    private void viewportTransformChanged() {
        final MapBase map = getMap();
        final Affine transform;

        try {
            transform = map.getViewportTransform().createInverse();
        } catch (NonInvertibleTransformException ex) {
            Logger.getLogger(MapGraticule.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        final Point2D p1 = transform.transform(new Point2D(0d, 0d));
        final Point2D p2 = transform.transform(new Point2D(map.getWidth(), 0d));
        final Point2D p3 = transform.transform(new Point2D(0d, map.getHeight()));
        final Point2D p4 = transform.transform(new Point2D(map.getWidth(), map.getHeight()));

        final Point2D min = new Point2D(
                Math.min(p1.getX(), Math.min(p2.getX(), Math.min(p3.getX(), p4.getX()))),
                Math.min(p1.getY(), Math.min(p2.getY(), Math.min(p3.getY(), p4.getY()))));

        final Point2D max = new Point2D(
                Math.max(p1.getX(), Math.max(p2.getX(), Math.max(p3.getX(), p4.getX()))),
                Math.max(p1.getY(), Math.max(p2.getY(), Math.max(p3.getY(), p4.getY()))));

        final Location start = map.getMapTransform().transform(min);
        final Location end = map.getMapTransform().transform(max);
        final double lineDistance = getLineDistance();
        final double latLabelStart = Math.ceil(start.getLatitude() / lineDistance) * lineDistance;
        final double lonLabelStart = Math.ceil(start.getLongitude() / lineDistance) * lineDistance;
        final ArrayList<PathElement> pathElements = new ArrayList<>();

        for (double lat = latLabelStart; lat <= end.getLatitude(); lat += lineDistance) {
            final Point2D lineStart = map.locationToViewportPoint(new Location(lat, start.getLongitude()));
            final Point2D lineEnd = map.locationToViewportPoint(new Location(lat, end.getLongitude()));
            pathElements.add(new MoveTo(lineStart.getX(), lineStart.getY()));
            pathElements.add(new LineTo(lineEnd.getX(), lineEnd.getY()));
        }

        for (double lon = lonLabelStart; lon <= end.getLongitude(); lon += lineDistance) {
            final Point2D lineStart = map.locationToViewportPoint(new Location(start.getLatitude(), lon));
            final Point2D lineEnd = map.locationToViewportPoint(new Location(end.getLatitude(), lon));
            pathElements.add(new MoveTo(lineStart.getX(), lineStart.getY()));
            pathElements.add(new LineTo(lineEnd.getX(), lineEnd.getY()));
        }

        final ObservableList<Node> children = getChildren();
        final Path path;

        if (children.isEmpty()) {
            path = new Path();
            path.strokeProperty().bind(strokeProperty);
            path.strokeWidthProperty().bind(strokeWidthProperty);
            children.add(path);
        } else {
            path = (Path) children.get(0);
        }

        path.getElements().setAll(pathElements);

        final Font font = getFont();
        int childIndex = 1;

        if (font != null) {
            final String format = getLabelFormat(lineDistance);
            final Rotate rotate = new Rotate(map.getHeading());

            for (double lat = latLabelStart; lat <= end.getLatitude(); lat += lineDistance) {
                for (double lon = lonLabelStart; lon <= end.getLongitude(); lon += lineDistance) {
                    final Point2D pos = map.locationToViewportPoint(new Location(lat, lon));
                    final Translate translate = new Translate(pos.getX(), pos.getY());
                    final Text text;

                    if (childIndex < children.size()) {
                        text = (Text) children.get(childIndex);
                        text.getTransforms().set(0, translate);
                        text.getTransforms().set(1, rotate);
                    } else {
                        text = new Text();
                        text.fillProperty().bind(textFillProperty);
                        text.setX(3);
                        text.setY(-font.getSize() / 4);
                        text.getTransforms().add(translate);
                        text.getTransforms().add(rotate);
                        children.add(text);
                    }

                    text.setFont(getFont());
                    text.setText(getLabelText(lat, format, "NS") + "\n"
                            + getLabelText(Location.normalizeLongitude(lon), format, "EW"));
                    childIndex++;
                }
            }
        }

        children.remove(childIndex, children.size());
    }

    private double getLineDistance() {
        double minDistance = getMinLineDistance() * 360d / (Math.pow(2d, getMap().getZoomLevel()) * (double) TileSource.TILE_SIZE);
        double scale = 1d;

        if (minDistance < 1d) {
            scale = minDistance < 1d / 60d ? 3600d : 60d;
            minDistance *= scale;
        }

        final double[] lineDistances = new double[]{1d, 2d, 5d, 10d, 15d, 30d, 60d};
        int i = 0;

        while (i < lineDistances.length - 1 && lineDistances[i] < minDistance) {
            i++;
        }

        return lineDistances[i] / scale;
    }

    private static String getLabelFormat(double lineDistance) {
        if (lineDistance < 1d / 60d) {
            return "%c %d°%02d'%02d\"";
        }
        if (lineDistance < 1d) {
            return "%c %d°%02d'";
        }
        return "%c %d°";
    }

    private static String getLabelText(double value, String format, String hemispheres) {
        char hemisphere = hemispheres.charAt(0);

        if (value < -1e-8) // ~1mm
        {
            value = -value;
            hemisphere = hemispheres.charAt(1);
        }

        int seconds = (int) Math.round(value * 3600d);

        return String.format(format, hemisphere, seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
}
