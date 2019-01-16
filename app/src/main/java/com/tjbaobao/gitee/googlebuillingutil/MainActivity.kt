package com.tjbaobao.gitee.googlebuillingutil

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.billingclient.api.SkuDetails
import com.tjbaobao.gitee.builling.GoogleBillingUtil
import com.tjbaobao.gitee.builling.GoogleBillingUtilOld

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GoogleBillingUtilOld.getInstance()
            .setOnStartSetupFinishedListener(object : GoogleBillingUtilOld.OnStartSetupFinishedListener{
            override fun onSetupSuccess() {
                log("onSetupSuccess")
            }

            override fun onSetupFail(responseCode: Int) {
                log("onSetupFail=$responseCode")
            }

            override fun onSetupError() {
                log("onSetupError")
            }
        }).setOnQueryFinishedListener(object : GoogleBillingUtilOld.OnQueryFinishedListener{

                override fun onQuerySuccess(skuType: String?, list: MutableList<SkuDetails>?) {
                    log("onQuerySuccess")
                }

                override fun onQueryFail(responseCode: Int) {
                    log("onQueryFail=$responseCode")
                }

                override fun onQueryError() {
                    log("onQueryError")
                }

            })
            .build(this.applicationContext)

    }

    private fun log(msg:String){
        Log.i("MyDebug",msg)
    }
}
