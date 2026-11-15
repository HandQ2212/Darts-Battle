package ui;
import javafx.scene.paint.Color;

/** Keep player identity recognizable for hits, misses and BUST. */
public final class ThrowPalette {
    private ThrowPalette(){}
    public static Color popup(boolean own,int score,boolean bust){
        return Color.web(own?(bust?"#164c7b":score==0?"#245f8a":"#2178c5"):(bust?"#8e3c14":score==0?"#7f5429":"#b85b08"));
    }
    public static Color accent(boolean own){return Color.web(own?"#68bbff":"#ffc078");}
}
