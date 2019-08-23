package com.lianglong.gmall.service;

import com.lianglong.gmall.bean.PaymentInfo;

public interface PaymentService {
    /**
     * 保存交易信息
     * @param paymentInfo
     */
    void  savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 查巡交易信息
     * @param paymentInfoQuery
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    /**
     * 根据第三方单号 更新交易状态
     * @param out_trade_no
     * @param paymentInfoUPD
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    /**
     * 发送交易结果信息
     * @param paymentInfo
     * @param result
     */
    public void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 主动询问支付定 交易是否成功
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 发送延迟队列
     * @param outTradeNo
     * @param delaySec
     * @param checkCount
     */
    public void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 处理过期的交易信息
     * @param id
     */
    void closePayment(String id);
}
