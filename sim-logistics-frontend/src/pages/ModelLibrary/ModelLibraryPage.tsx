/**
 * Model Library Page - 5.2 Model & Component Library Module
 *
 * Implements model management with:
 * - Model list (type, version, status, thumbnail)
 * - GLB file upload (via backend to MinIO)
 * - Model metadata form (type, version, dimensions, anchor)
 * - Model version management
 * - Device type to model mapping
 * - Model enable/disable
 * - Model transform parameters (scale/rotation/pivot)
 * - Entity-level model binding selector
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-11
 */
import React, { useEffect, useCallback, useRef, useState } from 'react'
import type { ModelSummary, ModelMetadata, ModelType, ModelDetail, ModelVersion } from '../../types'
import { useModelStore } from '@store/modelStore'
import { ErrorState, LoadingState, EmptyState } from '@components/ErrorState'
import { EntityModelSelector } from '@components/EntityModelSelector'
import { ModelPreview } from '@components/model/ModelPreview'
import { ModelViewerModal } from '@components/model/ModelViewerModal'
import { resolveModelAssetUrl } from '@/utils/modelAssetUrl'
import { t, tf } from '../../shared/i18n'

const MAX_MODEL_UPLOAD_SIZE_BYTES = 100 * 1024 * 1024
const MAX_MODEL_UPLOAD_SIZE_LABEL = '100MB'
type UploadApiError = { code?: string; status?: number; message?: string }

const getUploadErrorMessage = (error: unknown): string => {
  const maybeApiError = error as UploadApiError
  if (maybeApiError?.code === 'FILE_TOO_LARGE' || maybeApiError?.status === 413) {
    return tf('modelLibrary.upload.fileTooLargeServer', { maxSize: MAX_MODEL_UPLOAD_SIZE_LABEL })
  }
  if (maybeApiError?.code === 'BAD_REQUEST' || maybeApiError?.status === 400) {
    return t('modelLibrary.upload.badRequestTransform')
  }
  if (maybeApiError?.code === 'INTERNAL_ERROR' || maybeApiError?.message === 'Internal server error') {
    return t('toast.uploadFailed')
  }
  return maybeApiError?.message || t('toast.uploadFailed')
}

const getModelTypeLabel = (type: string): string => {
  const mapping: Record<string, string> = {
    OHT_VEHICLE: t('modelLibrary.type.ohtVehicle'),
    AGV_VEHICLE: t('modelLibrary.type.agvVehicle'),
    STOCKER: t('modelLibrary.type.stocker'),
    MACHINE: t('modelLibrary.type.machine'),
  }
  return mapping[type] ?? type
}

const getStatusDisplay = (status: string): string => {
  const mapping: Record<string, string> = {
    ACTIVE: t('modelLibrary.status.active'),
    DISABLED: t('modelLibrary.status.disabled'),
    PENDING: t('status.pending'),
  }
  return mapping[status] ?? status
}

/**
 * Status badge component
 */
const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const config = {
    ACTIVE: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
    DISABLED: 'bg-gray-500/20 text-gray-400 border-gray-500/30',
    PENDING: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
  }[status] || 'bg-gray-500/20 text-gray-400'

  return (
    <span className={`px-2 py-1 rounded border text-xs font-mono ${config}`}>
      {getStatusDisplay(status)}
    </span>
  )
}

/**
 * Model card component
 */
