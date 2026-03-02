/**
 * Vite environment types for sim-logistics-frontend.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
/// <reference types="vite/client" />

declare module '*.css' {
  const content: Record<string, string>
  export default content
}

declare module '*.glb' {
  const content: THREE.BufferGeometry
  export default content
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
