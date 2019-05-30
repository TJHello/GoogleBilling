package com.tjbaobao.gitee.googlebillingutil

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.tjbaobao.gitee.billing.GoogleBillingUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var googleBillingUtil: GoogleBillingUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GoogleBillingUtil.isDebug(true)
        GoogleBillingUtil.setSkus(arrayOf("love_poly_tips"), arrayOf())
        googleBillingUtil = GoogleBillingUtil.getInstance()
            .addOnGoogleBillingListener(this,OnGoogleBillingListener())
            .build(this)
        btNext.setOnClickListener {
            NextActivity.toActivity(this)
        }
    }

    /**
     * 使用了JAVA8特性，可以选择性实现自己想要的方法。
     */
    private inner class OnGoogleBillingListener : GoogleBillingUtil.OnGoogleBillingListener{
        //内购服务初始化成功
        override fun onSetupSuccess() {
            checkSubs()
        }
    }

    /**
     * 检查是否有有效订阅
     */
    private fun checkSubs(){
        when(googleBillingUtil.purchasesSizeSubs){
            0->{
                //不具备有效订阅
                Toast.makeText(this@MainActivity,"该用户不具备有效订阅", Toast.LENGTH_LONG).show()
            }
            -1->{
                //查询失败
                Toast.makeText(this@MainActivity,"查询有效订阅失败", Toast.LENGTH_LONG).show()
            }
            else->{
                //具有有效订阅
                Toast.makeText(this@MainActivity,"该用户具有有效订阅", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        googleBillingUtil.onDestroy(this)
        //退出程序的时候可以调用(实验性)
        GoogleBillingUtil.endConnection()
    }

    private fun log(msg:String){
        Log.i("MyDebug",msg)
    }
}
