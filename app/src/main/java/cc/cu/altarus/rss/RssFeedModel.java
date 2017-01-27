package cc.cu.altarus.rss;

import android.graphics.drawable.Drawable;

import java.util.HashMap;

/**
 * Created by travis on 12/26/16.]
 * Taken fromhttp://www.androidauthority.com/simple-rss-reader-full-tutorial-733245/
 */

public class RssFeedModel {
    public String title;
    public String link;
    public String description;
    public String imgSrc;

    public RssFeedModel(String title, String link, String description, String imgSrc) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.imgSrc = imgSrc;
    }
}
