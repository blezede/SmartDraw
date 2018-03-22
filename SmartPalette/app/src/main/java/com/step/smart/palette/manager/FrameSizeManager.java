package com.step.smart.palette.manager;

/**
 * Created by weflow on 2018/3/21.
 */

public class FrameSizeManager {

    public int frameWidth; //原始宽
    public int frameHeight;//原始高

    public int wholeWidth;//总宽 = 原始宽 + 扩展宽
    public int wholeHeight;//总高 = 原始高 + 扩展高

    //位置
    public int posX;
    public int posY;

    public int windowLeft;
    public int windowTop;

    public void calculate() {
        this.windowLeft = (this.frameWidth - this.wholeWidth) / 2;
        this.windowTop = (this.frameHeight - this.wholeHeight) / 2;
    }
}
