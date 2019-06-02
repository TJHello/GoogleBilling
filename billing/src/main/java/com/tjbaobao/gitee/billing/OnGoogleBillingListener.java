package com.tjbaobao.gitee.billing;

import android.support.annotation.NonNull;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

/**
 * 作者:TJbaobao
 * 时间:2019/6/2  13:51
 * 说明:
 * 使用：
 */
public class OnGoogleBillingListener {

    @SuppressWarnings("WeakerAccess")
    public String tag = null;

    /**
     * 查询成功
     * @param skuType 内购或者订阅
     * @param list 商品列表
     * @param isSelf 是否是当前页面的结果
     */
    public void onQuerySuccess(@NonNull String skuType, @NonNull List<SkuDetails> list, boolean isSelf){}

    /**
     * 购买成功
     * @param list 商品列表
     * @param isSelf 是否是当前页面的结果
     */
    public void onPurchaseSuccess(@NonNull List<Purchase> list, boolean isSelf){}

    /**
     * 初始化成功
     * @param isSelf 是否是当前页面的结果
     */
    public void onSetupSuccess(boolean isSelf){}

    /**
     * 链接断开
     */
    @SuppressWarnings("WeakerAccess")
    public void onBillingServiceDisconnected(){ }

    /**
     * 消耗成功
     * @param purchaseToken token
     * @param isSelf 是否是当前页面的结果
     */
    public void onConsumeSuccess(@NonNull String purchaseToken,boolean isSelf){}

    /**
     * 失败回调
     * @param tag {@link GoogleBillingUtil.GoogleBillingListenerTag}
     * @param responseCode 返回码{https://developer.android.com/google/play/billing/billing_reference}
     * @param isSelf 是否是当前页面的结果
     */
    public void onFail(@NonNull GoogleBillingUtil.GoogleBillingListenerTag tag, int responseCode, boolean isSelf){}

    /**
     * google组件初始化失败等等。
     * @param tag {@link GoogleBillingUtil.GoogleBillingListenerTag}
     * @param isSelf 是否是当前页面的结果
     */
    public void onError(@NonNull GoogleBillingUtil.GoogleBillingListenerTag tag, boolean isSelf){}

}
