package com.tjbaobao.gitee.billing;

import android.support.annotation.NonNull;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者:天镜baobao
 * 时间:2019/6/2  13:51
 * 说明:允许对该封装进行改动，但请注明出处。
 * 使用：
 *
 * Copyright 2019 天镜baobao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * @param purchase 商品
     * @param isSelf 是否是当前页面的结果
     * @param index 在当前列表的序号
     * @param size 商品列表的长度
     */
    public void onPurchaseSuccess(@NonNull Purchase purchase, boolean isSelf,int index,int size){
        onPurchaseSuccess(purchase,isSelf);
    }

    /**
     * 购买成功
     * @param purchase 商品
     * @param isSelf 是否是当前页面的结果
     */
    public void onPurchaseSuccess(@NonNull Purchase purchase, boolean isSelf){}


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

    /**
     * 获取历史订单-无论是否还有效
     * @param purchase 商品实体
     */
    public void onQueryHistory(@NonNull Purchase purchase){

    }

}
