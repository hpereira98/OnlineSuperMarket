package server;

import middleware.server.ConcurrentQueue;
import middleware.server.Pair;
import middleware.server.ServerMessageListener;
import middleware.server.Triplet;
import middleware.proto.MessageOuterClass.Message;
import middleware.spread.SpreadConnector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestManager implements Runnable {
    private static RequestManager instance = null;
    private static ServerMessageListener messageListener = null;
    private static Map<Pair<String,String>, WaitingRoom> waiting_requests = null;
    private static ConcurrentQueue<Message> sorted_requests = null;

    public RequestManager(ServerMessageListener serverMessageListener){
        messageListener = serverMessageListener;
        waiting_requests = new ConcurrentHashMap<>();
        sorted_requests = new ConcurrentQueue<>();
    }

    public static RequestManager initialize(ServerMessageListener serverMessageListener){
        if(instance == null)
            instance = new RequestManager(serverMessageListener);
        return instance;
    }


    @Override
    public void run() {

        Triplet<Boolean, Long, Message> message;

        while((message = messageListener.getNextRequest()) != null){

            if (!message.getFirst()) {
                Pair<String, String> pair = new Pair<>(message.getThird().getRequest().getSender(), message.getThird().getRequest().getUuid());
                waiting_requests.put(pair,new WaitingRoom());
            }

            if(messageListener.isPrimary())
                sorted_requests.add(message.getThird());

        }
    }

    public static Message publishRequest(Message msg){

        WaitingRoom wr = new WaitingRoom();
        wr.activate();

        String sender = messageListener.getMyself();
        String key = null;
        do{
            key = UUID.randomUUID().toString();
        } while(waiting_requests.putIfAbsent(new Pair<>(sender, key), wr) != null);

        Message.Builder builder = msg.toBuilder();
        builder.getRequestBuilder().setUuid(key).setSender(sender);
        msg = builder.build();

        SpreadConnector.cast(msg.toByteArray(), Set.of("Servers"));

        return wr.waitToProceed();
    }

    public static void putResponse(Pair<String,String> request_key, Message msg){

        WaitingRoom wr = waiting_requests.get(request_key);

        if(wr.isActive())
            wr.putMessage(msg);
        else
            waiting_requests.remove(request_key);

    }

    public static Message getNextRequest(){
        return sorted_requests.poll();
    }
}