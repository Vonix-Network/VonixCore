import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Menu } from 'lucide-react';
import Sidebar from './components/Sidebar';
import Documentation from './pages/Documentation';

function App() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  return (
    <Router>
      <div className="min-h-screen">
        <Sidebar isOpen={isSidebarOpen} setIsOpen={setIsSidebarOpen} />

        <main className="lg:pl-72 min-h-screen transition-all duration-300">
          <div
            className="sticky top-0 z-10 px-4 h-16 flex items-center lg:hidden backdrop-blur-md border-b"
            style={{
              backgroundColor: 'var(--glass-bg)',
              borderColor: 'var(--border-color)'
            }}
          >
            <button onClick={() => setIsSidebarOpen(true)} className="p-1" style={{ color: 'var(--text-secondary)' }}>
              <Menu size={24} />
            </button>
            <span className="ml-4 font-bold text-lg text-transparent bg-clip-text" style={{
              backgroundImage: 'linear-gradient(to right, var(--accent-primary), var(--accent-secondary))',
              WebkitBackgroundClip: 'text'
            }}>VonixCore</span>
          </div>

          <div className="p-6 md:p-10 lg:p-12">
            <Routes>
              <Route path="/" element={<Documentation file="introduction.md" />} />
              <Route path="/configuration" element={<Documentation file="configuration.md" />} />
              <Route path="/commands" element={<Documentation file="commands.md" />} />
              <Route path="/permissions" element={<Documentation file="permissions.md" />} />
              <Route path="/protection" element={<Documentation file="protection.md" />} />
              <Route path="/economy" element={<Documentation file="economy.md" />} />
              <Route path="/graves" element={<Documentation file="graves.md" />} />
              <Route path="/authentication" element={<Documentation file="authentication.md" />} />
              <Route path="/discord" element={<Documentation file="discord.md" />} />
              <Route path="/xpsync" element={<Documentation file="xpsync.md" />} />
              <Route path="*" element={<Navigate to="/" />} />
            </Routes>
          </div>
        </main>
      </div>
    </Router>
  );
}

export default App;
