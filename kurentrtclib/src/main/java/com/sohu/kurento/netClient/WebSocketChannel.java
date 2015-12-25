package com.sohu.kurento.netClient;

import com.sohu.kurento.util.LogCat;
import com.sohu.kurento.util.LooperExecutor;
import com.sohu.kurento.util.SinglExecterPool;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

/**
 * Created by jingbiaowang on 2015/7/21.
 * <p/>
 * Websocket channel.
 */
public class WebSocketChannel {


	private WebSocketConnection wsc;
	private WebSocketEvents wsEvents;
	private String wsUrl;

	private ArrayList<String> msges = new ArrayList<>();
    private LooperExecutor executor;

	private Object waitLock = new Object();
	private static final int WAIT_TIME = 500;
	private boolean lockEvent;

	/**
	 * webSocket state.
	 */
	private WebSocketState state;


	public interface WebSocketEvents {

		void onError(String e);

		void onConnected();

		void onMessage(String msg);

		void onClosed(String msg);
	}

	enum WebSocketState {
		NEW, CONNECTED, CLOSED, ERROR
	}


	public WebSocketChannel() {
		this.wsc = new WebSocketConnection();
		state = WebSocketState.NEW;

	}


//    public void setExecutor(LooperExecutor executor) {
//        this.executor = executor;
//    }

    /**
	 * 链接 .
	 *
	 * @param urlStr websocket url.
	 */
	public void connect(String urlStr, WebSocketEvents wsEvents) {
		this.wsUrl = urlStr;
		this.wsEvents = wsEvents;
		checkvalidThreadMethod(new ValidThreadCall() {
			@Override
			public void onValidThread() {
				try {
					LogCat.debug("websocket connect---------.");
					//handler is weakpreference.
					wsc.connect(wsUrl, new KWebSocketHandler());
				} catch (WebSocketException e) {
					e.printStackTrace();
					WebSocketChannel.this.wsEvents.onError(e.getMessage());
				}

			}
		});

	}

	public void sendMsg(final String msg) {

		checkvalidThreadMethod(new ValidThreadCall() {
			@Override
			public void onValidThread() {
				sendMsgs(msg);
			}
		});

	}

	/**
	 * Store the msg if the websocket hasn't connected.
	 *
	 * @param msg
	 */
	private void sendMsgs(String msg) {

		switch (state) {

			case NEW: {

				msges.add(msg);
				break;
			}

			case CONNECTED: {

				for (String sendMsg : msges) {
					wsc.sendTextMessage(sendMsg);
					LogCat.debug("send msg ====:" + msg);
				}

				msges.clear();
				wsc.sendTextMessage(msg);
				LogCat.debug("send msg ====:" + msg);
				break;
			}

			case CLOSED:
			case ERROR: {

				if (!wsc.isConnected()) {
					LogCat.e("the websocket isn't opening. you can't send any sth.");
					wsEvents.onError("the websocket isn't opening. you can't send any sth.");
					break;
				}
			}

		}

	}


	/**
	 * @param waitForComplete true, 等待网络请求完成。
	 */
	public void disconnect(final boolean waitForComplete) {

		//TODO send server to disconnect server.
		checkvalidThreadMethod(new ValidThreadCall() {
								   @Override
								   public void onValidThread() {
									   if (wsc.isConnected()) {
										   wsc.disconnect();
										   LogCat.debug("websocket closed!");
										   if (waitForComplete) {

											   synchronized (waitLock) {
												   while (!lockEvent) {
													   try {
														   waitLock.wait(WAIT_TIME);
														   //每500ms检测一次网络请。
													   } catch (InterruptedException e) {
														   e.printStackTrace();
													   }
												   }
											   }
										   }
									   }
								   }
							   }

		);

	}

	class KWebSocketHandler implements WebSocket.ConnectionHandler {

		@Override
		public void onOpen() {
			state = WebSocketState.CONNECTED;
			LogCat.debug("KWebSocket opened !");
			lockEvent = false;
			wsEvents.onConnected();
		}

		@Override
		public void onClose(int i, final String s) {
			state = WebSocketState.CLOSED;

			synchronized (waitLock) {
				lockEvent = true;
				waitLock.notify();
			}
			LogCat.debug("KWebSocke closed !");
			checkvalidThreadMethod(new ValidThreadCall() {
				@Override
				public void onValidThread() {
					wsEvents.onClosed(s);
				}
			});

		}

		@Override
		public void onTextMessage(final String s) {
			LogCat.debug("receive msg on Wschannnel***: " + s);
			checkvalidThreadMethod(new ValidThreadCall() {
				@Override
				public void onValidThread() {
					wsEvents.onMessage(s);
				}
			});
		}

		@Override
		public void onRawTextMessage(byte[] bytes) {

		}

		@Override
		public void onBinaryMessage(byte[] bytes) {

		}
	}

	public boolean isClosed() {
		return !wsc.isConnected();
	}


	interface ValidThreadCall {

		public void onValidThread();
	}

	/**
	 * make sure the method run in looper thread.
	 *
	 * @param callback
	 */
	private void checkvalidThreadMethod(final ValidThreadCall callback) {
//		if (executor.checkOnLooperThread()) {
//			callback.onValidThread();
//		} else {
//			executor.execute(new Runnable() {
//				@Override
//				public void run() {
//					callback.onValidThread();
//				}
//			});
//		}

        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
                callback.onValidThread();
            }
        });
	}

	public WebSocketConnection getWsc() {
		return wsc;
	}
}
