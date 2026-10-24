package com.beryu.tvhome

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

internal object InputRepository {
    fun read(context: Context, manager: TvInputManager?): List<InputEntry> {
        val inputs = manager?.tvInputList ?: return emptyList()
        val byId = inputs.associateBy { it.id }

        // Prefer physical HDMI ports. CEC children must not duplicate their parent.
        return inputs.filter { input ->
            input.type == TvInputInfo.TYPE_HDMI && input.isPassthroughInput &&
                !input.isHidden(context) && byId[input.parentId]?.type != TvInputInfo.TYPE_HDMI
        }.map { input ->
            InputEntry(input.id, InputRules.label(input.loadCustomLabel(context), input.loadLabel(context)))
        }.sortedWith(compareBy<InputEntry> { it.brand.ordinal }.thenBy { it.label }.thenBy { it.id })
    }
}
