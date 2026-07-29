/**
 * 字典查询 — Football 集成（D-DEDUP-01 / A-WP3）
 *
 * Football Admin DictDataController 无 GET /system/dict-data/type（仅有 simple-list/page）。
 * 浏览器 DictSelect 走 oa-server 薄封装 GET /admin-api/oa/dict/data?type=
 * （DictService → SystemDictAdapter → shenyu-system 字典表）。
 * 见 docs/delivery/FOOTBALL-OPS-BRANCH.md · Phase C 再评估 Admin simple-list。
 */
import { request } from '@/utils/request'

export interface DictItemVO {
  dictType: string
  label: string
  value: string
  sort: number
  status: string
}

export interface DictTypeVO {
  type: string
  name: string
  status: string
}

/** 按 type 拉取字典项（oa-server 薄封装，数据源为 Football system 字典）。 */
export function fetchDictDataViaOa(type: string) {
  return request.get<{ list: DictItemVO[]; total: number }>({
    url: '/oa/dict/data',
    params: { type },
  })
}

/** @deprecated 字典类型管理不下沉 OPS；请用 Football `#/dict` */
export function fetchDictTypes() {
  return request.get<{ list: DictTypeVO[]; total: number }>({
    url: '/oa/dict/types',
  })
}

/**
 * 按 type 拉取字典项。
 * 归一为 `{ list, total }`，供 DictSelect / DictLabel 使用。
 */
export async function fetchDictData(type: string): Promise<{ list: DictItemVO[]; total: number }> {
  const res = await fetchDictDataViaOa(type)
  const list = (res?.list ?? [])
    .map((item) => ({
      dictType: item.dictType || type,
      label: String(item.label ?? ''),
      value: String(item.value ?? ''),
      sort: Number(item.sort ?? 0),
      status: String(item.status ?? 'ENABLE'),
    }))
    .sort((a, b) => a.sort - b.sort)
  return { list, total: list.length }
}