const ModelCard: React.FC<{
  model: ModelSummary
  onView: (model: ModelSummary) => void
  onEdit: (id: string) => void
  onToggleStatus: (id: string) => void
  onDelete: (id: string) => void
  onResolvePreviewUrl: (model: ModelSummary) => Promise<string | null>
}> = ({ model, onView, onEdit, onToggleStatus, onDelete, onResolvePreviewUrl }) => {
  const [showActions, setShowActions] = useState(false)
  const safeVersions = Array.isArray(model.versions) ? model.versions : []
  const versionCount = safeVersions.length

  return (
    <div
      className="group relative bg-gradient-to-br from-industrial-slate to-industrial-dark border border-industrial-steel/30 rounded-lg overflow-hidden transition-all duration-300 hover:border-amber-500/50"
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      <ModelPreview
        name={model.name}
        modelUrl={null}
        onResolveModelUrl={() => onResolvePreviewUrl(model)}
      />

      <div className="p-4">
        <div className="flex items-start justify-between mb-2">
          <h3 className="text-lg font-display font-bold text-white line-clamp-1">
            {model.name}
          </h3>
          <StatusBadge status={model.status} />
        </div>

        <div className="flex items-center gap-3 text-xs text-gray-500 font-mono mb-3">
          <span>{model.type ? getModelTypeLabel(model.type) : t('common.unknown')}</span>
          <span>v{model.defaultVersion ?? 'N/A'}</span>
          <span>{versionCount} {t('modelLibrary.versionsSuffix')}</span>
        </div>
      </div>

      {showActions && (
        <div className="absolute inset-0 bg-industrial-dark/95 backdrop-blur flex items-center justify-center gap-2 animate-fade-in">
          <button
            onClick={() => onView(model)}
            className="px-3 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors text-sm font-bold"
          >
            {t('common.view')}
          </button>
          <button
            onClick={() => onEdit(model.modelId)}
            className="px-3 py-2 bg-industrial-steel hover:bg-industrial-blue text-white rounded-lg transition-colors text-sm"
          >
            {t('common.edit')}
          </button>
          <button
            onClick={() => onToggleStatus(model.modelId)}
            className={`px-3 py-2 rounded-lg transition-colors text-sm ${
              model.status === 'ACTIVE'
                ? 'bg-gray-700 hover:bg-gray-600 text-white'
                : 'bg-emerald-600 hover:bg-emerald-500 text-white'
            }`}
          >
            {model.status === 'ACTIVE' ? t('common.disable') : t('common.enable')}
          </button>
          <button
            onClick={() => onDelete(model.modelId)}
            className="px-3 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg transition-colors text-sm"
          >
            {t('common.delete')}
          </button>
        </div>
      )}
    </div>
  )
}

/**
 * Upload dialog component
 */
