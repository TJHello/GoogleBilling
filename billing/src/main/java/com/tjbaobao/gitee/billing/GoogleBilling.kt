package com.tjbaobao.gitee.billing

import android.app.Activity

/**
 * 作者:TJHello
 * 时间:2021/2/25  11:41
 */
class GoogleBilling private constructor(){

    companion object {

        private val mInappSKUList = mutableListOf<String>()
        private val mSubSKUList = mutableListOf<String>()

        private val mGoogleBilling : GoogleBilling = GoogleBilling()

        @JvmStatic
        fun initSku(inappSkuList:MutableList<String>,subSkuList:MutableList<String>){
            mInappSKUList.clear()
            mSubSKUList.clear()
        }

        @JvmStatic
        fun getInstance():GoogleBilling{
            return mGoogleBilling
        }

    }

    fun build(activity: Activity){

    }

}