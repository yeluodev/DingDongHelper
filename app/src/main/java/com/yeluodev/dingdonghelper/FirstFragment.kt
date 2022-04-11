package com.yeluodev.dingdonghelper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.yeluodev.dingdonghelper.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            openAccessibility(it)
        }
        binding.btnShowFloatBall.setOnClickListener {
            showFloatBall()
        }
    }

    /**
     * 展示悬浮球
     */
    private fun showFloatBall() {
        EasyFloat.with(requireActivity().applicationContext)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_SIDE)
            .setImmersionStatusBar(true)
            .setGravity(Gravity.END, -20, 100)
            .setLayout(R.layout.layout_float) {
                it.findViewById<FrameLayout>(R.id.fl).setOnClickListener {
                    showFloatToolbox()
                    Toast.makeText(context, "展示悬浮工具箱", Toast.LENGTH_SHORT).show()
                }
                it.findViewById<FrameLayout>(R.id.fl).setOnLongClickListener {
                    //长按取消悬浮球
                    EasyFloat.dismiss()
                    true
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openAccessibility(view: View?) {
        try {
            val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(accessibleIntent)
        } catch (e: Exception) {
            view?.let {
                Snackbar.make(view, "跳转失败，请前往系统设置>无障碍服务>叮咚助手", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }
    }

    private fun showFloatToolbox() {
        EasyFloat.with(requireActivity().applicationContext)
            .setTag("TOOLBOX")
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_SIDE)
            .setImmersionStatusBar(true)
            .setGravity(Gravity.END, -20, 100)
            .setLayout(R.layout.layout_float_toolbox) {
                it.findViewById<FrameLayout>(R.id.flClose).setOnClickListener {
                    EasyFloat.dismiss("TOOLBOX")
                }
                it.findViewById<SwitchCompat>(R.id.switchStatus)
                    .setOnCheckedChangeListener { buttonView, isChecked ->
                        DingDongHelper.SCRIPT_RUNNING_STATUS = isChecked
                        DingDongHelper.enableAutoRefresh = true
                    }
                it.findViewById<RadioGroup>(R.id.radioGroup)
                    .setOnCheckedChangeListener { buttonView, isChecked ->
                        when (isChecked) {
                            R.id.rbAutoRefresh -> DingDongHelper.scriptMode =
                                DingDongHelper.SCRIPT_MODE_REFRESH
                            else -> DingDongHelper.scriptMode =
                                DingDongHelper.SCRIPT_MODE_CREATE_ORDER
                        }
                    }
            }
            .show()
    }
}