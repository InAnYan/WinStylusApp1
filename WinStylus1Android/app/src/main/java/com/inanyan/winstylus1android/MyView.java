package com.inanyan.winstylus1android;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MyView extends View {
    private final Paint paint = new Paint();

    // Send data
    private float dotX = 0;
    private float dotY = 0;
    private boolean button1 = false; // Touch
    private boolean button2 = false; // Stylus
    private byte[] oldData;

    public TextView statusText; // From MainActivity

    // Connection
    private Socket socket;
    private OutputStream socketOutput;

    public MyView(Context context, AttributeSet set) {
        super(context, set);
    }

    // From connect button
    public void connect(String address) {
        new Thread() {
            @Override
            public void run() {
                closeConnection();
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByName(address),
                            55555), 2000);
                    socketOutput = socket.getOutputStream();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Connected");
                        }
                    });
                } catch (Exception e) {
                    createErrorAlert("Connection error", e.getMessage());
                    post(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Not connected");
                        }
                    });
                }
            }
        }.start();
    }

    // Movement handle
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        updatePointer(event);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        updatePointer(event);
        return true;
    }

    private void updatePointer(MotionEvent event) {
        dotX = event.getX();
        dotY = event.getY();

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                button1 = true;
                break;
            case MotionEvent.ACTION_UP:
                button1 = false;
                break;
        }

        button2 = event.getButtonState() == MotionEvent.BUTTON_STYLUS_PRIMARY;

        super.invalidate();
    }

    // Drawing dot and sending data
    @Override
    public void onDraw(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(button1 ? Color.BLUE : (button2 ? Color.GREEN : Color.BLACK));
        canvas.drawCircle(dotX, dotY, 5, paint);

        sendData();
    }

    // Sending data
    private void sendData() {
        new Thread() {
            @Override
            public void run() {
                byte[] temp = convertDataToBytes();
                if(isConnected() && (statusText.getText() != "Not connected") && (oldData != temp)) {
                    try {
                        socketOutput.write(temp);
                        socketOutput.flush();
                        oldData = temp;
                    } catch (Exception e) {
                        createErrorAlert("Send error", e.getMessage());
                        post(new Runnable() {
                            @Override
                            public void run() {
                                statusText.setText("Not connected");
                            }
                        });
                    }
                }
            }
        }.start();
    }

    private byte[] convertDataToBytes() {
        ByteBuffer b = ByteBuffer.allocate(24);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putDouble(dotX/getWidth());
        b.putDouble(dotY/getHeight());
        short buttonsInt = 0;
        if(button1) buttonsInt += 1;
        if(button2) buttonsInt += 2;
        b.putShort(buttonsInt);
        b.putShort(buttonsInt);
        b.putShort(buttonsInt);
        b.putShort(buttonsInt);
        return b.array();
    }

    private void createErrorAlert(String title, String message) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(getContext());
        dlg.setMessage(message);
        dlg.setTitle(title);
        dlg.setPositiveButton("OK", null);
        post(new Runnable() {
            @Override
            public void run() {
                dlg.create().show();
            }
        });
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isOutputShutdown();
    }

    public void finish() {
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (socketOutput != null) socketOutput.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            createErrorAlert("Closing error", e.getMessage());
        }
    }
}
