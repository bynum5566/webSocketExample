package webIni.controller;

import webIni.serverEnd.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class SWController {

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * desc: 跳轉index.html頁面
     * @return
     */
    @RequestMapping("/")
    public String index() {
        return "index";
    }
}