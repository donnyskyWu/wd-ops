export interface LayoutBlock {

  type: 'heading' | 'paragraph' | 'image' | 'quote' | 'divider' | 'list' | 'fixed'

  level?: number

  text?: string

  align?: string

  src?: string

  width?: string

  ordered?: boolean

  items?: string[]

  children?: Array<{ text: string; bold?: boolean; italic?: boolean }>

  styles?: Record<string, string>

}



export interface LayoutDocument {

  version: number

  blocks: LayoutBlock[]

  overflowSegmentCount?: number

}



export interface LayoutSchemaBlock {

  type: 'heading' | 'slot' | 'divider' | 'frame' | 'fixed' | 'section'

  level?: number

  slotKind?: 'heading' | 'paragraph' | 'quote' | 'list' | 'image'

  styleRef?: string

  repeat?: boolean

  optional?: boolean

  maxRepeat?: number

  align?: string

  ordered?: boolean

  fixedType?: string

  children?: LayoutSchemaBlock[]

}



export interface LayoutSchema {

  version: number

  globalStyles: Record<string, Record<string, string>>

  blocks: LayoutSchemaBlock[]

}



export interface LayoutTemplateVO {

  id: number

  templateName: string

  contentType: string

  documentType?: string | null

  description?: string

  sourceType: string

  sourceUrl?: string

  status: string

  thumbnailUrl?: string

  previewHtml?: string

  creatorUserId: number

  creatorName?: string

  updateTime?: string

}



export interface LayoutTemplateDetailVO extends LayoutTemplateVO {

  layoutJson?: LayoutDocument

  layoutSchema: LayoutSchema

  schemaVersion?: number

  layoutHtml: string

  previewHtml?: string

}



export interface LayoutTemplateSelectVO {

  id: number

  templateName: string

  documentType?: string | null

  thumbnailUrl?: string

  sourceType?: string

}



export interface LayoutTemplateForm {

  templateName: string

  description?: string

  documentType?: string | null

  layoutSchema: LayoutSchema

  status: string

  previewHtml?: string

  layoutHtml?: string

  styleCss?: string

  sourceType?: string

  sourceUrl?: string

}



export interface LayoutImportJobVO {

  id: number

  status: string

  sourceType: string

  sourceUrl?: string

  previewLayoutJson?: LayoutDocument

  previewLayoutSchema?: LayoutSchema

  extractionReport?: {
    strippedCharCount?: number
    slotCount?: number
    fixedBlockCount?: number
    warnings?: string[]
    fidelityNotes?: string[]
    structurePreserved?: boolean
    previewMode?: string
    inlineStyleCount?: number
  }

  previewHtml?: string
  styleCss?: string

  suggestedName?: string

  errorMessage?: string

}



export interface LayoutMergePreviewVO {

  layoutJson: LayoutDocument

  layoutHtml: string

  overflowSegmentCount?: number

  extractionReport?: LayoutImportJobVO['extractionReport']

}



export function emptyLayoutDocument(): LayoutDocument {

  return { version: 1, blocks: [] }

}



export function emptyLayoutSchema(): LayoutSchema {

  return {

    version: 2,

    globalStyles: {

      heading2: { fontSize: '18px', fontWeight: 'bold', color: '#1a1a1a', lineHeight: '1.4' },

      paragraph: { fontSize: '16px', color: '#333333', lineHeight: '1.75' },

      quote: {

        fontSize: '15px',

        color: '#666666',

        backgroundColor: '#f7f7f7',

        borderLeft: '4px solid #07c160',

        padding: '12px 16px'

      },

      divider: { borderColor: '#e5e5e5', margin: '24px 0' },

      image: { width: '100%', borderRadius: '4px' }

    },

    blocks: []

  }

}

