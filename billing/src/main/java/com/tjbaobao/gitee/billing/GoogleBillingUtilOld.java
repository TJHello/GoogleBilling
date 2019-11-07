package com.tjbaobao.gitee.billing;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.billingclient.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Created by TJbaobao on 2017/11/2.
 * CSDN:http://blog.csdn.net/u013640004/article/details/78257536
 *
 * 当前版本:V1.1.7
 * 更新日志:
 *
 * v1.1.7 2019/01/05
 * 开放消耗结果监听器设置-{@link #setOnConsumeResponseListener}
 *
 * v1.1.6 2018/09/05
 * 去掉我项目了的BaseApplication.getContext()的方法，初始现在需要传入一个Context，可以使用Application的Context
 * 对isGooglePlayServicesAvailable方法进行了说明，因为这个方法是要导入一个包才能使用的。
 * --> api "com.google.android.gms:play-services-location:11.8.0"
 *
 * v1.1.5 2018/07/13
 * 优化-尽可能处理了一些可能造成的内存泄漏的问题。
 * 修改-查询成功接口增加一个String skuType，参数，各位在查询的时候需要判断skuType
 * 增加-增加两处接口为Null的Log提示，tag为GoogleBillingUtil。
 *
 * V1.1.4 2018/01/03
 * 修改-实现单例模式，避免多实例导致的谷歌接口回调错乱问题。
 *
 * V1.1.3 2017/12/19
 * 修复-服务启动失败时导致的空指针错误。
 *
 * V1.1.2    2017/12/18
 * 修复-修复内购未被消耗的BUG。
 * 增加-每次启动都获取一次历史内购订单，并且全部消耗。
 * 增加-可以通过设置isAutoConsumeAsync来确定内购是否每次自动消耗。
 * 增加-将consumeAsync改为public，你可以手动调用消耗。
 *
 * V1.1.1  2017/11/2
 * 升级-内购API版本为google最新版本。compile 'com.android.billingclient:billing:1.0'
 * 特性-不需要key了，不需要IInAppBillingService.aidl了，不需要那一大堆Utils了，创建新实例的时候必须要传入购买回调接口。
 *
 * V1.0.3 2017/10/27
 * 增加-支持内购
 *
 * V1.0.2  2017/09/11
 * 修复-修复BUG
 *
 * v1.0.1 2017/07/29
 * 初始版本
 */

/**
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
 *
 */


@SuppressWarnings("ALL")
public class GoogleBillingUtilOld {

    private static final String TAG = "GoogleBillingUtilOld";
    private static final boolean IS_DEBUG = false;
    private static String[] inAppSKUS = new String[]{};//内购ID
    private static String[] subsSKUS = new String[]{};//订阅ID

    public static final String BILLING_TYPE_INAPP = BillingClient.SkuType.INAPP;//内购
    public static final String BILLING_TYPE_SUBS = BillingClient.SkuType.SUBS;//订阅

    private static BillingClient mBillingClient;
    private static BillingClient.Builder builder ;
    private static OnPurchaseFinishedListener mOnPurchaseFinishedListener;
    private static OnStartSetupFinishedListener mOnStartSetupFinishedListener ;
    private static OnQueryFinishedListener mOnQueryFinishedListener;
    private static OnConsumeResponseListener mOnConsumeResponseListener;

    private boolean isAutoConsumeAsync = true;//是否在购买成功后自动消耗商品

    private static final GoogleBillingUtilOld mGoogleBillingUtil = new GoogleBillingUtilOld() ;

    private GoogleBillingUtilOld()
    {

    }

    /**
     * 设置skus
     * @param inAppSKUS 内购id
     * @param subsSKUS 订阅id
     */
    public static void setSkus(@Nullable String[] inAppSKUS,@Nullable String[] subsSKUS){
        if(inAppSKUS!=null){
            GoogleBillingUtilOld.inAppSKUS = Arrays.copyOf(inAppSKUS,inAppSKUS.length);
        }
        if(subsSKUS!=null){
            GoogleBillingUtilOld.subsSKUS = Arrays.copyOf(subsSKUS,subsSKUS.length);
        }
    }

    public static GoogleBillingUtilOld getInstance()
    {
        cleanListener();
        return mGoogleBillingUtil;
    }

    public GoogleBillingUtilOld build(Context context)
    {
        if(mBillingClient==null)
        {
            synchronized (mGoogleBillingUtil)
            {
                if(mBillingClient==null)
                {
                    builder = BillingClient.newBuilder(context);
                    mBillingClient = builder.setListener(mGoogleBillingUtil.new MyPurchasesUpdatedListener()).build();
                }
                else
                {
                    builder.setListener(mGoogleBillingUtil.new MyPurchasesUpdatedListener());
                }
            }
        }
        else
        {
            builder.setListener(mGoogleBillingUtil.new MyPurchasesUpdatedListener());
        }
        synchronized (mGoogleBillingUtil)
        {
            if(mGoogleBillingUtil.startConnection())
            {
                mGoogleBillingUtil.queryInventoryInApp();
                mGoogleBillingUtil.queryInventorySubs();
                mGoogleBillingUtil.queryPurchasesInApp();
            }
        }
        return mGoogleBillingUtil;
    }

    public boolean startConnection()
    {
        if(mBillingClient==null)
        {
            log("初始化失败:mBillingClient==null");
            return false;
        }
        if(!mBillingClient.isReady())
        {
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                    if (billingResponseCode == BillingClient.BillingResponse.OK) {
                        queryInventoryInApp();
                        queryInventorySubs();
                        queryPurchasesInApp();
                        if(mOnStartSetupFinishedListener!=null)
                        {
                            mOnStartSetupFinishedListener.onSetupSuccess();
                        }
                    }
                    else
                    {
                        log("初始化失败:onSetupFail:code="+billingResponseCode);
                        if(mOnStartSetupFinishedListener!=null)
                        {
                            mOnStartSetupFinishedListener.onSetupFail(billingResponseCode);
                        }
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    if(mOnStartSetupFinishedListener!=null)
                    {
                        mOnStartSetupFinishedListener.onSetupError();
                    }
                    log("初始化错误:onBillingServiceDisconnected");
                }
            });
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Google购买商品回调接口(订阅和内购都走这个接口)
     */
    private class MyPurchasesUpdatedListener implements PurchasesUpdatedListener
    {
        @Override
        public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> list) {
            if(mOnPurchaseFinishedListener==null)
            {
                if(IS_DEBUG)
                {
                    log("警告:接收到购买回调，但购买商品接口为Null，请设置购买接口。eg:setOnPurchaseFinishedListener()");
                }
                return ;
            }
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null)
            {
                boolean isAutoConsumeAsync = mOnPurchaseFinishedListener.onPurchaseSuccess(list);
                if(isAutoConsumeAsync){
                    //消耗商品
                    for(Purchase purchase:list)
                    {
                        String sku = purchase.getSku();
                        if(sku!=null)
                        {
                            String skuType = getSkuType(sku);
                            if(skuType!=null)
                            {
                                if(skuType.equals(BillingClient.SkuType.INAPP))
                                {
                                    consumeAsync(purchase.getPurchaseToken());
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                mOnPurchaseFinishedListener.onPurchaseFail(responseCode);
            }
        }
    }

    /**
     * 查询内购商品信息
     */
    public void queryInventoryInApp()
    {
        queryInventory(BillingClient.SkuType.INAPP);
    }

    /**
     * 查询订阅商品信息
     */
    public void queryInventorySubs()
    {
        queryInventory(BillingClient.SkuType.SUBS);
    }

    private void queryInventory(final String skuType) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBillingClient == null)
                {
                    if(mOnQueryFinishedListener!=null)
                    {
                        mOnQueryFinishedListener.onQueryError();
                    }
                    return ;
                }
                ArrayList<String> skuList = new ArrayList<>();
                if(skuType.equals(BillingClient.SkuType.INAPP))
                {
                    Collections.addAll(skuList, inAppSKUS);
                }
                else if(skuType.equals(BillingClient.SkuType.SUBS))
                {
                    Collections.addAll(skuList, subsSKUS);
                }
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(skuType);
                mBillingClient.querySkuDetailsAsync(params.build(),new MySkuDetailsResponseListener(skuType));
            }
        };
        executeServiceRequest(runnable);
    }

    /**
     * Google查询商品信息回调接口
     */
    private class MySkuDetailsResponseListener implements SkuDetailsResponseListener
    {
        private String skuType ;

        public MySkuDetailsResponseListener(String skuType) {
            this.skuType = skuType;
        }

        @Override
        public void onSkuDetailsResponse(int responseCode , List<SkuDetails> list) {
            if(mOnQueryFinishedListener==null)
            {
                if(IS_DEBUG) {
                    log("警告:接收到查询商品回调，但查询商品接口为Null，请设置购买接口。eg:setOnQueryFinishedListener()");
                }
                return ;
            }
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null)
            {
                mOnQueryFinishedListener.onQuerySuccess(skuType,list);
            }
            else
            {
                mOnQueryFinishedListener.onQueryFail(responseCode);
            }
        }

    }

    /**
     * 发起内购
     * @param skuId
     */
    public void purchaseInApp(Activity activity,String skuId)
    {
        String skuType = getSkuType(skuId);
        if(BillingClient.SkuType.INAPP.equals(skuType)){
            purchase(activity,skuId, BillingClient.SkuType.INAPP);
        }else{
            throw new RuntimeException(
                    "检测到该商品id{"+skuId+"}的类型与内购类型{"+BillingClient.SkuType.INAPP+"}不相同");
        }
    }

    /**
     * 发起订阅
     * @param skuId
     */
    public void purchaseSubs(Activity activity,String skuId)
    {
        String skuType = getSkuType(skuId);
        if(BillingClient.SkuType.SUBS.equals(skuType)){
            purchase(activity,skuId, BillingClient.SkuType.SUBS);
        }else{
            throw new RuntimeException(
                    "检测到该商品id{"+skuId+"}的类型与订阅类型{"+BillingClient.SkuType.SUBS+"}不相同");
        }
    }

    private void purchase(Activity activity,final String skuId,final String skuType)
    {
        if(mBillingClient==null)
        {
            if(mOnPurchaseFinishedListener!=null)
            {
                mOnPurchaseFinishedListener.onPurchaseError();
            }
            return ;
        }
        if(startConnection())
        {
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(skuType)
                    .build();
            mBillingClient.launchBillingFlow(activity,flowParams);
        }
       else
        {
            if(mOnPurchaseFinishedListener!=null)
            {
                mOnPurchaseFinishedListener.onPurchaseError();
            }
        }
    }

    /**
     * 消耗商品
     * @param purchaseToken
     */
    public void consumeAsync(String purchaseToken)
    {
        if(mBillingClient==null)
        {
            return ;
        }
        mBillingClient.consumeAsync(purchaseToken, new MyConsumeResponseListener(mOnConsumeResponseListener));
    }

    /**
     * 消耗内购商品-通过sku数组
     * @param sku
     */
    public void consumeAsyncInApp(@NonNull String... sku)
    {
        if(mBillingClient==null) {
            return ;
        }
        List<String> skuList = Arrays.asList(sku);
        consumeAsyncInApp(skuList);
    }

    /**
     * 消耗内购商品-通过sku数组
     * @param skuList
     */
    public void consumeAsyncInApp(@NonNull List<String> skuList)
    {
        if(mBillingClient==null) {
            return ;
        }
        List<Purchase> purchaseList = queryPurchasesInApp();
        if(purchaseList!=null){
            for(Purchase purchase : purchaseList){
                if(skuList.contains(purchase.getSku())){
                    mBillingClient.consumeAsync(purchase.getPurchaseToken(), new MyConsumeResponseListener(mOnConsumeResponseListener));
                }
            }
        }
    }

    /**
     * Googlg消耗商品回调
     */
    private class MyConsumeResponseListener implements ConsumeResponseListener {
        private OnConsumeResponseListener onConsumeResponseListener;

        public MyConsumeResponseListener(OnConsumeResponseListener mOnConsumeResponseListener) {
            this.onConsumeResponseListener = mOnConsumeResponseListener;
        }

        @Override
        public void onConsumeResponse(int responseCode, String purchaseToken) {
            if (responseCode == BillingClient.BillingResponse.OK) {
                if(onConsumeResponseListener!=null){
                    onConsumeResponseListener.onConsumeSuccess(purchaseToken);
                }
            }else{
                if(onConsumeResponseListener!=null){
                    onConsumeResponseListener.onConsumeFail(responseCode);
                }
            }
        }
    }

    /**
     * 获取已经内购的商品
     */
    public List<Purchase> queryPurchasesInApp()
    {
        return queryPurchases(BillingClient.SkuType.INAPP);
    }

    /**
     * 获取已经订阅的商品
     * @return 商品列表
     */
    public List<Purchase> queryPurchasesSubs()
    {
        return queryPurchases(BillingClient.SkuType.SUBS);
    }

    private List<Purchase> queryPurchases(String skuType)
    {
        if(mBillingClient==null)
        {
            return null;
        }
        if(!mBillingClient.isReady())
        {
            startConnection();
        }
        else
        {
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(skuType);
            if(purchasesResult!=null)
            {
                if(purchasesResult.getResponseCode()== BillingClient.BillingResponse.OK)
                {
                    List<Purchase> purchaseList =  purchasesResult.getPurchasesList();
                    if(purchaseList!=null)
                    {
                        if(mOnPurchaseFinishedListener!=null){
                            boolean isAutoConsumeAsync = mOnPurchaseFinishedListener.onRecheck(skuType,purchaseList);
                            if(isAutoConsumeAsync){
                                for(Purchase purchase:purchaseList)
                                {
                                    if(skuType.equals(BillingClient.SkuType.INAPP))
                                    {
                                        consumeAsync(purchase.getPurchaseToken());
                                    }
                                }
                            }
                        }
                    }
                    return purchaseList;
                }
            }

        }
        return null;
    }

    /**
     * 异步联网查询所有的内购历史-无论是过期的、取消、等等的订单
     * @param listener
     */
    public void queryPurchaseHistoryAsyncInApp(PurchaseHistoryResponseListener listener){
        if(mBillingClient!=null) {
            if(mBillingClient.isReady()){
                mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,listener);
            }else{
                listener.onPurchaseHistoryResponse(-1,null);
            }
        } else{
            listener.onPurchaseHistoryResponse(-1,null);
        }
    }

    /**
     * 异步联网查询所有的订阅历史-无论是过期的、取消、等等的订单
     * @param listener
     */
    public void queryPurchaseHistoryAsyncSubs(PurchaseHistoryResponseListener listener){
        if(mBillingClient!=null) {
            if(mBillingClient.isReady()){
                mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS,listener);
            }else{
                listener.onPurchaseHistoryResponse(-1,null);
            }
        }else{
            listener.onPurchaseHistoryResponse(-1,null);
        }
    }


    /**
     * 获取有效订阅的数量
     * @return -1查询失败，0没有有效订阅，>0具有有效的订阅
     */
    public int getPurchasesSizeSubs()
    {
        List<Purchase> list = queryPurchasesSubs();
        if(list!=null)
        {
            return list.size();
        }
        return -1;
    }

    /**
     * 通过sku获取订阅商品序号
     * @param sku
     * @return 序号
     */
    public int getSubsPositionBySku(String sku)
    {
        return getPositionBySku(sku, BillingClient.SkuType.SUBS);
    }

    /**
     * 通过sku获取内购商品序号
     * @param sku
     * @return 成功返回需要 失败返回-1
     */
    public int getInAppPositionBySku(String sku)
    {
        return getPositionBySku(sku, BillingClient.SkuType.INAPP);
    }

    private int getPositionBySku(String sku,String skuType)
    {

        if(skuType.equals(BillingClient.SkuType.INAPP))
        {
            int i = 0;
            for(String s:inAppSKUS)
            {
                if(s.equals(sku))
                {
                    return i;
                }
                i++;
            }
        }
        else if(skuType.equals(BillingClient.SkuType.SUBS))
        {
            int i = 0;
            for(String s:subsSKUS)
            {
                if(s.equals(sku))
                {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    private void executeServiceRequest(final Runnable runnable)
    {
        if(startConnection())
        {
            runnable.run();
        }
    }

    /**
     * 通过序号获取订阅sku
     * @param position
     * @return sku
     */
    public String getSubsSkuByPosition(int position)
    {
        if(position>=0&&position<subsSKUS.length)
        {
            return subsSKUS[position];
        }
        else {
            return null;
        }
    }

    /**
     * 通过序号获取内购sku
     * @param position
     * @return sku
     */
    public String getInAppSkuByPosition(int position)
    {
        if(position>=0&&position<inAppSKUS.length)
        {
            return inAppSKUS[position];
        }
        else
        {
            return null;
        }
    }

    /**
     * 通过sku获取商品类型(订阅获取内购)
     * @param sku
     * @return inapp内购，subs订阅
     */
    public String getSkuType(String sku)
    {
        if(Arrays.asList(inAppSKUS).contains(sku))
        {
            return BillingClient.SkuType.INAPP;
        }
        else if(Arrays.asList(subsSKUS).contains(sku))
        {
            return BillingClient.SkuType.SUBS;
        }
        return null;
    }


    public GoogleBillingUtilOld setOnQueryFinishedListener(OnQueryFinishedListener onQueryFinishedListener) {
        mOnQueryFinishedListener = onQueryFinishedListener;
        return mGoogleBillingUtil;
    }

    public GoogleBillingUtilOld setOnPurchaseFinishedListener(OnPurchaseFinishedListener onPurchaseFinishedListener) {
        mOnPurchaseFinishedListener = onPurchaseFinishedListener;
        return mGoogleBillingUtil;
    }

    public OnStartSetupFinishedListener getOnStartSetupFinishedListener() {
        return mOnStartSetupFinishedListener;
    }

    public GoogleBillingUtilOld setOnStartSetupFinishedListener(OnStartSetupFinishedListener onStartSetupFinishedListener) {
        mOnStartSetupFinishedListener = onStartSetupFinishedListener;
        return mGoogleBillingUtil;
    }

    public static OnConsumeResponseListener getmOnConsumeResponseListener() {
        return mOnConsumeResponseListener;
    }

    public GoogleBillingUtilOld setOnConsumeResponseListener(OnConsumeResponseListener onConsumeResponseListener) {
        mOnConsumeResponseListener = onConsumeResponseListener;
        return mGoogleBillingUtil;
    }

    /**
     *  本工具查询回调接口
     */
    public interface OnQueryFinishedListener{
        //Inapp和sub都走这个接口,查询的时候一定要判断skuType
        public void onQuerySuccess(String skuType, List<SkuDetails> list);
        public void onQueryFail(int responseCode);
        public void onQueryError();
    }

    /**
     * 本工具购买回调接口(内购与订阅都走这接口)
     */
    public interface OnPurchaseFinishedListener{

        public boolean onPurchaseSuccess(List<Purchase> list);

        public boolean onRecheck(String skuType,List<Purchase> list);

        public void onPurchaseFail(int responseCode);

        public void onPurchaseError();

    }

    /**
     * google服务启动接口
     */
    public interface OnStartSetupFinishedListener{
        public void onSetupSuccess();

        public void onSetupFail(int responseCode);

        public void onSetupError();
    }

    /**
     * 消耗回调监听器
     */
    public interface OnConsumeResponseListener{
        public void onConsumeSuccess(String purchaseToken);
        public void onConsumeFail(int responseCode);
    }

    public boolean isReady() {
        return mBillingClient!=null&&mBillingClient.isReady();
    }

    public boolean isAutoConsumeAsync()
    {
        return isAutoConsumeAsync;
    }

    public void setIsAutoConsumeAsync(boolean isAutoConsumeAsync)
    {
        this.isAutoConsumeAsync= isAutoConsumeAsync;
    }

    /**
     * 清除所有监听器，防止内存泄漏
     * 如果有多个页面使用了支付，需要确保上个页面的cleanListener在下一个页面的GoogleBillingUtil.getInstance()前使用。
     * 所以不建议放在onDestory里调用
     */
    public static void cleanListener()
    {
        mOnPurchaseFinishedListener = null;
        mOnQueryFinishedListener = null;
        mOnStartSetupFinishedListener = null;
        mOnConsumeResponseListener = null;
        if(builder!=null)
        {
            builder.setListener(null);
        }
    }

    /**
     * 断开连接google服务
     * 注意！！！一般情况不建议调用该方法，让google保留连接是最好的选择。
     */
    public static void endConnection()
    {
        //注意！！！一般情况不建议调用该方法，让google保留连接是最好的选择。
        if(mBillingClient!=null)
        {
            if(mBillingClient.isReady())
            {
                mBillingClient.endConnection();
                mBillingClient = null;
            }
        }
    }

    private static void log(String msg)
    {
        if(IS_DEBUG)
        {
            Log.e(TAG,msg);
        }
    }
}
