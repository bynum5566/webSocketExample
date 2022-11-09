package webIni.serverEnd;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class ServerEndConfig {
    /**
     * 這個bean會自動註冊使用@ServerEndpoint註解宣告的物件，若不使用會產生404
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}