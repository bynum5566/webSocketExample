package webIni.serverEnd;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.*;
import freemarker.log.Logger;
import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;


@ServerEndpoint("/send/{topic}")
@Component
public class WebSocketServer  {
    static Logger logger = Logger.getLogger("WebSocketServer");
    /**
     * 靜態變數，用來記錄目前連線數，大專案請改成thread-safe
     */
    private static int onlineCount = 0;
    /**
     * concurrent包的thread-safe Set，存在每個client對應的WebSocketServer對象
     */
    private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 與某個client的會話連線，需透過它對client發送數據
     */
    private Session session;
    /**
     * 接收频道topic，測試專案所以我們這邊用client名稱當頻道標題
     */
    private String topic = "";

    /**
     * 建立連線成功，呼叫下列方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("topic") String topic) {
        this.session = session;
        this.topic = topic;
        if (webSocketMap.containsKey(topic)) {
            webSocketMap.remove(topic);
            webSocketMap.put(topic, this);
            //加入set中
        } else {
            webSocketMap.put(topic, this);
            //加入set中，在線人數+1
            addOnlineCount();
        }

        logger.info("client: " + topic + "已連線，目前線上人數為:" + getOnlineCount());
        try {
            sendMessage("連線成功");
        } catch (IOException e) {
            logger.error("client:" + topic + "，發現網路異常!(IOException)");
        }
    }


    /**
     * 連線關閉時呼叫的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(topic)) {
            webSocketMap.remove(topic);
            //從set中刪除
            subOnlineCount();
        }
        logger.info("client" + topic + "已退出連線，目前線上人數: " + getOnlineCount());
    }

    /**
     * 收到client訊息後呼叫的方法
     *
     * @param message client送來的訊息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("client:" + topic + ",訊息:" + message);
        //可以改寫成群組訊息，訊息可儲存到DB或redis
        if (StringUtils.isNotBlank(message)) {
            try {
                //解析發送的內文
                JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
                //追加發送人，防止竄改
                jsonObject.addProperty("from_topic", this.topic);
                String to_topic = jsonObject.get("to_topic").getAsString();
                //傳送給對應 toUserId 的 client 的 websocket
                if (StringUtils.isNotBlank(to_topic) && webSocketMap.containsKey(to_topic)) {
                    logger.info("即將送出訊息:" + jsonObject.toString()+" to "+to_topic);
                    webSocketMap.get(to_topic).sendMessage(jsonObject.toString());
                } else {
                    logger.error("目前請求的to_topic: " + to_topic + " 並未與伺服器連線");
                    //若要做成離線訊息，則可改寫傳至DB或redis處理
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("client: " + this.topic + "發生錯誤，原因:" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 實作 server 主動推送訊息
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 寄發自訂訊息
     */
    public static void sendInfo(String message, @PathParam("topic") String topic) throws IOException {
        logger.info("發送訊息給: " + topic + "，訊息內容: " + message);
        if (StringUtils.isNotBlank(topic) && webSocketMap.containsKey(topic)) {
            webSocketMap.get(topic).sendMessage(message);
        } else {
            logger.error("client: " + topic + "，目前不在線上");
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

}
