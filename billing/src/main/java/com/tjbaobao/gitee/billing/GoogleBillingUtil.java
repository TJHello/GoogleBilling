package com.tjbaobao.gitee.billing;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.billingclient.api.*;

import java.util.*;

/**
 * 作者:天镜baobao
 * 时间:2019/1/5  15:16
 * 说明:欢迎使用，但请遵循Apache License 2.0
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
 *
 */

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class GoogleBillingUtil {

    private static final String TAG = "GoogleBillingUtil";
    private static boolean IS_DEBUG = false;
    private static String[] inAppSKUS = new String[]{};//内购ID,必填，注意！如果用不着的请去掉多余的""
    private static String[] subsSKUS = new String[]{};//订阅ID,必填，注意！如果用不着的请去掉多余的""

    public static final String BILLING_TYPE_INAPP = BillingClient.SkuType.INAPP;//内购
    public static final String BILLING_TYPE_SUBS = BillingClient.SkuType.SUBS;//订阅

    private static BillingClient mBillingClient;
    private static BillingClient.Builder builder ;
    private static List<OnGoogleBillingListener> onGoogleBillingListenerList = new ArrayList<>();
    private static Map<String,OnGoogleBillingListener> onGoogleBillingListenerMap = new HashMap<>();
    private MyPurchasesUpdatedListener purchasesUpdatedListener = new MyPurchasesUpdatedListener();

    private static final GoogleBillingUtil mGoogleBillingUtil = new GoogleBillingUtil() ;

    private GoogleBillingUtil()
    {

    }

    //region===================================初始化google应用内购买服务=================================

    /**
     * 设置skus
     * @param inAppSKUS 内购id
     * @param subsSKUS 订阅id
     */
    public static void setSkus(@Nullable String[] inAppSKUS,@Nullable String[] subsSKUS){
        if(inAppSKUS!=null){
            GoogleBillingUtil.inAppSKUS = Arrays.copyOf(inAppSKUS,inAppSKUS.length);
        }
        if(subsSKUS!=null){
            GoogleBillingUtil.subsSKUS = Arrays.copyOf(subsSKUS,subsSKUS.length);
        }
    }

    private static <T> void copyToArray(T[] base,T[] target){
        System.arraycopy(base, 0, target, 0, base.length);
    }

    public static GoogleBillingUtil getInstance()
    {
        return mGoogleBillingUtil;
    }

    /**
     * 开始建立内购连接
     * @param activity activity
     */
    public GoogleBillingUtil build(Activity activity)
    {
        purchasesUpdatedListener.tag = getTag(activity);
        if(mBillingClient==null)
        {
            synchronized (mGoogleBillingUtil)
            {
                if(mBillingClient==null)
                {
                    builder = BillingClient.newBuilder(activity);
                    mBillingClient = builder.setListener(purchasesUpdatedListener).build();
                }
                else
                {
                    builder.setListener(purchasesUpdatedListener);
                }
            }
        }
        else
        {
            builder.setListener(purchasesUpdatedListener);
        }
        synchronized (mGoogleBillingUtil)
        {
            if(mGoogleBillingUtil.startConnection(activity))
            {
                mGoogleBillingUtil.queryInventoryInApp(getTag(activity));
                mGoogleBillingUtil.queryInventorySubs(getTag(activity));
                mGoogleBillingUtil.queryPurchasesInApp(getTag(activity));
            }
        }
        return mGoogleBillingUtil;
    }

    public boolean startConnection(Activity activity){
        return startConnection(getTag(activity));
    }

    private boolean startConnection(String tag)
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
                        queryInventoryInApp(tag);
                        queryInventorySubs(tag);
                        queryPurchasesInApp(tag);
                        for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                            listener.onSetupSuccess(listener.tag.equals(tag));
                        }
                    }
                    else
                    {
                        log("初始化失败:onSetupFail:code="+billingResponseCode);
                        for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                            listener.onFail(GoogleBillingListenerTag.SETUP,billingResponseCode, listener.tag.equals(tag));
                        }
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                        listener.onBillingServiceDisconnected();
                    }
                    log("初始化失败:onBillingServiceDisconnected");
                }
            });
            return false;
        }
        else
        {
            return true;
        }
    }

    //endregion

    //region===================================查询商品=================================

    /**
     * 查询内购商品信息
     */
    public void queryInventoryInApp(Activity activity)
    {
        queryInventoryInApp(getTag(activity));
    }

    private void queryInventoryInApp(String tag)
    {
        queryInventory(tag,BillingClient.SkuType.INAPP);
    }

    /**
     * 查询订阅商品信息
     */
    public void queryInventorySubs(Activity activity)
    {
        queryInventory(getTag(activity),BillingClient.SkuType.SUBS);
    }

    private void queryInventorySubs(String tag)
    {
        queryInventory(tag,BillingClient.SkuType.SUBS);
    }

    private void queryInventory(String tag,final String skuType) {
        Runnable runnable = () -> {
            if (mBillingClient == null)
            {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onError(GoogleBillingListenerTag.QUERY, listener.tag.equals(tag));
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
            if(!skuList.isEmpty()){
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(skuType);
                mBillingClient.querySkuDetailsAsync(params.build(),new MySkuDetailsResponseListener(skuType,tag));
            }
        };
        executeServiceRequest(tag,runnable);
    }
    
    //endregion

    //region===================================购买商品=================================
    /**
     * 发起内购
     * @param skuId 内购商品id
     */
    public void purchaseInApp(Activity activity, String skuId)
    {
        String skuType = getSkuType(skuId);
        if(BillingClient.SkuType.INAPP.equals(skuType)){
            purchase(activity,skuId, BillingClient.SkuType.INAPP);
        }else{
            throw new IllegalArgumentException(
                    "检测到该商品id{"+skuId+"}的类型与内购类型{"+BillingClient.SkuType.INAPP+"}不相同");
        }
    }

    /**
     * 发起订阅
     * @param skuId 订阅商品id
     */
    public void purchaseSubs(Activity activity,String skuId)
    {
        String skuType = getSkuType(skuId);
        if(BillingClient.SkuType.SUBS.equals(skuType)){
            purchase(activity,skuId, BillingClient.SkuType.SUBS);
        }else{
            throw new IllegalArgumentException(
                    "检测到该商品id{"+skuId+"}的类型与订阅类型{"+BillingClient.SkuType.SUBS+"}不相同");
        }
    }

    private void purchase(Activity activity,final String skuId,final String skuType)
    {
        String tag = getTag(activity);
        if(mBillingClient==null)
        {
            for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                listener.onError(GoogleBillingListenerTag.PURCHASE, listener.tag.equals(tag));
            }
            return ;
        }
        if(startConnection(tag))
        {
            purchasesUpdatedListener.tag = tag;
            builder.setListener(purchasesUpdatedListener);
            List<String> skuList = new ArrayList<>();
            skuList.add(skuId);
            SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(skuList)
                    .setType(skuType)
                    .build();
            mBillingClient.querySkuDetailsAsync(skuDetailsParams, (responseCode, skuDetailsList) -> {
                if(skuDetailsList!=null&&!skuDetailsList.isEmpty()){
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList.get(0))
                            .build();
                    mBillingClient.launchBillingFlow(activity,flowParams);
                }
            });
        }
        else
        {
            for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                listener.onError(GoogleBillingListenerTag.PURCHASE,listener.tag.equals(tag));
            }
        }
    }


    //endregion

    //region===================================消耗商品=================================
    /**
     * 消耗商品
     * @param purchaseToken {@link Purchase#getPurchaseToken()}
     */
    public void consumeAsync(Activity activity,String purchaseToken)
    {
        consumeAsync(getTag(activity),purchaseToken);
    }

    /**
     * 消耗商品
     * @param purchaseToken {@link Purchase#getPurchaseToken()}
     */
    private void consumeAsync(String tag,String purchaseToken)
    {
        if(mBillingClient==null)
        {
            return ;
        }
        mBillingClient.consumeAsync(purchaseToken, new MyConsumeResponseListener(tag));
    }

    /**
     * 消耗内购商品-通过sku数组
     * @param sku sku
     */
    public void consumeAsyncInApp(Activity activity,@NonNull String... sku)
    {
        if(mBillingClient==null) {
            return ;
        }
        List<String> skuList = Arrays.asList(sku);
        consumeAsyncInApp(activity,skuList);
    }

    /**
     * 消耗内购商品-通过sku数组
     * @param skuList sku数组
     */
    public void consumeAsyncInApp(Activity activity,@NonNull List<String> skuList)
    {
        if(mBillingClient==null) {
            return ;
        }
        List<Purchase> purchaseList = queryPurchasesInApp(activity);
        if(purchaseList!=null){
            for(Purchase purchase : purchaseList){
                if(skuList.contains(purchase.getSku())){
                    mBillingClient.consumeAsync(purchase.getPurchaseToken(), new MyConsumeResponseListener(getTag(activity)));
                }
            }
        }
    }

    //endregion    

    //region===================================本地订单查询=================================
    /**
     * 获取已经内购的商品
     * @return 商品列表
     */
    public List<Purchase> queryPurchasesInApp(Activity activity)
    {
        return queryPurchases(getTag(activity),BillingClient.SkuType.INAPP);
    }

    private List<Purchase> queryPurchasesInApp(String tag)
    {
        return queryPurchases(tag,BillingClient.SkuType.INAPP);
    }

    /**
     * 获取已经订阅的商品
     * @return 商品列表
     */
    public List<Purchase> queryPurchasesSubs(Activity activity)
    {
        return queryPurchases(getTag(activity),BillingClient.SkuType.SUBS);
    }

    private List<Purchase> queryPurchases(String tag,String skuType)
    {
        if(mBillingClient==null)
        {
            return null;
        }
        if(!mBillingClient.isReady())
        {
            startConnection(tag);
        }
        else
        {
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(skuType);
            if(purchasesResult!=null)
            {
                if(purchasesResult.getResponseCode()== BillingClient.BillingResponse.OK)
                {
                    List<Purchase> purchaseList =  purchasesResult.getPurchasesList();
                    if(purchaseList!=null&&!purchaseList.isEmpty())
                    {
                        for(OnGoogleBillingListener listener : onGoogleBillingListenerList) {
                            boolean isSelf = listener.tag.equals(tag);//是否是当前页面
                            for(Purchase purchase:purchaseList)
                            {
                                boolean isSuccess = listener.onRecheck(skuType, purchase, isSelf);
                                if (isSuccess && isSelf) {
                                    if(skuType.equals(BillingClient.SkuType.INAPP))
                                    {
                                        consumeAsync(tag,purchase.getPurchaseToken());
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
    //endregion

    //region===================================在线订单查询=================================
    /**
     * 异步联网查询所有的内购历史-无论是过期的、取消、等等的订单
     * @param activity activity
     */
    public void queryPurchaseHistoryAsyncInApp(Activity activity){
        queryPurchaseHistoryAsync(getTag(activity),BILLING_TYPE_INAPP);
    }

    /**
     * 异步联网查询所有的订阅历史-无论是过期的、取消、等等的订单
     * @param activity activity
     */
    public void queryPurchaseHistoryAsyncSubs(Activity activity){
        queryPurchaseHistoryAsync(getTag(activity),BILLING_TYPE_SUBS);
    }

    private void queryPurchaseHistoryAsync(String tag,String type){
        if(isReady()) {
            mBillingClient.queryPurchaseHistoryAsync(type,new MyPurchaseHistoryResponseListener(tag));
        }
    }
    //endregion

    //region===================================工具集合=================================
    /**
     * 获取有效订阅的数量
     * @return -1查询失败，0没有有效订阅，>0具有有效的订阅
     */
    public int getPurchasesSizeSubs(Activity activity)
    {
        List<Purchase> list = queryPurchasesSubs(activity);
        if(list!=null)
        {
            return list.size();
        }
        return -1;
    }

    /**
     * 通过sku获取订阅商品序号
     * @param sku sku
     * @return 序号
     */
    public int getSubsPositionBySku(String sku)
    {
        return getPositionBySku(sku, BillingClient.SkuType.SUBS);
    }

    /**
     * 通过sku获取内购商品序号
     * @param sku sku
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

    /**
     * 通过序号获取订阅sku
     * @param position 序号
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
     * @param position 序号
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
     * @param sku sku
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

    private String getTag(Activity activity){
        return activity.getLocalClassName();
    }

    //endregion

    //region===================================其他方法=================================

    private void executeServiceRequest(String tag,final Runnable runnable) {
        if(startConnection(tag))
        {
            runnable.run();
        }
    }


    /**
     * google内购服务是否已经准备好
     * @return boolean
     */
    public static boolean isReady() {
        return mBillingClient!=null&&mBillingClient.isReady();
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

    //endregion

    public GoogleBillingUtil addOnGoogleBillingListener(Activity activity,OnGoogleBillingListener onGoogleBillingListener){
        String tag = getTag(activity);
        onGoogleBillingListener.tag = tag;
        onGoogleBillingListenerMap.put(getTag(activity),onGoogleBillingListener);
        for(int i=onGoogleBillingListenerList.size()-1;i>=0;i--){
            OnGoogleBillingListener listener = onGoogleBillingListenerList.get(i);
            if(listener.tag.equals(tag)){
                onGoogleBillingListenerList.remove(listener);
            }
        }
        onGoogleBillingListenerList.add(onGoogleBillingListener);
        return this;
    }

    public void removeOnGoogleBillingListener(OnGoogleBillingListener onGoogleBillingListener){
        onGoogleBillingListenerList.remove(onGoogleBillingListener);
    }

    public void removeOnGoogleBillingListener(Activity activity){
        String tag = getTag(activity);
        for(int i=onGoogleBillingListenerList.size()-1;i>=0;i--){
            OnGoogleBillingListener listener = onGoogleBillingListenerList.get(i);
            if(listener.tag.equals(tag)){
                removeOnGoogleBillingListener(listener);
                onGoogleBillingListenerMap.remove(tag);
            }
        }
    }

    /**
     * 清除内购监听器，防止内存泄漏-在Activity-onDestroy里面调用。
     * 需要确保onDestroy和build方法在同一个线程。
     */
    public void onDestroy(Activity activity){
        if(builder!=null) {
            builder.setListener(null);
        }
        removeOnGoogleBillingListener(activity);
    }

    /**
     * Google购买商品回调接口(订阅和内购都走这个接口)
     */
    private class MyPurchasesUpdatedListener implements PurchasesUpdatedListener
    {

        public String tag ;

        @Override
        public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> list) {
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null)
            {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    boolean isSelf = listener.tag.equals(tag);//是否是当前页面
                    listener.onPurchaseSuccess(list,isSelf);
                    for(Purchase purchase:list) {
                        boolean isSuccess = listener.onPurchaseSuccess(purchase,isSelf);//是否允许消耗
                        String sku = purchase.getSku();
                        if(isSuccess&&isSelf){
                            //消耗商品
                            String skuType = getSkuType(sku);
                            if(skuType!=null)
                            {
                                if(skuType.equals(BillingClient.SkuType.INAPP))
                                {
                                    consumeAsync(tag,purchase.getPurchaseToken());
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                if(IS_DEBUG){
                    log("购买失败,responseCode:"+responseCode);
                }
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.PURCHASE,responseCode,listener.tag.equals(tag));
                }
            }
        }
    }

    /**
     * Google查询商品信息回调接口
     */
    private class MySkuDetailsResponseListener implements SkuDetailsResponseListener
    {
        private String skuType ;
        private String tag;

        public MySkuDetailsResponseListener(String skuType,String tag) {
            this.skuType = skuType;
            this.tag = tag;
        }

        @Override
        public void onSkuDetailsResponse(int responseCode , List<SkuDetails> list) {
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null)
            {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onQuerySuccess(skuType,list,listener.tag.equals(tag));
                }
            }
            else
            {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.QUERY,responseCode,listener.tag.equals(tag));
                }
            }
        }

    }

    /**
     * Googlg消耗商品回调
     */
    private class MyConsumeResponseListener implements ConsumeResponseListener
    {
        private String tag ;

        public MyConsumeResponseListener(String tag) {
            this.tag = tag;
        }

        @Override
        public void onConsumeResponse(int responseCode, String purchaseToken) {
            if (responseCode == BillingClient.BillingResponse.OK) {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onConsumeSuccess(purchaseToken,listener.tag.equals(tag));
                }
            }else{
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.COMSUME,responseCode,listener.tag.equals(tag));
                }
            }
        }
    }

    private class MyPurchaseHistoryResponseListener implements PurchaseHistoryResponseListener{

        private String tag ;
        public MyPurchaseHistoryResponseListener(String tag) {
            this.tag = tag;
        }

        @Override
        public void onPurchaseHistoryResponse(int responseCode, List<Purchase> list) {
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null){
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    for (Purchase purchase : list) {
                        listener.onQueryHistory(purchase);
                    }
                }
            }else{
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.HISTORY,responseCode,listener.tag.equals(tag));
                }
            }
        }
    }

    public enum GoogleBillingListenerTag{

        QUERY("query"),
        PURCHASE("purchase"),
        SETUP("setup"),
        COMSUME("comsume"),
        HISTORY("history")
        ;
        public String tag ;
        GoogleBillingListenerTag(String tag){
            this.tag = tag;
        }
    }

    private static void log(String msg)
    {
        if(IS_DEBUG)
        {
            Log.e(TAG,msg);
        }
    }

    public static void isDebug(boolean isDebug){
        GoogleBillingUtil.IS_DEBUG = isDebug;
    }
}
