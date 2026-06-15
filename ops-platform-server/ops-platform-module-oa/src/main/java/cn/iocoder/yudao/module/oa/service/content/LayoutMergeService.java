package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.util.LayoutJsonHelper;
import cn.iocoder.yudao.module.oa.util.LayoutSchemaHelper;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge content body into layout schema → layout_json v2 instance (ADR-020).
 */
@Service
public class LayoutMergeService {

    @Getter
    public static class MergeResult {
        private final JSONObject layoutJson;
        private final String layoutHtml;
        private final int overflowSegmentCount;

        public MergeResult(JSONObject layoutJson, String layoutHtml, int overflowSegmentCount) {
            this.layoutJson = layoutJson;
            this.layoutHtml = layoutHtml;
            this.overflowSegmentCount = overflowSegmentCount;
        }
    }

    public MergeResult merge(String body, Object existingLayout, Object layoutSchema) {
        String effectiveBody = body;
        if (existingLayout != null && (effectiveBody == null || effectiveBody.isBlank())) {
            effectiveBody = LayoutSchemaHelper.extractTextFromLayout(existingLayout);
        }
        List<String> segments = new ArrayList<>(LayoutSchemaHelper.splitBody(effectiveBody));
        JSONObject schema = JSONUtil.parseObj(JSONUtil.toJsonStr(layoutSchema));
        JSONObject globalStyles = schema.getJSONObject("globalStyles");
        JSONArray schemaBlocks = schema.getJSONArray("blocks");
        JSONArray out = new JSONArray();
        int segIdx = 0;

        if (schemaBlocks != null) {
            for (int i = 0; i < schemaBlocks.size(); i++) {
                JSONObject block = schemaBlocks.getJSONObject(i);
                if (block == null) {
                    continue;
                }
                String type = block.getStr("type");
                String styleRef = block.getStr("styleRef", "paragraph");
                JSONObject styles = LayoutSchemaHelper.resolveStyles(globalStyles, styleRef);

                switch (type) {
                    case "divider" -> out.add(LayoutSchemaHelper.dividerInstance(styles));
                    case "fixed" -> out.add(LayoutSchemaHelper.fixedInstance(block.getStr("fixedType", "decor"), styles));
                    case "frame" -> {
                        // optional image frame skipped in v2.0 unless content has URL (P1)
                    }
                    case "heading" -> {
                        if (segIdx < segments.size()) {
                            int level = block.getInt("level", 2);
                            out.add(LayoutSchemaHelper.headingInstance(level, segments.get(segIdx), styles));
                            segIdx++;
                        }
                    }
                    case "slot" -> segIdx = consumeSlot(out, block, globalStyles, segments, segIdx,
                            schemaBlocks, i);
                    case "section" -> {
                        int maxRepeat = block.getInt("maxRepeat", 10);
                        JSONArray children = block.getJSONArray("children");
                        int repeats = 0;
                        while (segIdx < segments.size() && repeats < maxRepeat) {
                            int before = segIdx;
                            if (children != null) {
                                for (int c = 0; c < children.size() && segIdx < segments.size(); c++) {
                                    segIdx = consumeSlot(out, children.getJSONObject(c), globalStyles, segments, segIdx,
                                            schemaBlocks, i);
                                }
                            }
                            if (segIdx == before) {
                                break;
                            }
                            repeats++;
                        }
                    }
                    default -> {
                    }
                }
            }
        }

        int overflow = 0;
        JSONObject defaultParaStyles = LayoutSchemaHelper.resolveStyles(globalStyles, "paragraph");
        while (segIdx < segments.size()) {
            out.add(LayoutSchemaHelper.paragraphInstance(segments.get(segIdx), defaultParaStyles, "left"));
            segIdx++;
            overflow++;
        }

        JSONObject doc = new JSONObject();
        doc.set("version", 2);
        doc.set("blocks", out);
        if (overflow > 0) {
            doc.set("overflowSegmentCount", overflow);
        }
        String html = LayoutJsonHelper.renderHtml(doc, false);
        return new MergeResult(doc, html, overflow);
    }

    private int consumeSlot(JSONArray out, JSONObject block, JSONObject globalStyles,
                            List<String> segments, int segIdx, JSONArray schemaBlocks, int blockIndex) {
        if (block == null) {
            return segIdx;
        }
        String slotKind = block.getStr("slotKind", "paragraph");
        String styleRef = block.getStr("styleRef", "paragraph");
        JSONObject styles = LayoutSchemaHelper.resolveStyles(globalStyles, styleRef);
        boolean repeat = block.getBool("repeat", false);
        String align = block.getStr("align", "left");
        Integer maxRepeat = block.getInt("maxRepeat");

        if ("paragraph".equals(slotKind) && repeat) {
            int reserved = countReservedSegments(schemaBlocks, blockIndex, segments.size() - segIdx);
            int available = segments.size() - segIdx;
            int limit = maxRepeat != null ? maxRepeat : Math.max(0, available - reserved);
            if (reserved == 0) {
                limit = available;
            }
            int used = 0;
            while (segIdx < segments.size() && used < limit) {
                out.add(LayoutSchemaHelper.paragraphInstance(segments.get(segIdx), styles, align));
                segIdx++;
                used++;
            }
            return segIdx;
        }

        if (segIdx >= segments.size()) {
            return segIdx;
        }

        switch (slotKind) {
            case "heading" -> {
                out.add(LayoutSchemaHelper.headingInstance(block.getInt("level", 3), segments.get(segIdx), styles));
                segIdx++;
            }
            case "quote" -> {
                out.add(LayoutSchemaHelper.quoteInstance(segments.get(segIdx), styles));
                segIdx++;
            }
            case "paragraph" -> {
                out.add(LayoutSchemaHelper.paragraphInstance(segments.get(segIdx), styles, align));
                segIdx++;
            }
            case "list" -> {
                JSONObject list = new JSONObject();
                list.set("type", "list");
                list.set("ordered", block.getBool("ordered", false));
                list.set("items", List.of(segments.get(segIdx)));
                list.set("styles", styles);
                out.add(list);
                segIdx++;
            }
            default -> {
            }
        }
        return segIdx;
    }

    private int countReservedSegments(JSONArray schemaBlocks, int fromIndex, int remaining) {
        int reserved = 0;
        for (int j = fromIndex + 1; j < schemaBlocks.size() && reserved < remaining; j++) {
            JSONObject b = schemaBlocks.getJSONObject(j);
            if (b == null) {
                continue;
            }
            String type = b.getStr("type");
            if ("divider".equals(type) || "fixed".equals(type) || "frame".equals(type)) {
                continue;
            }
            if ("heading".equals(type)) {
                reserved++;
                continue;
            }
            if ("slot".equals(type)) {
                if (b.getBool("repeat", false) && "paragraph".equals(b.getStr("slotKind"))) {
                    Integer max = b.getInt("maxRepeat");
                    reserved += max != null ? max : 1;
                } else {
                    reserved++;
                }
            }
        }
        return reserved;
    }
}
