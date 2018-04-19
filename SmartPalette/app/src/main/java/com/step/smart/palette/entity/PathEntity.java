package com.step.smart.palette.entity;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;

import com.step.smart.palette.Constant.LineType;

/**
 * Created by weflow on 2018/3/19.
 */

public class PathEntity {

    public LineType type;//记录类型
    public Paint paint;//笔类
    public Path path;//画笔路径数据
    public PointF[] linePoints; //线数据
    public RectF rect; //圆、矩形区域
    public String text;//文字
    public TextPaint textPaint;//笔类

    public RectF pathRect;//路径的rect区域

    public PathEntity(LineType type) {
        this.type = type;
    }

    public Bitmap bitmap;//图形
    public Matrix matrix;//图形
    public Matrix srcMatrix;
    public RectF photoRectSrc = new RectF();
    public float scaleMax = 3;

}
