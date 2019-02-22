package com.tjbaobao.gitee.builling;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.billingclient.api.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作者:TJbaobao
 * 时间:2019/1/5  15:16
 * 说明:
 * 使用：
 */
@SuppressWarnings("ALL")
public class GoogleBillingUtil {

    private static final String TAG = "GoogleBillingUtil";
    private static final boolean IS_DEBUG = false;
    private static String[] inAppSKUS = new String[]{};//内购ID,必填，注意！如果用不着的请去掉多余的""
    private static String[] subsSKUS = new String[]{};//订阅ID,必填，注意！如果用不着的请去掉多余的""

    public static final String BILLING_TYPE_INAPP = BillingClient.SkuType.INAPP;//内购
    public static final String BILLING_TYPE_SUBS = BillingClient.SkuType.SUBS;//订阅

    private static BillingClient mBillingClient;
    private static BillingClient.Builder builder ;
    private static List<OnPurchaseFinishedListener> mOnPurchaseFinishedListenerList = new ArrayList<>();
    private static List<OnStartSetupFinishedListener> mOnStartSetupFinishedListenerList = new ArrayList<>();
    private static List<OnQueryFinishedListener> mOnQueryFinishedListenerList = new ArrayList<>();
    private static List<OnConsumeResponseListener> mOnConsumeResponseListenerList = new ArrayList<>();
    private MyPurchasesUpdatedListener purchasesUpdatedListener = new MyPurchasesUpdatedListener();

    private boolean isAutoConsumeAsync = true;//是否在购买成功后自动消耗商品

    private static final GoogleBillingUtil mGoogleBillingUtil = new GoogleBillingUtil() ;

    private GoogleBillingUtil()
    {

    }

    /**
     * 设置skus
     * @param inAppSKUS 内购id
     * @param subsSKUS 订阅id
     */
    public static void setSkus(@Nullable String[] inAppSKUS,@Nullable String[] subsSKUS){
        if(inAppSKUS!=null){
            GoogleBillingUtil.inAppSKUS = inAppSKUS;
        }
        if(subsSKUS!=null){
            GoogleBillingUtil.subsSKUS = subsSKUS;
        }
    }

    public static GoogleBillingUtil getInstance()
    {
        return mGoogleBillingUtil;
    }

