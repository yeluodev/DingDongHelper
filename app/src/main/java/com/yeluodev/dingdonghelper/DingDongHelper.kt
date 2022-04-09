package com.yeluodev.dingdonghelper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

/**
 * TODO
 * 1、应用退出后如何重新启动
 * 2、规避频繁点击购物车 done
 * 3、购物车未刷出商品列表时如何自动下拉刷新 done
 * 4、确认订单页如何规避退出场景
 */
class DingDongHelper : AccessibilityService() {

    companion object {
        const val TAG = "DingDongService"
        const val HOME_ACTIVITY = "com.yaya.zone.home.HomeActivity"
        const val WRITE_ORDER_ACTIVITY = "cn.me.android.cart.activity.WriteOrderActivity"

        //叮咚送达时间半窗
        const val CHOOSE_DELIVERY_TIME = "gy"
        const val CHOOSE_DELIVERY_TIME_v2 = "iy"

        //重新加载和返回购物车弹窗
        const val GX0 = "gx0"
        const val DY = "dy"

        //下单失败，送达时间已抢光
        const val XV0 = "xv0"

        //余额支付密码输入半窗
        const val U90 = "u90"

        //叮咚加载动画
        const val XN1 = "xn1"

        const val RETURN_CART_DIALOG = "by"
    }

