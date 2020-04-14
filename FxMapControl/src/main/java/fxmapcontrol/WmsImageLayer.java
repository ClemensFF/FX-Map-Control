/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Map image overlay. Fills the viewport with a single map image from a Web Map Service (WMS).
 *
 * The base request URL is specified by the serviceUrl property.
 */
public class WmsImageLayer extends MapImageLayer {

    private final StringProperty serviceUrlProperty = new SimpleStringProperty(this, "serviceUrl");
    private final StringProperty layersProperty = new SimpleStringProperty(this, "layers", "");
    private final StringProperty stylesProperty = new SimpleStringProperty(this, "styles", "");
    private final StringProperty formatProperty = new SimpleStringProperty(this, "format", "image/png");

    public WmsImageLayer(String serviceUrl) {
        this();
        setServiceUrl(serviceUrl);
    }

    public WmsImageLayer() {
        ChangeListener changeListener = (observable, oldValue, newValue) -> updateImage();
        serviceUrlProperty.addListener(changeListener);
        layersProperty.addListener(changeListener);
        stylesProperty.addListener(changeListener);
        formatProperty.addListener(changeListener);
    }

    public final StringProperty serviceUrlProperty() {
        return serviceUrlProperty;
    }

    public final String getServiceUrl() {
        return serviceUrlProperty.get();
    }

    public final void setServiceUrl(String serviceUrl) {
        serviceUrlProperty.set(serviceUrl);
    }

    public final StringProperty layersProperty() {
        return layersProperty;
    }

    public final String getLayers() {
        return layersProperty.get();
    }

    public final void setLayers(String layers) {
        layersProperty.set(layers);
    }

    public final StringProperty stylesProperty() {
        return stylesProperty;
    }

    public final String getStyles() {
        return stylesProperty.get();
    }

    public final void setStyles(String styles) {
        stylesProperty.set(styles);
    }

    public final StringProperty formatProperty() {
        return formatProperty;
    }

    public final String getFormat() {
        return formatProperty.get();
    }

    public final void setFormat(String format) {
        formatProperty.set(format);
    }

    public List<String> getAllLayers() {
        List<String> layerNames = null;

        if (getServiceUrl() != null && !getServiceUrl().isEmpty()) {
            String url = getRequestUrl("GetCapabilities").replace(" ", "%20");

            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
                NodeList layers = document.getDocumentElement().getElementsByTagName("Layer");

                layerNames = new ArrayList<>();

                if (layers.getLength() > 0) {
                    layers = ((Element) layers.item(0)).getElementsByTagName("Layer");

                    for (int i = 0; i < layers.getLength(); i++) {
                        NodeList names = ((Element) layers.item(i)).getElementsByTagName("Name");

                        if (names.getLength() > 0) {
                            layerNames.add(names.item(0).getTextContent());
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(WmsImageLayer.class.getName()).log(Level.WARNING,
                        String.format("%s: %s", url, ex.toString()));
            }
        }

        return layerNames;
    }

    @Override
    protected Image loadImage() {
        Image image = null;
        String url = getImageUrl();

        if (url != null && !url.isEmpty()) {
            image = new Image(url, true);
        }

        return image;
    }

    protected String getImageUrl() {
        if (getServiceUrl() == null || getServiceUrl().isEmpty()) {
            return null;
        }

        MapProjection projection = getMap().getProjection();
        Bounds bounds = projection.boundingBoxToBounds(getBoundingBox());
        double viewScale = getMap().getViewTransform().getScale();
        String url = getRequestUrl("GetMap");

        if (!url.toUpperCase().contains("LAYERS=") && getLayers() != null) {
            url += "&LAYERS=" + getLayers();
        }

        if (!url.toUpperCase().contains("STYLES=") && getStyles() != null) {
            url += "&STYLES=" + getStyles();
        }

        if (!url.toUpperCase().contains("FORMAT=") && getFormat() != null) {
            url += "&FORMAT=" + getFormat();
        }

        url += "&CRS=" + projection.getCrsValue();
        url += "&BBOX=" + projection.getBboxValue(bounds);
        url += "&WIDTH=" + (int) Math.round(viewScale * bounds.getWidth());
        url += "&HEIGHT=" + (int) Math.round(viewScale * bounds.getHeight());

        return url.replace(" ", "%20");
    }

    private String getRequestUrl(String request) {
        String url = getServiceUrl();

        if (!url.endsWith("?") && !url.endsWith("&")) {
            url += !url.contains("?") ? "?" : "&";
        }

        if (!url.toUpperCase().contains("SERVICE=")) {
            url += "SERVICE=WMS&";
        }

        if (!url.toUpperCase().contains("VERSION=")) {
            url += "VERSION=1.3.0&";
        }

        return url + "REQUEST=" + request;
    }
}