    /**
     * 开始建立内购连接
     * @param context applicationContext
     * @return
     */
    public GoogleBillingUtil build(Context context)
    {
        if(mBillingClient==null)
        {
            synchronized (mGoogleBillingUtil)
            {
                if(mBillingClient==null)
                {
                    if(isGooglePlayServicesAvailable(context))
                    {
                        builder = BillingClient.newBuilder(context);
                        mBillingClient = builder.build();
                    }
                    else
                    {
                        if(IS_DEBUG)
                        {
                            log("警告:GooglePlay服务处于不可用状态，请检查");
                        }
                        for(OnStartSetupFinishedListener listener:mOnStartSetupFinishedListenerList){
                            listener.onSetupError();
                        }
                    }
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
                        for(OnStartSetupFinishedListener listener:mOnStartSetupFinishedListenerList){
                            listener.onSetupSuccess();
                        }
                    }
                    else
                    {
                        log("初始化失败:onSetupFail:code="+billingResponseCode);
                        for(OnStartSetupFinishedListener listener:mOnStartSetupFinishedListenerList){
                            listener.onSetupFail(billingResponseCode);
                        }
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    for(OnStartSetupFinishedListener listener:mOnStartSetupFinishedListenerList){
                        listener.onSetupError();
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

    /**
     * Google购买商品回调接口(订阅和内购都走这个接口)
     */
    private class MyPurchasesUpdatedListener implements PurchasesUpdatedListener
    {
        @Override
        public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> list) {
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null)
            {
                if(isAutoConsumeAsync)
                {
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
                for(OnPurchaseFinishedListener listener:mOnPurchaseFinishedListenerList){
                    listener.onPurchaseSuccess(list);
                }
            }
            else
            {
                for(OnPurchaseFinishedListener listener:mOnPurchaseFinishedListenerList){
                    listener.onPurchaseFail(responseCode);
                }
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
                    for(OnQueryFinishedListener listener:mOnQueryFinishedListenerList){
                        listener.onQueryError();
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
            if(responseCode== BillingClient.BillingResponse.OK&&list!=null)
            {
                for(OnQueryFinishedListener listener:mOnQueryFinishedListenerList){
                    listener.onQuerySuccess(skuType,list);
                }
            }
            else
            {
                for(OnQueryFinishedListener listener:mOnQueryFinishedListenerList){
                    listener.onQueryFail(responseCode);
                }
            }
        }

    }

    /**
     * 发起内购
     * @param skuId
     * @return
     */
    public void purchaseInApp(Activity activity, String skuId)
    {
        purchase(activity,skuId, BillingClient.SkuType.INAPP);
    }

    /**
     * 发起订阅
     * @param skuId
     * @return
     */
    public void purchaseSubs(Activity activity,String skuId)
    {
        purchase(activity,skuId, BillingClient.SkuType.SUBS);
    }

    private void purchase(Activity activity,final String skuId,final String skuType)
    {
        if(mBillingClient==null)
        {
            for(OnPurchaseFinishedListener listener:mOnPurchaseFinishedListenerList){
                listener.onPurchaseError();
            }
            return ;
        }
        if(startConnection())
        {
            builder.setListener(purchasesUpdatedListener);
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(skuType)
                    .build();
            mBillingClient.launchBillingFlow(activity,flowParams);
        }
        else
        {
            for(OnPurchaseFinishedListener listener:mOnPurchaseFinishedListenerList){
                listener.onPurchaseError();
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
        mBillingClient.consumeAsync(purchaseToken, new MyConsumeResponseListener());
    }

    /**
     * Googlg消耗商品回调
     */
    private class MyConsumeResponseListener implements ConsumeResponseListener
    {
        @Override
        public void onConsumeResponse(int responseCode, String purchaseToken) {
            if (responseCode == BillingClient.BillingResponse.OK) {
                for(OnConsumeResponseListener listener:mOnConsumeResponseListenerList){
                    listener.onConsumeSuccess(purchaseToken);
                }
            }else{
                for(OnConsumeResponseListener listener:mOnConsumeResponseListenerList){
                    listener.onConsumeFail(responseCode);
                }
            }
        }
    }

    /**
     * 获取已经内购的商品
     * @return
     */
    public List<Purchase> queryPurchasesInApp()
    {
        return queryPurchases(BillingClient.SkuType.INAPP);
    }

    /**
     * 获取已经订阅的商品
     * @return
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
                    if(isAutoConsumeAsync)
                    {
                        if(purchaseList!=null)
                        {
                            for(Purchase purchase:purchaseList)
                            {
                                if(skuType.equals(BillingClient.SkuType.INAPP))
                                {
                                    consumeAsync(purchase.getPurchaseToken());
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
     * @return
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
     * @return
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
     * @return
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

    /**
     * 检测GooglePlay服务是否可用(需要导入包api "com.google.android.gms:play-services-location:11.8.0"，也可以不检查，跳过这个代码)
     * @param context
     * @return
     */
    public static boolean isGooglePlayServicesAvailable(Context context)
    {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        if(googleApiAvailability!=null)
        {
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
            return resultCode == ConnectionResult.SUCCESS;
        }
        return false;
        //return true;//不检查直接跳过
    }

    /**
     * 设置查询结果监听器{@link #addOnQueryFinishedListener}
     * @param onQueryFinishedListener
     * @return
     */
    @Deprecated
    public GoogleBillingUtil setOnQueryFinishedListener(OnQueryFinishedListener onQueryFinishedListener) {
        mOnQueryFinishedListenerList.clear();
        return addOnQueryFinishedListener(onQueryFinishedListener);
    }

    /**
     * 设置购买结果监听器{@link #addOnPurchaseFinishedListener}
     * @param onPurchaseFinishedListener
     * @return
     */
    @Deprecated
    public GoogleBillingUtil setOnPurchaseFinishedListener(OnPurchaseFinishedListener onPurchaseFinishedListener) {
        mOnPurchaseFinishedListenerList.clear();
        return addOnPurchaseFinishedListener(onPurchaseFinishedListener);
    }

    /**
     * 设置内购服务启动结果监听器{@link #addOnStartSetupFinishedListener}
     * @param onStartSetupFinishedListener
     * @return
     */
    @Deprecated
    public GoogleBillingUtil setOnStartSetupFinishedListener(OnStartSetupFinishedListener onStartSetupFinishedListener) {
        mOnStartSetupFinishedListenerList.clear();
        return addOnStartSetupFinishedListener(onStartSetupFinishedListener);
    }

    /**
     * 设置消耗结果监听器{@link #addmOnConsumeResponseListener}
     * @param onConsumeResponseListener
     * @return
     */
    @Deprecated
    public GoogleBillingUtil setOnConsumeResponseListener(OnConsumeResponseListener onConsumeResponseListener) {
        mOnConsumeResponseListenerList.clear();
        return addOnConsumeResponseListener(onConsumeResponseListener);
    }

    /**
     * 添加一个查询结果监听器
     * @param onQueryFinishedListener
     * @return
     */
    public GoogleBillingUtil addOnQueryFinishedListener(@NonNull OnQueryFinishedListener onQueryFinishedListener){
        if(!mOnQueryFinishedListenerList.contains(onQueryFinishedListener))
        {
            mOnQueryFinishedListenerList.add(onQueryFinishedListener);
        }
        return mGoogleBillingUtil;
    }

    /**
     * 添加一个购买结果监听器
     * @param onQueryFinishedListener
     * @return
     */
    public GoogleBillingUtil addOnPurchaseFinishedListener(@NonNull OnPurchaseFinishedListener onPurchaseFinishedListener) {
        if(!mOnPurchaseFinishedListenerList.contains(onPurchaseFinishedListener))
        {
            mOnPurchaseFinishedListenerList.add(onPurchaseFinishedListener);
        }
        return mGoogleBillingUtil;
    }

    /**
     * 添加一个内购服务启动结果监听器
     * @param onQueryFinishedListener
     * @return
     */
    public GoogleBillingUtil addOnStartSetupFinishedListener(@NonNull OnStartSetupFinishedListener onStartSetupFinishedListener) {
        if(!mOnStartSetupFinishedListenerList.contains(onStartSetupFinishedListener))
        {
            mOnStartSetupFinishedListenerList.add(onStartSetupFinishedListener);
        }
        return mGoogleBillingUtil;
    }

    /**
     * 添加一个消耗结果监听器
     * @param onConsumeResponseListener
     * @return
     */
    public GoogleBillingUtil addOnConsumeResponseListener(@NonNull OnConsumeResponseListener onConsumeResponseListener) {
        if(!mOnConsumeResponseListenerList.contains(onConsumeResponseListener)){
            mOnConsumeResponseListenerList.add(onConsumeResponseListener);
        }
        return mGoogleBillingUtil;
    }

    public static void removeOnQueryFinishedListener(@NonNull OnQueryFinishedListener onQueryFinishedListener){
        mOnQueryFinishedListenerList.remove(onQueryFinishedListener);
    }

    public static void removeOnPurchaseFinishedListener(@NonNull OnPurchaseFinishedListener onPurchaseFinishedListener){
        mOnPurchaseFinishedListenerList.remove(onPurchaseFinishedListener);
    }

    public static void removeOnStartSetupFinishedListener(@NonNull OnStartSetupFinishedListener onStartSetupFinishedListener){
        mOnStartSetupFinishedListenerList.remove(onStartSetupFinishedListener);
    }

    public static void removeOnConsumeResponseListener(@NonNull OnConsumeResponseListener onConsumeResponseListener){
        mOnConsumeResponseListenerList.remove(onConsumeResponseListener);
    }

    public static void removeAllOnQueryFinishedListener(){
        mOnQueryFinishedListenerList.clear();
    }

    public static void removeAllOnPurchaseFinishedListener(){
        mOnPurchaseFinishedListenerList.clear();
    }

    public static void removeAllOnStartSetupFinishedListener(){
        mOnStartSetupFinishedListenerList.clear();
    }

    public static void removeAllOnConsumeResponseListener(){
        mOnConsumeResponseListenerList.clear();
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

        public void onPurchaseSuccess(List<Purchase> list);

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
        mOnPurchaseFinishedListenerList.clear();
        mOnQueryFinishedListenerList.clear();
        mOnStartSetupFinishedListenerList.clear();
        mOnConsumeResponseListenerList.clear();

    }

    /**
     * 清除内购监听器，防止内存泄漏
     */
    public static void onDestroy(){
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