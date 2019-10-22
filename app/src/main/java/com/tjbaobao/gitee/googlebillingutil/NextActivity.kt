package com.tjbaobao.gitee.googlebillingutil

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.tjbaobao.gitee.billing.GoogleBillingUtil
import com.tjbaobao.gitee.billing.OnGoogleBillingListener
import kotlinx.android.synthetic.main.next_activity_layout.*


/**
 * 作者:TJbaobao
 * 时间:2019/3/13  11:24
 * 说明:
 * 使用：
 */
class NextActivity : AppCompatActivity() {


    private lateinit var googleBillingUtil: GoogleBillingUtil

    companion object {
        fun toActivity(activity: AppCompatActivity){
            val intent = Intent(activity,NextActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.next_activity_layout)
        googleBillingUtil = GoogleBillingUtil.getInstance()
            .addOnGoogleBillingListener(this, OnMyGoogleBillingListener())
            .build(this)

        //region============================设置按钮点击监听============================
        tvBack.setOnClickListener {
            this.finish()
        }
        btInapp.setOnClickListener {
            googleBillingUtil.purchaseInApp(this@NextActivity,googleBillingUtil.getInAppSkuByPosition(0))
        }
        btSubs.setOnClickListener {
            googleBillingUtil.purchaseSubs(this@NextActivity,googleBillingUtil.getSubsSkuByPosition(0))
        }
        //endregion
    }

    private inner class OnMyGoogleBillingListener : OnGoogleBillingListener(){

        override fun onQuerySuccess(skuType: String, list: MutableList<SkuDetails>,isSelf:Boolean) {
            if(skuType==GoogleBillingUtil.BILLING_TYPE_INAPP){
                //内购商品
                if(list.size>0){
                    btInapp.text = String.format("发起内购:%s",list[0].price)
                }
            }else if(skuType==GoogleBillingUtil.BILLING_TYPE_SUBS){
                //订阅商品
                if(list.size>0){
                    btSubs.text = String.format("发起订阅:%s",list[0].price)
                }
            }
        }

        override fun onPurchaseSuccess(purchase: Purchase,isSelf:Boolean) {
            val sku = purchase.sku
            val skuType = googleBillingUtil.getSkuType(sku)
            if(skuType==GoogleBillingUtil.BILLING_TYPE_INAPP){
                Toast.makeText(this@NextActivity,"内购成功:$sku",Toast.LENGTH_LONG).show()
            }else if(skuType==GoogleBillingUtil.BILLING_TYPE_SUBS){
                Toast.makeText(this@NextActivity,"订阅成功:$sku",Toast.LENGTH_LONG).show()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        googleBillingUtil.onDestroy(this)
    }

}