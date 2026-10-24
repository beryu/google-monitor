package com.beryu.tvhome;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InputRepository {
    static List<InputEntry> read(Context context, TvInputManager manager) {
        List<InputEntry> entries = new ArrayList<>();
        if (manager == null) return entries;
        List<TvInputInfo> all = manager.getTvInputList();
        Map<String, TvInputInfo> byId = new HashMap<>();
        for (TvInputInfo info : all) byId.put(info.getId(), info);
        // Prefer physical HDMI ports. CEC children must not duplicate their parent.
        for (TvInputInfo info : all) {
            if (info.getType() != TvInputInfo.TYPE_HDMI || !info.isPassthroughInput()
                    || info.isHidden(context)) continue;
            TvInputInfo parent = byId.get(info.getParentId());
            if (parent != null && parent.getType() == TvInputInfo.TYPE_HDMI) continue;
            String label = InputRules.label(info.loadCustomLabel(context), info.loadLabel(context));
            if (InputRules.brand(label) != InputRules.Brand.NONE) {
                entries.add(new InputEntry(info.getId(), label));
            }
        }
        entries.sort(Comparator.comparingInt((InputEntry e) -> e.brand.ordinal())
                .thenComparing(e -> e.label).thenComparing(e -> e.id));
        return entries;
    }
}
