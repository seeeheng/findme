package com.github.navisensfindme;

import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;

/* Implementation 3: Freelance Pirate */
class ZeroMQClient {
    private static final int REQUEST_TIMEOUT = 1000;
    private static final int MAX_RETRIES = 3;
    private String endpoint = "tcp://";
    private String ip;
    private static ZContext ctx;

    private ZMsg tryRequest(ZContext ctx, ZMsg request){
        Log.i("zeromq", "I: Sending message to " + this.endpoint);
        ZMQ.Socket client = ctx.createSocket(SocketType.REQ);
        client.connect(this.endpoint);

        // send message, wait safely for reply.
        ZMsg msg = request.duplicate();
        msg.send(client);

        ZMQ.Poller poller = ctx.createPoller(1);
        poller.register(client, ZMQ.Poller.POLLIN);
        poller.poll(REQUEST_TIMEOUT);

        ZMsg reply = null;
        if (poller.pollin(0)){
            reply = ZMsg.recvMsg(client);
        }

        ctx.destroySocket(client);
        poller.close();
        return reply;
    }

    private void send(ZMsg request){
        ZMsg reply = null;
        int retries;
        for (retries = 0; retries < MAX_RETRIES; retries++){
            reply = tryRequest(ctx, request);
            if(reply != null){
                reply.destroy();
                Log.i("zeromq", "Message sent and received.");
                break; // successful request!
            }
            Log.i("zeromq", "No response from " + endpoint + ", retrying...");
        }
        request.destroy();
    }

    void initializeClient(String ip){
        try(ZContext context = new ZContext()){
            ctx = context;
            this.ip = ip;
            // TODO: validate ip.
            this.endpoint = endpoint + ip;
        }
    }

    void stopClient(){
        // stop client.
    }

    void sendMessage(String[] messages){
        ZMsg request = new ZMsg();
        for (String message : messages) {
            request.add(message);
        }
        send(request);
    }
}