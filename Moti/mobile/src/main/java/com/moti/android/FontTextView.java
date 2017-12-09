package com.moti.android;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class FontTextView extends TextView{

    public static final String NORMAL = "0x0";
    public static final String BOLD = "0x1";

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        String style = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "textStyle");
        if (style != null)
            switch (style) {
                case BOLD:
                    setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/bariol_bold.ttf"));
                    break;
                case NORMAL:
                default:
                    setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/bariol_regular.ttf"));
            }
        else
            setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/bariol_regular.ttf"));

    }
}
