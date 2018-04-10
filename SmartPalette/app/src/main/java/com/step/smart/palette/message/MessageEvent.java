package com.step.smart.palette.message;

/**
 * Created by weflow on 2017/12/27.
 */

public class MessageEvent {

    public static final int REQUEST_PREVIEW_BITMAP = 1;
    public static final int PREVIEW_BITMAP_RESULT = 2;

    private static MessageEvent messageEvent;
    public int what = -1;
    public int arg = 0;
    public Object obj;

    public static MessageEvent obtain() {
        if (messageEvent == null) {
            return new MessageEvent();
        } else {
            messageEvent.what = -1;
            messageEvent.arg = 0;
            messageEvent.obj = null;
            return messageEvent;
        }
    }
}
