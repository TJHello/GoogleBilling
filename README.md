# GoogleBilling 3.0.0.1-a01

#### 基于com.android.billingclient:billing:3.0.0，对整个支付流程进行封装。
##### QQ群交流：425219113(计算机语言交流)

**提醒：从2021年8月2日开始，所有新应用都必须使用Billing Library版本3或更高版本。到2021年11月1日，对现有应用程序的所有更新都必须使用Billing Library版本3或更高版本。**

##### [1.2.2.21入口](2021年8月2日弃用)(https://gitee.com/tjbaobao/GoogleBilling/tree/master/)

**自荐一款全平台广告聚合SDK自动集成框架【ADEasy】:[https://blog.csdn.net/u013640004/article/details/105416193](https://blog.csdn.net/u013640004/article/details/105416193)**

##### 3.0.0新特性[(官方说明)](https://developer.android.com/google/play/billing/release-notes)

- 删除了奖励的SKU支持。
- 删除了ChildDirected和UnderAgeOfConsent参数。
- 删除了不推荐使用的开发人员有效负载方法。
- 删除了不推荐使用的方法BillingFlowParams.setAccountId() 和BillingFlowParams.setDeveloperId()。
- 删除了不推荐使用的方法BillingFlowParams.setOldSkus(String oldSku) 和BillingFlowParams.addOldSku(String oldSku)。
- 添加了可空性注释。
- 其他bug修复


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
        //如不可用，则
        //maven { url 'https://tjhello.gitee.io/publiclib/'}
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
    implementation 'com.TJHello:GoogleBilling:3.0.0.1-a01'
}


```


- 代码示例 [MainActivity](https://gitee.com/tjbaobao/GoogleBilling/blob/master/app/src/main/java/com/tjbaobao/gitee/googlebillingutil/MainActivity.kt)

```kotlin
    private lateinit var googleBillingUtil: GoogleBillingUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GoogleBillingUtil.isDebug(true)
        //设置商品id，如没有，则传入null，可全局灵活配合查询商品进行使用。
        GoogleBillingUtil.setSkus(arrayOf("inappSku"), arrayOf("subsSku"))
        googleBillingUtil = GoogleBillingUtil.getInstance()
            .addOnGoogleBillingListener(this,OnGoogleBillingListener())
            .build(this)
    }
    
    /**
     * 使用了JAVA8特性，可以选择性实现自己想要的方法。
     */
    private inner class OnGoogleBillingListener : OnGoogleBillingListener(){
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
public boolean queryPurchaseHistoryAsyncInApp(Activity activity)//查询历史内购订单
public boolean queryPurchaseHistoryAsyncSubs(Activity activity)//查询历史订阅订单

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

setSkus的时候将内购sku和订阅sku的参数顺序弄错了，应该是第一个是内购的，第二个参数是订阅的。如果不是这个问题，请debug源码看sku是否设置正常。

---
### =================API说明=================

3.0.0版本新增(无)
