package com.sohu.kurento.netClient;

import com.sohu.kurento.bean.RoomBean;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by jingbiaowang on 2015/7/22.
 */
public interface KWEvent {

	public void portError(String msg);

	//from server.

	public void onRegisterRoomSuccess(RoomBean room);

	public void onRegisterRoomFailure(String msg);

	public void onRemoteAnswer(String sdp);

	public void onRemoteIceCandidate(final IceCandidate candidate);

	public void onDisconnect();

	//接受聊天信息。
	public void onMessage(String msg);

	//from peerconnection.
	public void onLocalSdp(SessionDescription localsdp);

	/**
	 *
	 */
	public void onClientPrepareComplete();

	/**
	 * Callback fired once local Ice candidate is generated.
	 *
	 * @param candidate
	 */
	public void onIceCandidate(final IceCandidate candidate);

	/**
	 * Callback fired once connection is established(IceConnectionState is CONNECTED).
	 */
	public void onIceConnected();

	/**
	 * Callback fired once connection is closed(IceConnectionoState is DISCONNECTED).
	 */
	public void onIceDisconnected();

	/**
	 * Callback fired once peer connection is closed.
	 */
	public void onPeerConnectionClosed();
}
