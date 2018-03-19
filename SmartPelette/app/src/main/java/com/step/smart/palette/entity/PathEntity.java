package com.step.smart.palette.entity;

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

    public PathEntity(LineType type) {
        this.type = type;
    }

    public int textOffX;
    public int textOffY;
    public int textWidth;//文字位置

}
