package com.lianglong.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.lianglong.gmall.bean.OrderInfo;
import com.lianglong.gmall.bean.PaymentInfo;
import com.lianglong.gmall.bean.enums.PaymentStatus;
import com.lianglong.gmall.config.LoginRequire;
import com.lianglong.gmall.payment.config.AlipayConfig;
import com.lianglong.gmall.service.OrderService;
import com.lianglong.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    AlipayClient alipayClient;


    @Reference
    OrderService orderService;


    @RequestMapping("index")
   // @LoginRequire
    public String index(HttpServletRequest request, Model model){
        // 获取订单的id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        model.addAttribute("orderId",orderId);
        model.addAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @RequestMapping(value = "/alipay/submit",method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        //获取订单Id
        String orderId = request.getParameter("orderId");
        //取得订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.toString());

        paymentService.savePaymentInfo(paymentInfo);

        // 需要生产二维码 需要将参数放入配置文件中，从配置文件中读取。
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调路径
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调路径
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());

        alipayRequest.setBizContent(JSON.toJSONString(map));

        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单


        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset==UTF-8");
        // 当生成支付二维码的时候，发送一个消息队列，这个消息队列中存储这 第三方交易编号，时间间隔，检查次数。
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;

    }
    // 同步回调的控制器
    @RequestMapping("alipay/callback/return")
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }

    // 异步回调的控制器
    @RequestMapping("alipay/callback/notify")
    public String callBackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) throws AlipayApiException {
        // 验签：
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 获取第三方交易编号
            String out_trade_no = paramMap.get("out_trade_no");

            // 需要保证支付的交易状态TRADE_SUCCESS或者TRADE_FINISHED
            String trade_status = paramMap.get("trade_status");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){

                // 调用服务层查询paymentInfo对象 select * from paymentInfo where out_trade_no = ?
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoResult = paymentService.getPaymentInfo(paymentInfoQuery);
                // 保证交易记录中的状态不能为IPAD,CLOSE 时为验签失败！ 查询paymentInfo 中的payment_status 状态不能为IPAD,CLOSE
                if (paymentInfoResult.getPaymentStatus()==PaymentStatus.PAID || paymentInfoResult.getPaymentStatus()==PaymentStatus.ClOSED ){
                    return "failure";
                }else{
                    // 验签成功之后，需要修改状态！
                    PaymentInfo paymentInfoUPD = new PaymentInfo();
                    // 将交易状态变为已支付
                    paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);

                    // 回调时间设置为当前时间
                    paymentInfoUPD.setCallbackTime(new Date());

                    // 修改内容体：
                    paymentInfoUPD.setCallbackContent(paramMap.toString());
                    // update paymentInfo set  payment_status = PaymentStatus.PAID where out_trade_no = ?
                    paymentService.updatePaymentInfo(out_trade_no,paymentInfoUPD);
                    paymentService.sendPaymentResult(paymentInfoResult,"success");
                    return "success";
                }
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result") String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }
    // 查询订单信息
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        return ""+flag;
    }


}
