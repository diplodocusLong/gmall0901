package com.lianglong.gmall.passport.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.lianglong.gmall.bean.UserInfo;
import com.lianglong.gmall.passport.config.JwtUtil;
import com.lianglong.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


@Controller
public class PassportController {
    @Value("${token.key}")
    String signKey;
    @Reference
    private UserService userService;


    @RequestMapping("index")
    public String index(HttpServletRequest request) {

        String originUrl = request.getParameter("originUrl");

        request.setAttribute("originUrl", originUrl);

        return "index";
    }


    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {
        //取得ip地址
        String remoteAddr = request.getHeader("X-forwarded-for");

        if (userInfo != null) {

            UserInfo info = userService.login(userInfo);

            if (info == null) {
                return "fail";
            } else {
                Map<String, Object> map = new HashMap();
                map.put("userId", info.getId());
                map.put("nickName", info.getNickName());
                String token = JwtUtil.encode(signKey, map, remoteAddr);
                return token;
            }
        }

        return "fail";

    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        String token = request.getParameter("token");

        String currentIp = request.getParameter("currentIp");

        Map<String, Object> map = JwtUtil.decode(token, signKey, currentIp);

        if(map!=null){
             String userId=(String)map.get("userId");

            UserInfo verify = userService.verify(userId);

            if(verify!=null){
                return "success";
            }

        }

        return "fail";
    }

}