    var currentClassName: String = ""
    var chooseTimeSuccess: Boolean = false
    var enableJumpCart: Boolean = true
    var checkNotificationCount: Int = 0
    var enableAutoRefresh = true
    var job: Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            //过滤非叮咚买菜app相关的事件
            val packageName = event.packageName
            if (packageName?.equals("com.yaya.zone") == false) {
                return
            }
            handleClassName(event)
            when (currentClassName) {
                HOME_ACTIVITY -> jumpToCart(event)
                WRITE_ORDER_ACTIVITY -> pay(event)
                CHOOSE_DELIVERY_TIME -> chooseDeliveryTime(event)
                CHOOSE_DELIVERY_TIME_v2 -> chooseDeliveryTime(event)
                GX0 -> performGlobalAction(GLOBAL_ACTION_BACK)
                DY -> clickReturnCartBtn(event)
                XV0 -> clickReturnCartBtn(event)
                RETURN_CART_DIALOG -> clickReturnCartBtn(event)
                else -> clickDialog(event)
            }
        }
    }

    /**
     * 处理事件，得到触发事件的类名
     */
    private fun handleClassName(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        currentClassName = event.className as String
        Log.d(TAG, "currentClassName: $currentClassName")
        if (currentClassName in listOf(HOME_ACTIVITY, WRITE_ORDER_ACTIVITY, CHOOSE_DELIVERY_TIME)) {
            enableJumpCart = true
        }
        if (currentClassName == HOME_ACTIVITY) {
            checkNotificationCount = 0
        }
    }

    /**
     * 确认订单页数据加载失败弹窗，点击返回购物车页面
     */
    private fun clickReturnCartBtn(event: AccessibilityEvent) {
        val nodes = event.source?.findAccessibilityNodeInfosByText("返回购物车")
        nodes?.forEach { node ->
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "yeluo 点击返回购物车")
            return@forEach
        }
    }

    /**
     * TODO 未理解该方法用处
     */
    private fun checkNotification(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            if (checkNotificationCount++ > 1) {
                Log.d(TAG, "checkNotificationCount: $checkNotificationCount, return")
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } else {
            checkNotificationCount = 0
        }
    }

    /**
     * 确认订单页，点击立即支付
     */
    private fun pay(event: AccessibilityEvent) {
        val nodes = event.source?.findAccessibilityNodeInfosByText("立即支付")
        nodes?.forEach { node ->
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "yeluo 点击立即支付")
        }
    }

    /**
     * 选择送达时间半窗
     */
    private fun chooseDeliveryTime(event: AccessibilityEvent) {
        Log.d(TAG, "chooseDeliveryTime: ${event.source}")
        val nodes =
            event.source?.findAccessibilityNodeInfosByViewId("com.yaya.zone:id/cl_item_select_hour_root")
        chooseTimeSuccess = false
        nodes?.forEach { node ->
            Log.d(
                TAG,
                "chooseDeliveryTime: ${node.getChild(0)?.text} isEnabled = ${node?.isEnabled}"
            )
            if (node?.isEnabled == true) {
                chooseTimeSuccess = true
                Log.d(TAG, "yeluo 选择送达时间：${node.getChild(0)?.text}")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return@forEach
            }
        }
        if (!chooseTimeSuccess) {
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
            Log.d(TAG, "送达时间选择失败: $event")
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d(TAG, "yeluo 关闭选择送达时间半窗")
            GlobalScope.launch {
                delay(100)
                Log.d(TAG, "yeluo 延时100ms后，检查当前状态")
                if (currentClassName == CHOOSE_DELIVERY_TIME) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d(TAG, "yeluo 兜底检查，若半窗还未关闭，再次点击返回关闭半窗")
                }
            }
        }
    }


    /**
     * 如果打开在首页，自动跳转购物车tab
     */
    private fun jumpToCart(event: AccessibilityEvent?) {
        event?.let {
            Log.d(TAG, "yeluo 开始查找底部导航的购物车按钮")
            //查找底部导航购物车按钮
            val nodes =
                event.source?.findAccessibilityNodeInfosByViewId("com.yaya.zone:id/rl_car_layout")
            nodes?.forEach {
                //若当前已在购物车tab，则尝试查找去结算按钮，点击跳转确认订单页
                if (currentClassName == HOME_ACTIVITY && it.getChild(0)?.isSelected == true) {
                    Log.d(TAG, "yeluo 当前已选中购物车页面")
                    jumpToCreateOrder(event)
                    return
                }
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "yeluo 点击底部导航切换至购物车页面")
            }
        }
    }

    /**
     * 跳转确认订单页面
     */
    private fun jumpToCreateOrder(event: AccessibilityEvent) {
        if (!enableJumpCart) {
            Log.d(TAG, "enableJumpCart: $enableJumpCart, return")
            return
        }
        val nodes =
            event.source?.findAccessibilityNodeInfosByViewId("com.yaya.zone:id/btn_submit")
        var goodsLoaded = false
        nodes?.forEach {
            goodsLoaded = true
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            enableJumpCart = false
            Log.d(TAG, "yeluo 点击去结算，跳转确认订单页面")
            return@forEach
        }
        if (!goodsLoaded) {
            Log.e(TAG, "yeluo 判断enableScroll = $enableAutoRefresh")
            if (!enableAutoRefresh) return
            val cartListNode =
                event.source.findAccessibilityNodeInfosByViewId("com.yaya.zone:id/list_good_car")
            cartListNode?.forEach { _ ->
                enableAutoRefresh = false
                Log.d(TAG, "yeluo 滚动页面")
                autoRefreshCart()
            }
        }
    }

    /**
     * 自动下拉刷新购物车商品列表
     */
    private fun autoRefreshCart() {
        job?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(540f, 1000f) //滑动起点
            path.lineTo(540f, 1600f) //滑动终点
            val builder = GestureDescription.Builder()
            val description = builder.addStroke(StrokeDescription(path, 100L, 100L)).build()
            //100L 第一个是开始的时间，第二个是持续时间
            dispatchGesture(description, object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    resetEnableScrollFlag()
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    resetEnableScrollFlag()
                }
            }, null)
        }
    }

    /**
     * 延时重置下拉刷新标记
     */
    private fun resetEnableScrollFlag() {
        job = GlobalScope.launch {
            delay(500)
            enableAutoRefresh = true
            Log.e(TAG, "yeluo 延时设置可滚动enableScroll = $enableAutoRefresh")
        }
    }

    /**
     * 选择继续支付or修改送达时间
     */
    private fun clickDialog(event: AccessibilityEvent) {
        var nodes = event.source?.findAccessibilityNodeInfosByText("继续支付")
        if (nodes == null) {
            nodes = event.source?.findAccessibilityNodeInfosByText("修改送达时间")
        }
        nodes?.forEach { node ->
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e(TAG, "yeluo 订单中出现商品售罄情况，选择继续支付or修改送达时间")
            return@forEach
        }
    }

    override fun onInterrupt() {
        println("onInterrupt")
    }

}

 


