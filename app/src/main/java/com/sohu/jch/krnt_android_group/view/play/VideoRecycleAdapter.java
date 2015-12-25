package com.sohu.jch.krnt_android_group.view.play;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sohu.jch.krnt_android_group.R;
import com.sohu.jch.krnt_android_group.controller.Participant;
import com.sohu.kurento.bean.VideoViewParam;
import com.sohu.kurento.group.KPeerConnectionClient;
import com.sohu.kurento.util.LogCat;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;

/**
 * Created by jingbiaowang on 2015/12/7.
 */
public class VideoRecycleAdapter extends RecyclerView.Adapter<VideoRecycleAdapter.VideoHolder> {


    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 0;
    private static final int LOCAL_Y_CONNECTED = 0;
    private static final int LOCAL_WIDTH_CONNECTED = 100;
    private static final int LOCAL_HEIGHT_CONNECTED = 100;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;

    private Context context;

    private String TAG = "adapter";

    private ArrayList<Participant> data;

    public VideoRecycleAdapter(Context context) {
        this.context = context;
        this.data = new ArrayList<>();
    }

    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LogCat.debug(TAG + " onCreateViewHolder position ");
        VideoHolder viewHolder = new VideoHolder(LayoutInflater.from(context).inflate(R.layout.video_item, parent, false));
        viewHolder.videoview.init(data.get(0).getEglBase().getContext(), new PVideoRenderEvents());

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VideoHolder holder, int position) {
        LogCat.debug(TAG + "onBindViewHolder position " + position);

        Participant participant = data.get(position);

        if (participant.getConnectionType() != KPeerConnectionClient.ConnectionType.SEND_ONLY) {
            participant.attachRemoteRenderer(holder.videoitemperlayout, getRemoteVideoViewParam(position), holder.videoview);
        } else if (participant.getConnectionType() != KPeerConnectionClient.ConnectionType.READ_ONLY) {
            participant.attchLocalRenderer(holder.videoitemperlayout, getLocalVideoViewParam(position), holder.videoview);
        }
        if (participant.isIceConnected()) {
            holder.videopro.setVisibility(View.GONE);
        }

        holder.participant = data.get(position);
        holder.videonametv.setText(data.get(position).getName());
    }

    @Override
    public void onViewDetachedFromWindow(VideoHolder holder) {
        LogCat.debug(TAG + "onViewDetachedFromWindow " + holder.videonametv.getText().toString());
        holder.dispatchRender();
        super.onViewDetachedFromWindow(holder);
    }


    @Override
    public int getItemCount() {
        int count = data.size();

        LogCat.debug("adapter count : " + count);

        return count;
    }

    public void notifyInsertedParticipant(int position, Participant participant) {
        LogCat.i("adapter add view : " + participant.getName() + " position : " + position);
        data.add(participant);
        notifyItemInserted(position);
    }

    public void notifyRemovedParticipant(int position, Participant participant) {
        LogCat.i("adapter remove view : " + participant);
        data.remove(position);
        notifyItemRemoved(position);
    }

    public VideoViewParam getLocalVideoViewParam(int position) {

        VideoViewParam viewParam = new VideoViewParam(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED, true, RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        return viewParam;
    }

    public VideoViewParam getRemoteVideoViewParam(int position) {
        VideoViewParam viewParam = new VideoViewParam(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED, true, RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        return viewParam;
    }

    public static class VideoHolder extends RecyclerView.ViewHolder {

        public SurfaceViewRenderer videoview;
        public PercentFrameLayout videoitemperlayout;
        public TextView videonametv;
        public ProgressBar videopro;
        public Participant participant;
        private View containerView;

        public VideoHolder(View itemView) {

            super(itemView);
            this.containerView = itemView;
            initialize(itemView);
        }

        private void initialize(View holderView) {

            videoview = (SurfaceViewRenderer) holderView.findViewById(R.id.video_view);
            videoitemperlayout = (PercentFrameLayout) holderView.findViewById(R.id.video_item_per_layout);
            videonametv = (TextView) holderView.findViewById(R.id.video_name_tv);
            videopro = (ProgressBar) holderView.findViewById(R.id.video_pro);
        }

        public void dispatchRender() {

            if (participant.getConnectionType() != KPeerConnectionClient.ConnectionType.READ_ONLY) {
                participant.dispatchRemoteRenderer(videoview);
            }
            if (participant.getConnectionType() != KPeerConnectionClient.ConnectionType.SEND_ONLY) {
                participant.dispatchLocalRenderer(videoview);
            }
        }

    }

    private class PVideoRenderEvents implements RendererCommon.RendererEvents {

        @Override
        public void onFirstFrameRendered() {

        }

        @Override
        public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

        }
    }
}
