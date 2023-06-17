package com.lagou.edu.message.server;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.lagou.edu.common.jwt.JwtUtil;
import com.lagou.edu.common.string.GsonUtil;
import com.lagou.edu.message.client.dto.Message;
import com.lagou.edu.message.consts.Constants;
import com.lagou.edu.message.server.store.StoreFacotryProvider;
import com.lagou.edu.message.util.PushUtils;
import com.lagou.edu.message.util.ServerConfigUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Slf4j
public class PushServer {

    public static final PushServer pushServer = new PushServer();
    private Namespace pushNamespace;
    private SocketIOServer server;

    private PushServer() {
        final Configuration configuration = new Configuration();
        configuration.setStoreFactory(StoreFacotryProvider.getRedissonStoreFactory());
        configuration.setAuthorizationListener(new UserAuthorizationListener());
        configuration.setPort(ServerConfigUtils.instance.getWebSocketPort());
        configuration.setContext(ServerConfigUtils.instance.getWebSocketContext());
        configuration.setOrigin(ServerConfigUtils.instance.getWebSocketOrigin());

        final SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        configuration.setSocketConfig(socketConfig);

        server = new SocketIOServer(configuration);
        pushNamespace = (Namespace) server.addNamespace(ServerConfigUtils.instance.getWebSocketContext());

        configuration.setExceptionListener(new ExceptionListener() {
            @Override
            public void onEventException(Exception e, List<Object> list, SocketIOClient socketIOClient) {
                UUID sessionId = socketIOClient.getSessionId();
                log.error("onEventException, sessionId:{}, roomList:{}",sessionId,socketIOClient.get(Constants.ROOM),e);
            }

            @Override
            public void onDisconnectException(Exception e, SocketIOClient socketIOClient) {
                UUID sessionId = socketIOClient.getSessionId();
                log.error("onDisconnectException, sessionId:{}, roomList:{}",sessionId,socketIOClient.get(Constants.ROOM),e);
            }

            @Override
            public void onConnectException(Exception e, SocketIOClient socketIOClient) {
                UUID sessionId = socketIOClient.getSessionId();
                log.error("onConnectException, sessionId:{}, roomList:{}",sessionId,socketIOClient.get(Constants.ROOM),e);
            }

            @Override
            public void onPingException(Exception e, SocketIOClient socketIOClient) {
                UUID sessionId = socketIOClient.getSessionId();
                log.error("onPingException, sessionId:{}, roomList:{}",sessionId,socketIOClient.get(Constants.ROOM),e);
            }

            @Override
            public boolean exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
                try {
                    Channel channel = channelHandlerContext.channel();
                    if(channel == null){
                        log.error("exceptionCaught, channel:{}, isOpen:{}, remoteAddress:{}",channel.id(),channel.isOpen(),channel.remoteAddress());
                    }
                }catch (Exception e){
                    log.error("exceptionCaught",e);
                }
                return false;
            }
        });

        pushNamespace.addEventListener("send-message", Map.class, new DataListener<Map>() {
            @Override
            public void onData(SocketIOClient socketIOClient, Map map, AckRequest ackRequest) throws Exception {
                JwtUtil.JwtResult userInfo = UserAuthorizationListener.getUserInfo(socketIOClient.getHandshakeData());
                Integer userId = userInfo.getUserId();
                String content = (String) map.get("content");
                String type = (String) map.get("type");
                Message message = new Message(type, content, userId);
                log.info("from {} userId {} type {} content {} ",userId,userId,type,content);
            }
        });

