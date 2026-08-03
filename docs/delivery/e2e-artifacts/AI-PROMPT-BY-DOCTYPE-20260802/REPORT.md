# AI prompt-by-documentType smoke (2026-08-02)

## Mapping (docx → code)

| Docx section | `dict_document_type` |
|---|---|
| 1.赛后复盘提示词 | `POST_MATCH_REVIEW` |
| 2.预热前瞻提示词 | `PREHEAT_PREVIEW` |
| 3.新号引流提示词 | `NEW_ACCOUNT_TRAFFIC` |
| 短视频文案提示词 | `SHORT_VIDEO_SCRIPT` |
| 5.正式方案提示词 | `OFFICIAL_PLAN` |

## Verify

```powershell
# Seed (Flyway V168 on ops restart, or manual):
python docs/delivery/e2e-artifacts/AI-PROMPT-BY-DOCTYPE-20260802/_apply_v168.py

# Smoke two documentTypes → two promptIds
python docs/delivery/e2e-artifacts/AI-PROMPT-BY-DOCTYPE-20260802/smoke_generate.py
```

Expect `RESULTS.json` `ok=true`, distinct `promptId` / `promptTemplateName`.
Ops log: `AI content chat prompt resolved: ... documentType=..., promptId=...`.
