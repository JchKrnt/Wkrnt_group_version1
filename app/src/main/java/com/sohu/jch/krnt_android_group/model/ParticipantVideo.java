package com.sohu.jch.krnt_android_group.model;

import android.view.View;

import com.sohu.jch.krnt_android_group.view.play.PercentFrameLayout;

import org.webrtc.SurfaceViewRenderer;

/**
 * Created by jingbiaowang on 2015/12/7.
 */
public class ParticipantVideo {

    private PercentFrameLayout videoViewContainer;
    private SurfaceViewRenderer videoView;

    public SurfaceViewRenderer getVideoView() {
        return videoView;
    }

    public void setVideoView(SurfaceViewRenderer videoView) {
        this.videoView = videoView;
    }

    public PercentFrameLayout getVideoViewContainer() {
        return videoViewContainer;
    }

    public void setVideoViewContainer(PercentFrameLayout videoViewContainer) {
        this.videoViewContainer = videoViewContainer;
    }
}
