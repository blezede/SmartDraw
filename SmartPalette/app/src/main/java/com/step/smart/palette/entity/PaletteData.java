package com.step.smart.palette.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by max on 2018/3/19.
 */

public class PaletteData {

    public List<PathEntity> pathList = new ArrayList<>();

    public List<PathEntity> redoList = new ArrayList<>();

    public List<PathEntity> undoList = new ArrayList<>();
}