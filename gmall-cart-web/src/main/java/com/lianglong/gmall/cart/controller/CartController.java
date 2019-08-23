package com.lianglong.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.lianglong.gmall.bean.CartInfo;
import com.lianglong.gmall.bean.SkuInfo;
import com.lianglong.gmall.config.LoginRequire;
import com.lianglong.gmall.service.CartService;
import com.lianglong.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {


    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private CartService cartService;

    @Reference
    ManageService manageService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {

        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");

        if(skuNum==null){

            skuNum="1";
        }

        //先尝试从域中得到userId
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            //用户没有登录,存到cookie
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        } else {
            //用户登录,持久化到数据库,同步到redis
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartList = null;
        if (userId == null) {
            //用户没有登录需要从cookie中列出 构物车中 各个商品
            cartList = cartCookieHandler.getCartList(request);

            request.setAttribute("cartInfoList", cartList);

            return "cartList";

        }

        //用户已经登录 上边return 没有执行 继续下边代码
        //用户已经登录从数据库拿数据  但是要先检查cookie中是否有 尚未存入数据库的数据
        //如果有 先存入数据库再刷新redis 最后再返回数据

        //cookie中查出来的list
        cartList = cartCookieHandler.getCartList(request);

        if (cartList!= null && cartList.size() > 0) {
            // 开始合并
            cartList = cartService.mergeToCartList(cartList, userId);
            // 删除cookie中的购物车
            cartCookieHandler.deleteCartCookie(request, response);

            request.setAttribute("cartInfoList", cartList);

        }else {

            cartList=cartService.getCartList(userId);

            request.setAttribute("cartInfoList",cartList);
        }
        return "cartList";
    }


    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {
        //首先分两种情况 登录  和   未登录

        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");

        if (userId!= null) {
            cartService.checkCart(skuId,isChecked,userId);
        } else {
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);

        if (cookieHandlerCartList!=null&&cookieHandlerCartList.size()>0) {

            cartService.mergeToCartList(cookieHandlerCartList,userId);

            cartCookieHandler.deleteCartCookie(request,response);

        }
        return "redirect://order.gmall.com/trade";

    }
}