package com.example.dragtoactionlayoutdemo.utils;

import android.content.Context;

/**
 * 密度工具类
 */
public class DensityUtil {

    /**
     * 将dp类型的尺寸转换成px类型的尺寸
     */
    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }



}
