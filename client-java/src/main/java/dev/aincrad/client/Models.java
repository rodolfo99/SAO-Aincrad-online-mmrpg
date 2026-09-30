package dev.aincrad.client;

import javafx.scene.paint.Color;

final class Models {
    private Models() {}
    static Color color(String value) {try{return Color.web(value);}catch(Exception invalid){return Color.web("#6f969e");}}
}
