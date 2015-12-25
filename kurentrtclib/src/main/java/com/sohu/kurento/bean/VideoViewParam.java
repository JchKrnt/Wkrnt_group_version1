package com.sohu.kurento.bean;

import org.webrtc.RendererCommon;

/**
 * Created by jingbiaowang on 2015/12/7.
 */
public class VideoViewParam {

    private int layoutPositionX;
    private int layoutPositionY;
    private int layoutWidth;
    private int layoutHeight;

    private boolean zOrderMediaOverlay;

    private RendererCommon.ScalingType scalingType;
    private boolean mirror;

    public VideoViewParam(int layoutPositionX, int layoutPositionY, int layoutWidth, int layoutHeight, boolean mirror, RendererCommon.ScalingType scalingType) {
        this.layoutHeight = layoutHeight;
        this.layoutPositionX = layoutPositionX;
        this.layoutPositionY = layoutPositionY;
        this.layoutWidth = layoutWidth;
        this.mirror = mirror;
        this.scalingType = scalingType;
    }

    public RendererCommon.ScalingType getScalingType() {
        return scalingType;
    }

    public void setScalingType(RendererCommon.ScalingType scalingType) {
        this.scalingType = scalingType;
    }

    public int getLayoutHeight() {
        return layoutHeight;
    }

    public void setLayoutHeight(int layoutHeight) {
        this.layoutHeight = layoutHeight;
    }

    public int getLayoutPositionX() {
        return layoutPositionX;
    }

    public void setLayoutPositionX(int layoutPositionX) {
        this.layoutPositionX = layoutPositionX;
    }

    public int getLayoutPositionY() {
        return layoutPositionY;
    }

    public void setLayoutPositionY(int layoutPositionY) {
        this.layoutPositionY = layoutPositionY;
    }

    public int getLayoutWidth() {
        return layoutWidth;
    }

    public void setLayoutWidth(int layoutWidth) {
        this.layoutWidth = layoutWidth;
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public boolean iszOrderMediaOverlay() {
        return zOrderMediaOverlay;
    }

    public void setzOrderMediaOverlay(boolean zOrderMediaOverlay) {
        this.zOrderMediaOverlay = zOrderMediaOverlay;
    }
}