const UploadDialog: React.FC<{
  isOpen: boolean
  onClose: () => void
  onUpload: (
    file: File,
    name: string,
    type: ModelType,
    metadata: ModelMetadata
  ) => Promise<void>
  isUploading: boolean
  uploadProgress: number
}> = ({ isOpen, onClose, onUpload, isUploading, uploadProgress }) => {
  const [file, setFile] = useState<File | null>(null)
  const [name, setName] = useState('')
  const [type, setType] = useState<ModelType>('MACHINE')
  const [version, setVersion] = useState('1.0.0')

  const [dimensions, setDimensions] = useState({ width: 1, height: 1, depth: 1 })
  const [anchorPoint, setAnchorPoint] = useState({ x: 0, y: 0, z: 0 })
  const [scale, setScale] = useState({ x: 1, y: 1, z: 1 })
  const [rotation, setRotation] = useState({ x: 0, y: 0, z: 0 })
  const [pivot, setPivot] = useState({ x: 0, y: 0, z: 0 })
  const [uploadError, setUploadError] = useState<string | null>(null)

  const modelTypes: ModelType[] = [
    'OHT_VEHICLE',
    'AGV_VEHICLE',
    'STOCKER',
    'ERACK',
    'MANUAL_STATION',
    'CONVEYOR',
    'OPERATOR',
    'MACHINE',
    'BAY',
    'CHUTE',
    'SAFETY_ZONE',
  ]

  const handleSubmit = async () => {
    if (!file || !name) return
    if (file.size > MAX_MODEL_UPLOAD_SIZE_BYTES) {
      setUploadError(tf('modelLibrary.upload.fileTooLarge', { maxSize: MAX_MODEL_UPLOAD_SIZE_LABEL }))
      return
    }

    try {
      setUploadError(null)
      const metadata: ModelMetadata = {
        type,
        version,
        dimensions,
        anchorPoint,
        transform: {
          scale,
          rotation,
          pivot,
        },
      }

      await onUpload(file, name, type, metadata)

      setTimeout(() => {
        onClose()
        setFile(null)
        setName('')
        setVersion('1.0.0')
        setDimensions({ width: 1, height: 1, depth: 1 })
        setAnchorPoint({ x: 0, y: 0, z: 0 })
        setScale({ x: 1, y: 1, z: 1 })
        setRotation({ x: 0, y: 0, z: 0 })
        setPivot({ x: 0, y: 0, z: 0 })
      }, 500)
    } catch (error) {
      setUploadError(getUploadErrorMessage(error))
      console.error(t('toast.uploadFailed'), error)
    }
  }

  if (!isOpen) return <></>

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur flex items-center justify-center z-50 overflow-y-auto">
      <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-6 w-full max-w-2xl my-8 animate-slide-up">
        <h2 className="text-xl font-display font-bold text-white mb-4">
          {t('modelLibrary.uploadDialogTitle')}
        </h2>

        {uploadError && (
          <div className="mb-4 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">
            {uploadError}
          </div>
        )}

        <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-2">
          <div>
            <label className="block text-gray-400 text-sm mb-2">
              {t('modelLibrary.glbFile')}
            </label>
            <input
              type="file"
              accept=".glb"
              onChange={(e) => {
                setFile(e.target.files?.[0] || null)
                setUploadError(null)
              }}
              disabled={isUploading}
              className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-2">
              {t('modelLibrary.modelName')}
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={isUploading}
              placeholder={t('modelLibrary.modelNamePlaceholder')}
              className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-2">
              {t('modelLibrary.version')}
            </label>
            <input
              type="text"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              disabled={isUploading}
              placeholder={t('modelLibrary.versionPlaceholder')}
              className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-2">
              {t('modelLibrary.modelType')}
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as ModelType)}
              disabled={isUploading}
              className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
            >
              {modelTypes.map((item) => (
                <option key={item} value={item}>
                  {getModelTypeLabel(item)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-2">
              {t('modelLibrary.dimensions')}
            </label>
            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="block text-gray-500 text-xs mb-1">{t('modelLibrary.width')}</label>
                <input
                  type="number"
                  step="0.01"
                  value={dimensions.width}
                  onChange={(e) => setDimensions({ ...dimensions, width: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                />
              </div>
              <div>
                <label className="block text-gray-500 text-xs mb-1">{t('modelLibrary.height')}</label>
                <input
                  type="number"
                  step="0.01"
                  value={dimensions.height}
                  onChange={(e) => setDimensions({ ...dimensions, height: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                />
              </div>
              <div>
                <label className="block text-gray-500 text-xs mb-1">{t('modelLibrary.depth')}</label>
                <input
                  type="number"
                  step="0.01"
                  value={dimensions.depth}
                  onChange={(e) => setDimensions({ ...dimensions, depth: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-2">
              {t('modelLibrary.anchorPoint')}
            </label>
            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="block text-gray-500 text-xs mb-1">{t('modelLibrary.axisX')}</label>
                <input
                  type="number"
                  step="0.01"
                  value={anchorPoint.x}
                  onChange={(e) => setAnchorPoint({ ...anchorPoint, x: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                />
              </div>
              <div>
                <label className="block text-gray-500 text-xs mb-1">{t('modelLibrary.axisY')}</label>
                <input
                  type="number"
                  step="0.01"
                  value={anchorPoint.y}
                  onChange={(e) => setAnchorPoint({ ...anchorPoint, y: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                />
              </div>
              <div>
                <label className="block text-gray-500 text-xs mb-1">{t('modelLibrary.axisZ')}</label>
                <input
                  type="number"
                  step="0.01"
                  value={anchorPoint.z}
                  onChange={(e) => setAnchorPoint({ ...anchorPoint, z: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                />
              </div>
            </div>
          </div>

          <div className="border-t border-industrial-steel/30 pt-4">
            <h3 className="text-white text-sm font-semibold mb-3">{t('modelLibrary.transformOptional')}</h3>

            <div className="mb-4">
              <label className="block text-gray-400 text-sm mb-2">
                {t('modelLibrary.scale')}
              </label>
              <div className="grid grid-cols-3 gap-2">
                <input
                  type="number"
                  step="0.1"
                  value={scale.x}
                  onChange={(e) => setScale({ ...scale, x: parseFloat(e.target.value) || 1 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisX')}
                />
                <input
                  type="number"
                  step="0.1"
                  value={scale.y}
                  onChange={(e) => setScale({ ...scale, y: parseFloat(e.target.value) || 1 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisY')}
                />
                <input
                  type="number"
                  step="0.1"
                  value={scale.z}
                  onChange={(e) => setScale({ ...scale, z: parseFloat(e.target.value) || 1 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisZ')}
                />
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-gray-400 text-sm mb-2">
                {t('modelLibrary.rotation')}
              </label>
              <div className="grid grid-cols-3 gap-2">
                <input
                  type="number"
                  step="0.1"
                  min={-Math.PI}
                  max={Math.PI}
                  value={rotation.x}
                  onChange={(e) => setRotation({ ...rotation, x: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisX')}
                />
                <input
                  type="number"
                  step="0.1"
                  min={-Math.PI}
                  max={Math.PI}
                  value={rotation.y}
                  onChange={(e) => setRotation({ ...rotation, y: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisY')}
                />
                <input
                  type="number"
                  step="0.1"
                  min={-Math.PI}
                  max={Math.PI}
                  value={rotation.z}
                  onChange={(e) => setRotation({ ...rotation, z: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisZ')}
                />
              </div>
              <p className="text-gray-500 text-xs mt-1">{t('common.range')}: {-Math.PI.toFixed(2)} to {Math.PI.toFixed(2)}</p>
            </div>

            <div>
              <label className="block text-gray-400 text-sm mb-2">
                {t('modelLibrary.pivotPoint')}
              </label>
              <div className="grid grid-cols-3 gap-2">
                <input
                  type="number"
                  step="0.01"
                  value={pivot.x}
                  onChange={(e) => setPivot({ ...pivot, x: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisX')}
                />
                <input
                  type="number"
                  step="0.01"
                  value={pivot.y}
                  onChange={(e) => setPivot({ ...pivot, y: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisY')}
                />
                <input
                  type="number"
                  step="0.01"
                  value={pivot.z}
                  onChange={(e) => setPivot({ ...pivot, z: parseFloat(e.target.value) || 0 })}
                  disabled={isUploading}
                  className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
                  placeholder={t('modelLibrary.axisZ')}
                />
              </div>
            </div>
          </div>

          {isUploading && (
            <div>
              <div className="flex justify-between text-xs text-gray-400 mb-1">
                <span>{t('modelLibrary.uploading')}</span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="w-full h-2 bg-industrial-dark rounded-full overflow-hidden">
                <div
                  className="h-full bg-amber-500 transition-all duration-300"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 mt-6">
          <button
            onClick={onClose}
            disabled={isUploading}
            className="px-4 py-2 text-gray-400 hover:text-white transition-colors disabled:opacity-50"
          >
            {t('common.cancel')}
          </button>
          <button
            onClick={handleSubmit}
            disabled={!file || !name || isUploading}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors font-bold disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {t('common.upload')}
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Model detail dialog component
 */
const ModelDetailDialog: React.FC<{
  isOpen: boolean
  onClose: () => void
  model: ModelDetail | null
  onUploadVersion: (id: string, file: File, metadata: ModelMetadata) => Promise<ModelVersion>
  onSetDefaultVersion: (id: string, versionId: string) => Promise<void>
  isUploading: boolean
  uploadProgress: number
}> = ({
  isOpen,
  onClose,
  model,
  onUploadVersion,
  onSetDefaultVersion,
  isUploading,
  uploadProgress,
}) => {
  const [showUploadVersion, setShowUploadVersion] = useState(false)
  const [versionFile, setVersionFile] = useState<File | null>(null)
  const [versionNumber, setVersionNumber] = useState('')
  const [versionUploadError, setVersionUploadError] = useState<string | null>(null)
  const safeVersions = Array.isArray(model?.versions) ? model.versions : []

  const handleUploadVersion = async () => {
    if (!model || !versionFile) return

    const metadata: ModelMetadata = {
      type: model.type,
      version: versionNumber,
      dimensions: { width: 1, height: 1, depth: 1 },
      anchorPoint: { x: 0, y: 0, z: 0 },
      transform: {
        scale: { x: 1, y: 1, z: 1 },
        rotation: { x: 0, y: 0, z: 0 },
        pivot: { x: 0, y: 0, z: 0 },
      },
    }

    try {
      setVersionUploadError(null)
      await onUploadVersion(model.modelId, versionFile, metadata)
      setShowUploadVersion(false)
      setVersionFile(null)
      setVersionNumber('')
    } catch (error) {
      setVersionUploadError(getUploadErrorMessage(error))
      console.error(t('toast.uploadFailed'), error)
    }
  }

  const handleSetDefault = async (versionId: string) => {
    if (!model) return
    await onSetDefaultVersion(model.modelId, versionId)
  }

  if (!isOpen || !model) return null

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur flex items-center justify-center z-50 overflow-y-auto">
      <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-6 w-full max-w-3xl my-8 animate-slide-up">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-xl font-display font-bold text-white">
              {model.name}
            </h2>
            <p className="text-gray-400 text-sm mt-1">{getModelTypeLabel(model.type)}</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white transition-colors"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="space-y-6 max-h-[70vh] overflow-y-auto pr-2">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-white text-sm font-semibold">{t('modelLibrary.versionSection')}</h3>
              <button
                onClick={() => setShowUploadVersion(true)}
                disabled={isUploading}
                className="px-3 py-1 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors text-sm font-bold disabled:opacity-50"
              >
                {t('modelLibrary.uploadNewVersion')}
              </button>
            </div>

            {showUploadVersion && (
              <div className="mb-4 p-4 bg-industrial-dark border border-industrial-steel/30 rounded-lg">
                <div className="space-y-3">
                  {versionUploadError && (
                    <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">
                      {versionUploadError}
                    </div>
                  )}
                  <div>
                    <label className="block text-gray-400 text-sm mb-1">{t('modelLibrary.versionNumber')}</label>
                    <input
                      type="text"
                      value={versionNumber}
                      onChange={(e) => {
                        setVersionNumber(e.target.value)
                        setVersionUploadError(null)
                      }}
                      placeholder={t('modelLibrary.versionNumberPlaceholder')}
                      className="w-full px-3 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-amber-500/50 transition-colors"
                    />
                  </div>
                  <div>
                    <label className="block text-gray-400 text-sm mb-1">{t('modelLibrary.glbFile')}</label>
                    <input
                      type="file"
                      accept=".glb"
                      onChange={(e) => {
                        setVersionFile(e.target.files?.[0] || null)
                        setVersionUploadError(null)
                      }}
                      className="w-full px-3 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors"
                    />
                  </div>
                  {isUploading && (
                    <div>
                      <div className="flex justify-between text-xs text-gray-400 mb-1">
                        <span>{t('modelLibrary.uploading')}</span>
                        <span>{uploadProgress}%</span>
                      </div>
                      <div className="w-full h-2 bg-industrial-slate rounded-full overflow-hidden">
                        <div
                          className="h-full bg-amber-500 transition-all duration-300"
                          style={{ width: `${uploadProgress}%` }}
                        />
                      </div>
                    </div>
                  )}
                  <div className="flex gap-2">
                    <button
                      onClick={handleUploadVersion}
                      disabled={!versionFile || !versionNumber || isUploading}
                      className="px-3 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors text-sm font-bold disabled:opacity-50"
                    >
                      {t('common.upload')}
                    </button>
                    <button
                      onClick={() => {
                        setShowUploadVersion(false)
                        setVersionFile(null)
                        setVersionNumber('')
                        setVersionUploadError(null)
                      }}
                      disabled={isUploading}
                      className="px-3 py-2 text-gray-400 hover:text-white transition-colors text-sm disabled:opacity-50"
                    >
                      {t('common.cancel')}
                    </button>
                  </div>
                </div>
              </div>
            )}

            <div className="space-y-2">
              {safeVersions.map((version) => (
                <div
                  key={version.versionId}
                  className="flex items-center justify-between p-3 bg-industrial-dark border border-industrial-steel/30 rounded-lg"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-white font-mono text-sm">v{version.version}</span>
                      {version.isDefault && (
                        <span className="px-2 py-0.5 bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded text-xs">
                          {t('common.default')}
                        </span>
                      )}
                      <StatusBadge status={version.status} />
                    </div>
                    <div className="text-gray-500 text-xs mt-1">
                      {(version.fileSize / 1024).toFixed(1)} KB • {new Date(version.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  {!version.isDefault && (
                    <button
                      onClick={() => handleSetDefault(version.versionId)}
                      className="px-3 py-1 bg-industrial-steel hover:bg-industrial-blue text-white rounded-lg transition-colors text-xs"
                    >
                      {t('modelLibrary.setDefault')}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="border-t border-industrial-steel/30 pt-4">
            <h3 className="text-white text-sm font-semibold mb-3">{t('modelLibrary.defaultTransformTitle')}</h3>
            <p className="text-gray-500 text-sm mb-4">
              {t('modelLibrary.defaultTransformHint')}
            </p>

            {(() => {
              const defaultVersion = safeVersions.find((v) => v.isDefault)
              const transform = defaultVersion?.metadata.transform
              if (!transform) return null

              return (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-industrial-dark border border-industrial-steel/30 rounded-lg">
                <div>
                  <label className="block text-gray-400 text-xs mb-1">{t('modelLibrary.scale')}</label>
                  <div className="font-mono text-white text-sm">
                    {transform.scale
                      ? `${transform.scale.x.toFixed(2)}, ${transform.scale.y.toFixed(2)}, ${transform.scale.z.toFixed(2)}`
                      : '1, 1, 1'}
                  </div>
                </div>

                <div>
                  <label className="block text-gray-400 text-xs mb-1">{t('modelLibrary.rotationRadians')}</label>
                  <div className="font-mono text-white text-sm">
                    {transform.rotation ? (
                      <span>
                        {transform.rotation.x.toFixed(2)}, {transform.rotation.y.toFixed(2)}, {transform.rotation.z.toFixed(2)}
                      </span>
                    ) : (
                      '0, 0, 0'
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-gray-400 text-xs mb-1">{t('modelLibrary.pivotMeters')}</label>
                  <div className="font-mono text-white text-sm">
                    {transform.pivot ? (
                      <span>
                        {transform.pivot.x.toFixed(2)}, {transform.pivot.y.toFixed(2)}, {transform.pivot.z.toFixed(2)}
                      </span>
                    ) : (
                      '0, 0, 0'
                    )}
                  </div>
                </div>
              </div>
              )
            })()}
          </div>
        </div>

        <div className="flex justify-end gap-3 mt-6">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
          >
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Model library page component
 */
const ModelLibraryPage: React.FC = () => {
  const {
    models,
    pagination,
    isLoading,
    error,
    searchQuery,
    filterType,
    filterStatus,
    isUploading,
    uploadProgress,
    currentModel,
    fetchModels,
    fetchModel,
    fetchModelVersions,
    uploadModel,
    uploadVersion,
    setDefaultVersion,
    enableModel,
    disableModel,
    deleteModel,
    setSearchQuery,
    setFilterType,
    setFilterStatus,
    clearError,
  } = useModelStore()

  const [showUploadDialog, setShowUploadDialog] = useState(false)
  const [showDetailDialog, setShowDetailDialog] = useState(false)
  const [showBindingPanel, setShowBindingPanel] = useState(false)
  const [showViewerModal, setShowViewerModal] = useState(false)
  const [viewerModelName, setViewerModelName] = useState('')
  const [viewerModelUrl, setViewerModelUrl] = useState<string | null>(null)
  const viewerRequestRef = useRef(0)
  const previewUrlCacheRef = useRef<Map<string, string | null>>(new Map())

  useEffect(() => {
    fetchModels()
  }, [fetchModels])

  useEffect(() => {
    previewUrlCacheRef.current.clear()
  }, [models])

  const resolvePreviewUrl = useCallback(
    async (model: ModelSummary): Promise<string | null> => {
      const cached = previewUrlCacheRef.current.get(model.modelId)
      if (cached !== undefined) {
        return cached
      }

      try {
        const versions = await fetchModelVersions(model.modelId)
        const selectedVersion = versions.find((item) => item.isDefault)
          ?? versions.find((item) => item.version === model.defaultVersion)
          ?? versions[0]
        const fileUrl = resolveModelAssetUrl(selectedVersion?.fileUrl)
        previewUrlCacheRef.current.set(model.modelId, fileUrl)
        return fileUrl
      } catch {
        previewUrlCacheRef.current.set(model.modelId, null)
        return null
      }
    },
    [fetchModelVersions]
  )

  const handleViewModel = useCallback(
    async (model: ModelSummary) => {
      const requestId = ++viewerRequestRef.current
      setViewerModelName(model.name)
      setViewerModelUrl(null)
      setShowViewerModal(true)

      try {
        const resolvedUrl = await resolvePreviewUrl(model)
        if (requestId !== viewerRequestRef.current) return
        setViewerModelUrl(resolvedUrl && resolvedUrl.trim() ? resolvedUrl : '')
      } catch {
        if (requestId !== viewerRequestRef.current) return
        setViewerModelUrl('')
      }
    },
    [resolvePreviewUrl]
  )

  const handleEditModel = useCallback(
    (id: string) => {
      fetchModel(id).then(() => {
        setShowDetailDialog(true)
      })
    },
    [fetchModel]
  )

  const handleCloseViewerModal = useCallback(() => {
    viewerRequestRef.current += 1
    setShowViewerModal(false)
    setViewerModelUrl(null)
    setViewerModelName('')
  }, [])

  const handleToggleStatus = useCallback(
    async (id: string) => {
      const model = models.find((m) => m.modelId === id)
      if (!model) return

      try {
        if (model.status === 'ACTIVE') {
          await disableModel(id)
        } else {
          await enableModel(id)
        }
      } catch (err) {
        console.error(t('toast.toggleStatusFailed'), err)
      }
    },
    [models, enableModel, disableModel]
  )

  const handleDelete = useCallback(
    async (id: string) => {
      const model = models.find((m) => m.modelId === id)
      if (!model) return

      if (confirm(tf('modelLibrary.deleteConfirm', { name: model.name }))) {
        try {
          await deleteModel(id)
        } catch (err) {
          console.error(t('toast.deleteFailed'), err)
        }
      }
    },
    [models, deleteModel]
  )

  const handleUpload = useCallback(
    async (file: File, name: string, type: ModelType, metadata: ModelMetadata) => {
      await uploadModel(file, name, type, metadata)
    },
    [uploadModel]
  )

  return (
    <div className="min-h-screen bg-gradient-to-br from-industrial-dark via-industrial-slate to-industrial-blue">
      <header className="border-b border-industrial-steel/30 bg-industrial-dark/50 backdrop-blur sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-display font-bold text-white tracking-tight">
                {t('modelLibrary.title')}
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                {t('modelLibrary.subtitle')}
              </p>
            </div>

            <button
              onClick={() => setShowUploadDialog(true)}
              className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors text-sm font-bold"
            >
              {t('modelLibrary.uploadModel')}
            </button>
          </div>

          <div className="flex items-center gap-4 mt-4">
            <div className="flex-1 max-w-md">
              <input
                type="text"
                placeholder={t('modelLibrary.searchPlaceholder')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-4 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-amber-500/50 transition-colors"
              />
            </div>

            <select
              value={filterType || ''}
              onChange={(e) => setFilterType(e.target.value || null)}
              className="px-4 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors"
            >
              <option value="">{t('modelLibrary.allTypes')}</option>
              <option value="OHT_VEHICLE">{t('modelLibrary.type.ohtVehicle')}</option>
              <option value="AGV_VEHICLE">{t('modelLibrary.type.agvVehicle')}</option>
              <option value="STOCKER">{t('modelLibrary.type.stocker')}</option>
              <option value="MACHINE">{t('modelLibrary.type.machine')}</option>
            </select>

            <select
              value={filterStatus || ''}
              onChange={(e) => setFilterStatus(e.target.value || null)}
              className="px-4 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors"
            >
              <option value="">{t('modelLibrary.allStatuses')}</option>
              <option value="ACTIVE">{t('modelLibrary.status.active')}</option>
              <option value="DISABLED">{t('modelLibrary.status.disabled')}</option>
            </select>

            <button
              data-testid="toggle-binding-panel"
              onClick={() => setShowBindingPanel((prev) => !prev)}
              className="px-4 py-2 bg-industrial-steel hover:bg-industrial-blue text-white rounded-lg transition-colors text-sm"
            >
              {showBindingPanel ? t('modelLibrary.bindingPanelHide') : t('modelLibrary.bindingPanelShow')}
            </button>
          </div>
        </div>
      </header>

      {error && (
        <div className="max-w-7xl mx-auto px-6 mt-6">
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 flex items-center justify-between">
            <span className="text-red-400 text-sm">{error}</span>
            <button
              onClick={clearError}
              className="text-gray-400 hover:text-white transition-colors"
            >
              ✕
            </button>
          </div>
        </div>
      )}

      <main className="max-w-7xl mx-auto px-6 py-8">
        {showBindingPanel && (
          <div className="mb-6 p-4 bg-industrial-slate/80 border border-industrial-steel/30 rounded-lg">
            <h2 className="text-white font-display text-lg mb-3">{t('modelLibrary.bindingPanelTitle')}</h2>
            <EntityModelSelector />
          </div>
        )}

        {isLoading ? (
          <LoadingState message={t('modelLibrary.loadingModels')} />
        ) : error ? (
          <ErrorState
            title={t('modelLibrary.loadFailedTitle')}
            message={error}
            onRetry={() => {
              clearError()
              fetchModels()
            }}
          />
        ) : models.length === 0 ? (
          <EmptyState
            title={t('modelLibrary.emptyTitle')}
            message={t('modelLibrary.emptyMessage')}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {models.map((model) => (
              <ModelCard
                key={model.modelId}
                model={model}
                onView={handleViewModel}
                onEdit={handleEditModel}
                onToggleStatus={handleToggleStatus}
                onDelete={handleDelete}
                onResolvePreviewUrl={resolvePreviewUrl}
              />
            ))}
          </div>
        )}

        {!isLoading && models.length > 0 && pagination.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-8">
            <button
              onClick={() => fetchModels(pagination.page - 1)}
              disabled={pagination.page <= 1}
              className="px-3 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
            >
              {t('common.previous')}
            </button>

            <span className="text-gray-400 text-sm">
              {t('common.page')} {pagination.page} {t('common.of')} {pagination.totalPages}
            </span>

            <button
              onClick={() => fetchModels(pagination.page + 1)}
              disabled={pagination.page >= pagination.totalPages}
              className="px-3 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
            >
              {t('common.next')}
            </button>
          </div>
        )}
      </main>

      <UploadDialog
        isOpen={showUploadDialog}
        onClose={() => setShowUploadDialog(false)}
        onUpload={handleUpload}
        isUploading={isUploading}
        uploadProgress={uploadProgress}
      />

      <ModelViewerModal
        open={showViewerModal}
        modelName={viewerModelName}
        modelUrl={viewerModelUrl}
        onClose={handleCloseViewerModal}
      />

      <ModelDetailDialog
        isOpen={showDetailDialog}
        onClose={() => {
          setShowDetailDialog(false)
        }}
        model={currentModel}
        onUploadVersion={uploadVersion}
        onSetDefaultVersion={setDefaultVersion}
        isUploading={isUploading}
        uploadProgress={uploadProgress}
      />
    </div>
  )
}

export default ModelLibraryPage
