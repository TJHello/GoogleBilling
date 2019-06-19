package com.tjbaobao.gitee.billing;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.billingclient.api.*;

import java.util.*;

/**
 * 作者:TJbaobao
 * 时间:2019/1/5  15:16
 * 说明:
 * 使用：
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

    private static boolean isAutoConsumeAsync = true;//是否在购买成功后自动消耗商品
    private static boolean isAutoAcknowledgePurchase = true;//是否在购买成功后自动消耗商品

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
                    mBillingClient = builder.setListener(purchasesUpdatedListener)
                            .enablePendingPurchases()
                            .build();
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
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        queryInventoryInApp(tag);
                        queryInventorySubs(tag);
                        queryPurchasesInApp(tag);
                        for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                            listener.onSetupSuccess(listener.tag.equals(tag));
                        }
                    }
                    else
                    {
                        log("初始化失败:onSetupFail:code="+billingResult.getResponseCode());
                        for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                            listener.onFail(GoogleBillingListenerTag.SETUP,billingResult.getResponseCode(), listener.tag.equals(tag));
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

    public void queryInventorySubs(String tag)
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
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(skuType);
            mBillingClient.querySkuDetailsAsync(params.build(),new MySkuDetailsResponseListener(skuType,tag));
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
        purchase(activity,skuId, BillingClient.SkuType.INAPP);
    }

    /**
     * 发起订阅
     * @param skuId 订阅商品id
     */
    public void purchaseSubs(Activity activity,String skuId)
    {
        purchase(activity,skuId, BillingClient.SkuType.SUBS);
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
            builder.setListener(purchasesUpdatedListener);
            List<String> skuList = new ArrayList<>();
            skuList.add(skuId);
            SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(skuList)
                    .setType(skuType)
                    .build();
            mBillingClient.querySkuDetailsAsync(skuDetailsParams, (responseCode, skuDetailsList) -> {
                if(!skuDetailsList.isEmpty()){
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
        consumeAsync(getTag(activity),purchaseToken,null);
    }


    public void consumeAsync(Activity activity,String purchaseToken,@Nullable String developerPayload)
    {
        consumeAsync(getTag(activity),purchaseToken,developerPayload);
    }

    /**
     * 消耗商品
     * @param purchaseToken {@link Purchase#getPurchaseToken()}
     */
    private void consumeAsync(String tag,String purchaseToken){
        consumeAsync(tag,purchaseToken,null);
    }


    private void consumeAsync(String tag,String purchaseToken,@Nullable String developerPayload)
    {
        if(mBillingClient==null)
        {
            return ;
        }
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .setDeveloperPayload(developerPayload)
                .build();
        mBillingClient.consumeAsync(consumeParams,new MyConsumeResponseListener(tag));
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
        consumeAsyncInApp(activity,skuList,null);
    }

    /**
     * 消耗内购商品-通过sku数组
     * @param skuList sku数组
     */
    public void consumeAsyncInApp(Activity activity,@NonNull List<String> skuList,@Nullable List<String> developerPayloadList)
    {
        if(mBillingClient==null) {
            return ;
        }
        List<Purchase> purchaseList = queryPurchasesInApp(activity);
        if(purchaseList!=null){
            for(Purchase purchase : purchaseList){
                int index = skuList.indexOf(purchase.getSku());
                if(index!=-1){
                    if(developerPayloadList!=null&&index<developerPayloadList.size()){
                        consumeAsync(activity,purchase.getPurchaseToken(),developerPayloadList.get(index));
                    }else{
                        consumeAsync(activity,purchase.getPurchaseToken(),null);
                    }
                }
            }
        }
    }

    //endregion

    //region===================================确认购买=================================

    /**
     * 确认购买
     * @param activity activity
     * @param purchaseToken token
     */
    public void acknowledgePurchase(Activity activity,String purchaseToken){
        acknowledgePurchase(activity,purchaseToken,null);
    }

    public void acknowledgePurchase(Activity activity,String purchaseToken,@Nullable String developerPayload){
        acknowledgePurchase(getTag(activity),purchaseToken,developerPayload);
    }

    private void acknowledgePurchase(String tag,String purchaseToken){
        acknowledgePurchase(tag,purchaseToken,null);
    }

    private void acknowledgePurchase(String tag,String purchaseToken,@Nullable String developerPayload){
        if(mBillingClient==null)
        {
            return ;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .setDeveloperPayload(developerPayload)
                .build();
        mBillingClient.acknowledgePurchase(params,new MyAcknowledgePurchaseResponseListener(tag));
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
                if(purchasesResult.getResponseCode()== BillingClient.BillingResponseCode.OK)
                {
                    List<Purchase> purchaseList =  purchasesResult.getPurchasesList();
                    if(purchaseList!=null)
                    {
                        for(Purchase purchase:purchaseList)
                        {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                if(skuType.equals(BillingClient.SkuType.INAPP))
                                {
                                    if(isAutoConsumeAsync) {
                                        consumeAsync(tag,purchase.getPurchaseToken());
                                    }else if(isAutoAcknowledgePurchase){
                                        if(!purchase.isAcknowledged()){
                                            acknowledgePurchase(tag,purchase.getPurchaseToken());
                                        }
                                    }
                                }else if(skuType.equals(BillingClient.SkuType.SUBS)){
                                    if(isAutoAcknowledgePurchase){
                                        if(!purchase.isAcknowledged()){
                                            acknowledgePurchase(tag,purchase.getPurchaseToken());
                                        }
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
     * @param listener 监听器
     * @return 返回false的时候说明网络出错
     */
    public boolean queryPurchaseHistoryAsyncInApp(PurchaseHistoryResponseListener listener){
        if(isReady()) {
            mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,listener);
            return true;
        }
        return false;
    }

    /**
     * 异步联网查询所有的订阅历史-无论是过期的、取消、等等的订单
     * @param listener 监听器
     * @return 返回false的时候说明网络出错
     */
    public boolean queryPurchaseHistoryAsyncSubs(PurchaseHistoryResponseListener listener){
        if(isReady()) {
            mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS,listener);
            return true;
        }else{
            return false;
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
     * 设置是否自动消耗内购商品
     * @param isAutoConsumeAsync 自动消耗内购商品
     */
    public static void setIsAutoConsumeAsync(boolean isAutoConsumeAsync) {
        GoogleBillingUtil.isAutoConsumeAsync= isAutoConsumeAsync;
    }

    /**
     * 设置是否自动确认购买
     * @param isAutoAcknowledgePurchase boolean
     */
    public static void setIsAutoAcknowledgePurchase(boolean isAutoAcknowledgePurchase){
        GoogleBillingUtil.isAutoAcknowledgePurchase = isAutoAcknowledgePurchase;
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
        for(OnGoogleBillingListener listener : onGoogleBillingListenerList ){
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
        public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
            if(billingResult.getResponseCode()== BillingClient.BillingResponseCode.OK&&list!=null)
            {
                //消耗商品
                for(Purchase purchase:list)
                {
                    if(purchase.getPurchaseState()== Purchase.PurchaseState.PURCHASED){
                        //已购买
                        String sku = purchase.getSku();
                        String skuType = getSkuType(sku);
                        if(skuType!=null)
                        {
                            if(skuType.equals(BillingClient.SkuType.INAPP))
                            {
                                if(isAutoConsumeAsync) {
                                    consumeAsync(tag,purchase.getPurchaseToken());
                                }else if(isAutoAcknowledgePurchase){
                                    if(!purchase.isAcknowledged()){
                                        acknowledgePurchase(tag,purchase.getPurchaseToken());
                                    }
                                }
                            }
                            else if(skuType.equals(BillingClient.SkuType.SUBS))
                            {
                                if(isAutoAcknowledgePurchase){
                                    if(!purchase.isAcknowledged()){
                                        acknowledgePurchase(tag,purchase.getPurchaseToken());
                                    }
                                }
                            }
                        }
                    }
                }
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onPurchaseSuccess(list,listener.tag.equals(tag));
                }
            }
            else
            {
                if(IS_DEBUG){
                    log("购买失败,responseCode:"+billingResult.getResponseCode()+",msg:"+billingResult.getDebugMessage());
                }
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.PURCHASE,billingResult.getResponseCode(),listener.tag.equals(tag));
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
        public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
            if(billingResult.getResponseCode()== BillingClient.BillingResponseCode.OK&&list!=null)
            {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onQuerySuccess(skuType,list,listener.tag.equals(tag));
                }
            }
            else
            {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.QUERY,billingResult.getResponseCode(),listener.tag.equals(tag));
                }
                if(IS_DEBUG){
                    log("查询失败,responseCode:"+billingResult.getResponseCode()+",msg:"+billingResult.getDebugMessage());
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
        public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onConsumeSuccess(purchaseToken,listener.tag.equals(tag));
                }
            }else{
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.COMSUME,billingResult.getResponseCode(),listener.tag.equals(tag));
                }
                if(IS_DEBUG){
                    log("消耗失败,responseCode:"+billingResult.getResponseCode()+",msg:"+billingResult.getDebugMessage());
                }
            }
        }
    }

    /**
     * Googlg消耗商品回调
     */
    private class MyAcknowledgePurchaseResponseListener implements AcknowledgePurchaseResponseListener{

        private String tag ;

        public MyAcknowledgePurchaseResponseListener(String tag) {
            this.tag = tag;
        }

        @Override
        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onAcknowledgePurchaseSuccess(listener.tag.equals(tag));
                }
            }else{
                for(OnGoogleBillingListener listener:onGoogleBillingListenerList){
                    listener.onFail(GoogleBillingListenerTag.AcKnowledgePurchase,billingResult.getResponseCode(),listener.tag.equals(tag));
                }
                if(IS_DEBUG){
                    log("确认购买失败,responseCode:"+billingResult.getResponseCode()+",msg:"+billingResult.getDebugMessage());
                }
            }
        }
    }

    public enum GoogleBillingListenerTag{

        QUERY("query"),
        PURCHASE("purchase"),
        SETUP("setup"),
        COMSUME("comsume"),
        AcKnowledgePurchase("AcKnowledgePurchase"),
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