        pushNamespace.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                UUID sessionId = socketIOClient.getSessionId();
                JwtUtil.JwtResult userInfo = UserAuthorizationListener.getUserInfo(socketIOClient.getHandshakeData());
                if(userInfo != null){
                    log.info("disconnect userId: {} userName: {} sid: {} ",userInfo.getUserId(),userInfo.getUserName(),sessionId);
                }else{
                    socketIOClient.disconnect();
                    log.info("client get handShakeData is null, disconnect. sid: {} ",sessionId);
                    return;
                }
                String userIdStr = Integer.toString(userInfo.getUserId());
                List<String> roomList = PushUtils.getRoomList(userIdStr, null, null);
                for (String roomStr:roomList) {
                    socketIOClient.leaveRoom(roomStr);
                }
                try{
                    List<String> oldRoomList = socketIOClient.get(Constants.ROOM);
                    if(oldRoomList != null && oldRoomList.size() > 0){
                        for (String room: oldRoomList) {
                            if(StringUtils.isBlank(room)){
                                continue;
                            }
                            socketIOClient.leaveRoom(room);
                        }
                    }
                    socketIOClient.disconnect();
                }catch (Exception e){
                    log.error("leave old room exception, sid: {}",sessionId,e);
                }finally {
                    try{
                        socketIOClient.del(Constants.ROOM);
                        log.info("sid: {},del has success, field: {}",sessionId,Constants.ROOM);
                    }catch (Exception e){
                        log.info("sid: {},del has exception, field: {}",sessionId,Constants.ROOM);
                    }
                }
            }
        });

        pushNamespace.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                try{
                    UUID sessionId = socketIOClient.getSessionId();
                    JwtUtil.JwtResult userInfo = UserAuthorizationListener.getUserInfo(socketIOClient.getHandshakeData());
                    if(userInfo != null){
                        Set<String> allRooms = socketIOClient.getAllRooms();
                        for (String room: allRooms) {
                            socketIOClient.leaveRoom(room);
                        }
                        String userId = Integer.toString(userInfo.getUserId());
                        List<String> roomList = PushUtils.getRoomList(userId, null, null);
                        for (String roomStr:roomList) {
                            socketIOClient.joinRoom(roomStr);
                            log.info("userId: {},  sid: {}, join room {}",userId,sessionId,roomStr);
                        }
                    }else{
                        socketIOClient.disconnect();
                        log.warn("sid: {} has no userId ",sessionId);
                    }
                }catch (Exception e){
                    log.error("addConnectListener - err",e);
                }
            }
        });
    }

    /**
     * 推送消息
     *
     * @param message
     */
    public void push(Message message) {
    	final String type,json;
    	final Integer userId;
    	long l11;
    	try{
    	    long l0 = System.currentTimeMillis();
            type = message.getType();
            userId = message.getUserId();
            json = GsonUtil.toJson(message);
            l11 = System.currentTimeMillis();
            if(l11 - l0 > 50l){
                log.info("当前node.push耗时 ,time= {} ms",l11 - l0);
            }
        }finally {

        }

    	String room;
    	long l12;
    	try{
    	    if(userId == null){
    	        throw new NullPointerException("userId 不能为空");
            }
            room = PushUtils.getRoom(null, userId, null);
    	    log.info("send message to {}, type: {}",room,type);
    	    l12 = System.currentTimeMillis();
            if(l12 - l11 > 50l){
                log.info("当前node.push耗时 ,time= {} ms",l12 - l11);
            }
        }finally {

        }

        Packet packet;
    	long l13;
    	try{
            packet = new Packet(PacketType.MESSAGE);
            packet.setSubType(PacketType.EVENT);
            packet.setName("message");
            ArrayList<Object> data = new ArrayList<>();
            data.add(json);
            packet.setData(data);
            packet.setNsp(pushNamespace.getName());

            l13 = System.currentTimeMillis();
            if(l13 - l12 > 50l){
                log.info("当前node.push耗时 ,time= {} ms",l13 - l12);
            }
        }finally {

        }

    	int i1;
    	final long l2;
    	try{
    	    i1 = 0;
    	    try{
                Iterable<SocketIOClient> clients = pushNamespace.getRoomClients(room);
                for (SocketIOClient client:clients) {
                    client.send(packet);
                    i1++;
                }
            }catch (Exception e){
    	        log.error("当前服务直接推送失败",e);
            }

            l2 = System.currentTimeMillis();

            if(l2 - l13 > 50l){
                log.info("当前node.push耗时 ,time= {} ms",l2 - l13);
            }
        }finally {

        }
    }

    /**
     * 同步启动服务；
     */
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            log.error("Push server start failed!", e);
            System.exit(-1);
        }
    }

    /**
     * 停止服务
     */
    public void stop() {
        server.stop();
    }

    public Map<String, Object> getStatus() {
        HashMap<String, Object> status = new HashMap<>();
        status.put("namespace", pushNamespace.getName());   // namespace
        status.put("rooms", pushNamespace.getRooms());
        status.put("clients", pushNamespace.getAllClients().size());
        return status;
    }

}
