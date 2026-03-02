/**
 * Application entry point for sim-logistics-frontend.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { SceneListPage, SceneCreatePage, SceneEditorPage, ModelLibraryPage, SimulationLandingPage, SimulationViewerPage, KPIDashboardPage, TaskManagementPage } from './pages'
import { ToastContainer, AppNavBar } from './components'
import { useToastStore } from './store/toastStore'
import { useI18n } from './shared/i18n/useI18n'
import './index.css'
import '@google/model-viewer'

/**
 * Root application component with toast notifications
 */
const ComingSoonPage: React.FC<{ messageKey: 'page.simulationComingSoon' | 'page.configComingSoon' }> = ({ messageKey }) => {
  const { t } = useI18n()
  return <div className="p-8 text-white">{t(messageKey)}</div>
}

export const App: React.FC = () => {
  const { toasts, dismissToast } = useToastStore()

  return (
    <>
      <BrowserRouter>
        <div className="min-h-screen bg-gradient-to-br from-industrial-dark via-industrial-slate to-industrial-blue">
          <AppNavBar />
          <Routes>
            <Route path="/" element={<Navigate to="/scenes" replace />} />
            <Route path="/scenes" element={<SceneListPage />} />
            <Route path="/scenes/new" element={<SceneCreatePage />} />
            <Route path="/scenes/:id/edit" element={<SceneEditorPage />} />
            <Route path="/models" element={<ModelLibraryPage />} />
            <Route path="/simulation" element={<SimulationLandingPage />} />
            <Route path="/simulation/:id" element={<SimulationViewerPage />} />
            <Route path="/kpi" element={<KPIDashboardPage />} />
            <Route path="/tasks" element={<TaskManagementPage />} />
            <Route path="/config" element={<ComingSoonPage messageKey="page.configComingSoon" />} />
          </Routes>
        </div>
      </BrowserRouter>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </>
  )
}

// Mount application
const rootElement = document.getElementById('root')
if (rootElement) {
  ReactDOM.createRoot(rootElement).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  )
}
