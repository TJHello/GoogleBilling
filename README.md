# GoogleBilling 1.2.2.13 [![](https://jitpack.io/v/TJHello/GoogleBilling.svg)](https://jitpack.io/#TJHello/GoogleBilling)

#### 基于com.android.billingclient:billing:1.2.2，对整个支付流程进行封装。
##### QQ群交流：425219113(计算机语言交流)

**提醒：从2021年8月2日开始，所有新应用都必须使用Billing Library版本3或更高版本。到2021年11月1日，对现有应用程序的所有更新都必须使用Billing Library版本3或更高版本。**

##### [最新版测试版3.0.0.1入口](https://github.com/TJHello/GoogleBilling/tree/3.0/) (2021年8月2日开始强制使用)
##### [最新版测试版2.0.3.10入口](https://github.com/TJHello/GoogleBilling/tree/2.0/)
##### [测试版1.2.2.22入口](https://github.com/TJHello/GoogleBilling/tree/1.2.2/)

**自荐一款全平台广告聚合SDK自动集成框架【ADEasy】:[https://blog.csdn.net/u013640004/article/details/105416193](https://blog.csdn.net/u013640004/article/details/105416193)**

**友情链接一款IOS内购封装【[DYFStoreKit](https://github.com/dgynfi/DYFStoreKit)】【[DYFStore](https://github.com/dgynfi/DYFStore)】**


**1.2.2.22改动**

- 重写自动消耗逻辑，改为每次购买成功，或者查询到未处理订单时，由使用者决定是否自动消耗。
- 增加onRecheck接口，返回未处理的有效订单
- 用内购id来发起订阅，或者用订阅id来发起内购时，会抛出异常提醒使用者。


<img src="https://gitee.com/tjbaobao/GoogleBilling/raw/master/images/home.webp"  width="216" height="384">
<img src="https://gitee.com/tjbaobao/GoogleBilling/raw/master/images/purchase.webp"  width="216" height="384">

---
### =================使用方法=================
- API接入

```groovy
//Project
allprojects {
      repositories {
  	    ...
  	    maven { url 'https://raw.githubusercontent.com/TJHello/publicLib/master'}
      }
  }
//app
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
dependencies {
     implementation 'com.TJHello:GoogleBilling:1.2.2.13'
}


```


- 代码示例 [MainActivity](https://gitee.com/tjbaobao/GoogleBilling/blob/master/app/src/main/java/com/tjbaobao/gitee/googlebillingutil/MainActivity.kt)

```kotlin
    private lateinit var googleBillingUtil: GoogleBillingUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GoogleBillingUtil.isDebug(true)
        GoogleBillingUtil.setSkus(arrayOf("inappSku"), arrayOf("subsSku"))
        googleBillingUtil = GoogleBillingUtil.getInstance()
            .addOnGoogleBillingListener(this,OnGoogleBillingListener())
            .build(this)
    }
    
    /**
     * 使用了JAVA8特性，可以选择性实现自己想要的方法。
     */
    private inner class OnGoogleBillingListener : GoogleBillingUtil.OnGoogleBillingListener(){
        //内购服务初始化成功
        override fun onSetupSuccess() {
            
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        googleBillingUtil.onDestroy(this)
    }
```

- 发起内购或者订阅


```java
public void queryInventoryInApp() //查询内购商品信息列表
public void queryInventorySubs() //查询订阅商品信息列表
public void purchaseInApp(Activity activity,String skuId) //发起内购
public void purchaseSubs(Activity activity,String skuId) //发起订阅
public List<Purchase> queryPurchasesInApp(Activity activity)//获取有效内购订单
public List<Purchase> queryPurchasesSubs(Activity activity)//获取有效订阅订单
public void queryPurchaseHistoryAsyncInApp(Activity activity)//查询历史内购订单
public void queryPurchaseHistoryAsyncSubs(Activity activity)//查询历史订阅订单
```

---
### =================响应码汇总([官方地址](https://developer.android.com/google/play/billing/billing_reference))=================

| 响应代码                                    | 值 | 说明                                                                                                                                 |
| ------------------------------------------- | -- | ------------------------------------------------------------------------------------------------------------------------------------ |
| BILLING_RESPONSE_RESULT_OK                  | 0  | 成功                                                                                                                                  |
| BILLING_RESPONSE_RESULT_USER_CANCELED       | 1  | 用户按上一步或取消对话框                                                                                                             |
| BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE | 2  | 网络连接断开                                                                                                                         |
| BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE | 3  | 所请求的类型不支持 Billing API 版本(支付环境问题)                                                                                         |
| BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE    | 4  | 请求的商品已不再出售。                                                                                                               |
| BILLING_RESPONSE_RESULT_DEVELOPER_ERROR     | 5  | 提供给 API 的参数无效。此错误也可能说明未在 Google Play 中针对应用内购买结算正确签署或设置应用，或者应用在其清单中不具备所需的权限。 |
| BILLING_RESPONSE_RESULT_ERROR               | 6  | API 操作期间出现严重错误                                                                                                             |
| BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED  | 7  | 未能购买，因为已经拥有此商品                                                                                                         |
| BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED      | 8  | 未能消费，因为尚未拥有此商品         

---
### =================常见问题=================

 **1. 初始化失败，错误码:3，这是支付环境问题。** 

有以下可能：用的是模拟器，三件套版本太旧，应用的支付环境没配置(接入谷歌服务，支付权限)，vpn地域不支持。

解决方法：a.先验证环境。在商店下载一个有内购的应用，看能否进行内购。b.如果别人的能进行内购之后，再次测试你的应用，看是否正常，来确认应用的支付环境是否正常。

 **2. 能够查询价格，但无法购买，提示"商品无法购买"之类。** 

这是基础配置问题，有以下可能：版本号与线上版本不对应，测试版本却不是测试账号(大概率)，签名不对应。

 **3. 能够查询价格，但无法调起支付都没有弹窗，错误码:3，报错：Error:In-app billing error: Null data in IAB activity resul。** 

原因是没有给Google play商店弹窗权限，国内很多手机都有弹窗权限管理，特别是小米，如果没允许，是不会有任何提示，并且拦截了的。(这个问题在新版的gp商店已经不存在）

 **4. 支付提示成功，但却走onQueryFail回调，并且返回的商品列表为null。** 

这是因为你调错了方法，记得purchaseInApp是内购的，purchaseSubs是订阅的。查询的时候同理。另外查询的时候报错，很有可能是你setSKUS的时候传了一个空字符串，而不是空数组。

 **5. 查询的时候返回的商品列表长度为0。** 

setSkus的时候将内购sku和订阅sku的参数顺序弄错了，应该是第一个是内购的，第二个参数是订阅的。

或者是商品还没有发布成功，需要等待一段时间(很有可能，新发布的商品是无论怎么查询还是购买，谷歌那边都是没有响应的)


 **6. 我们检测到您的应用使用的是旧版 Google Play Developer API。自 2019 年 12 月 1 日起，
 我们将不再支持此 API 的版本 1 和版本 2。请在该日期之前将您使用的 API 更新到版本 3。请注意，此变动与弃用 AIDL/结算库无关。**

 升级到com.android.billingclient:billing库，弃用AIDL相关代码。

 后台也要弃用v3的校验接口。具体见谷歌官方文档。


---
### =================API说明=================

1. 初始化google应用内购买服务

```java
//设置内购id和订阅id，用于自动查询等
public static void setSkus(@Nullable String[] inAppSKUS,@Nullable String[] subsSKUS)

//获取单例，全局通用
public static GoogleBillingUtil getInstance()

//初始化服务，建立连接，全局通用
public GoogleBillingUtil build

//建立连接，build中已经包含，用于特殊用户自定义重连机制
public boolean startConnection(Activity activity)

```

2. 查询商品

```
//查询内购商品信息(价格等信息)
public void queryInventoryInApp(Activity activity)

//查询订阅商品信息(价格等信息)
public void queryInventorySubs(Activity activity)

```

3. 购买商品

```

//发起内购
public void purchaseInApp(Activity activity, String skuId)

//发起订阅
public void purchaseSubs(Activity activity,String skuId)

```
4. 消耗商品

```
//消耗商品，通过purchaseToken
public void consumeAsync(Activity activity,String purchaseToken)

//消耗商品，通过sku数组
public void consumeAsyncInApp(Activity activity,@NonNull String... sku)

//消耗商品，通过sku列表
public void consumeAsyncInApp(Activity activity,@NonNull List<String> skuList)

```
5. 本地订单查询(查询GP本地缓存，不具备高实时性)

```
//取已经内购的商品
public List<Purchase> queryPurchasesInApp(Activity activity)

//获取已经订阅的商品
public List<Purchase> queryPurchasesSubs(Activity activity)

```
6. 在线订单查询(联网存，具备高实时性，但查到的是所有订单)

```
//异步联网查询所有的内购历史-无论是过期的、取消、等等的订单
public void queryPurchaseHistoryAsyncInApp(Activity activity)

//异步联网查询所有的订阅历史-无论是过期的、取消、等等的订单
public void queryPurchaseHistoryAsyncSubs(Activity activity)

```
7. 工具集合

```
//获取有效订阅的数量
public int getPurchasesSizeSubs(Activity activity)

//通过sku获取内购商品序号
public int getInAppPositionBySku(String sku)

//通过sku获取订阅商品序号
public int getSubsPositionBySku(String sku)

//通过序号获取订阅sku
public String getSubsSkuByPosition(int position)

//通过序号获取内购sku
public String getInAppSkuByPosition(int position)

//通过sku获取商品类型
public String getSkuType(String sku)

```
8. 其他方法

```
//google内购服务是否已经准备好
public static boolean isReady()

//设置是否自动消耗内购商品
public static void setIsAutoConsumeAsync(boolean isAutoConsumeAsync)

//断开连接google服务(不要频繁使用)
public static void endConnection()

```
9. 监听器相关

```
//添加监听器
public GoogleBillingUtil addOnGoogleBillingListener(Activity activity,OnGoogleBillingListener onGoogleBillingListener)

//移除监听器
public void removeOnGoogleBillingListener(OnGoogleBillingListener onGoogleBillingListener)

//移除某个页面的所有监听器
public void removeOnGoogleBillingListener(Activity activity)

//清除内购监听器，防止内存泄漏-在Activity-onDestroy里面调用。
public void onDestroy(Activity activity)

```