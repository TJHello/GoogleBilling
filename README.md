# GoogleBilling 2.0.0 [![](https://jitpack.io/v/com.gitee.tjbaobao/GoogleBilling.svg)](https://jitpack.io/#com.gitee.tjbaobao/GoogleBilling)

#### 基于com.android.billingclient:billing:2.0.0，对整个支付流程进行封装。
##### QQ群交流：425219113(计算机语言交流)

#### 2.0.0新特性[官方说明](http://https://developer.android.com/google/play/billing/billing_library_releases_notes?hl=zh-cn)

- 增加"确认购买"概念，每个新购买的商品都需要调用acknowledgePurchase方法来进行确认购买，如果没有进行确认购买，三天后会遭受系统自动退款。
- 强制删除BillingFlowParams.setSku（）方法，改为使用BillingClient.querySkuDetailsAsync()+BillingFlowParams.Builder.setSkuDetails()来配置发起购买的参数。
- 消耗商品和确认购买配置添加setDeveloperPayload()方法，可以将一个字符串传递给google，然后可以在查询商品记录里面获得该值。（目前没在发起购买里看到这个方法，具体试用场景还不是很明确）
- 增加"待交易"概念，购买商品回调里面，我们需要通过getPurchaseState方法来判断商品当前的状态。
- 其他一些api修改，问题不大。不过一些开发者测试之类的功能应该很有用，但我没有具体去看。


<img src="https://images.gitee.com/uploads/images/2019/0602/144013_b5f39e6b_927162.png"  width="216" height="384">
<img src="https://images.gitee.com/uploads/images/2019/0602/144525_18b8420b_927162.png"  width="216" height="384">

---
### =================使用方法=================
- API接入

```groovy
//Project
allprojects {
      repositories {
  	    ...
  	    maven { url 'https://jitpack.io' }
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
    implementation 'com.gitee.tjbaobao:GoogleBilling:2.0.0-alpha03'
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
    private inner class OnGoogleBillingListener : GoogleBillingUtil.OnGoogleBillingListener{
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
### =================API说明=================

2.0.0版本新增

- public static void setIsAutoAcknowledgePurchase(boolean isAutoAcknowledgePurchase) //设置是否自动确认购买
- public void acknowledgePurchase(Activity activity,String purchaseToken)//确认购买
