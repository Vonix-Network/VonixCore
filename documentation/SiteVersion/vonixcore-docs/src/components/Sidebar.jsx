import React from 'react';
import { NavLink } from 'react-router-dom';
import { Shield, DollarSign, Command, Server, Key, MessageSquare, Skull, Home, X } from 'lucide-react';
import clsx from 'clsx';

const Sidebar = ({ isOpen, setIsOpen }) => {
    const links = [
        { name: 'Introduction', path: '/', icon: Home },
        { name: 'Configuration', path: '/configuration', icon: Server },
        { name: 'Commands', path: '/commands', icon: Command },
        { name: 'Permissions', path: '/permissions', icon: Key },
        { name: 'Protection', path: '/protection', icon: Shield },
        { name: 'Economy', path: '/economy', icon: DollarSign },
        { name: 'Authentication', path: '/authentication', icon: Key },
        { name: 'Discord', path: '/discord', icon: MessageSquare },
        { name: 'XP Sync', path: '/xpsync', icon: Server },
    ];

    return (
        <>
            {/* Mobile Overlay */}
            <div
                className={clsx(
                    "fixed inset-0 z-20 bg-black/50 backdrop-blur-sm lg:hidden transition-opacity",
                    isOpen ? "opacity-100" : "opacity-0 pointer-events-none"
                )}
                onClick={() => setIsOpen(false)}
            />

            {/* Sidebar */}
            <aside
                className={clsx(
                    "fixed top-0 left-0 bottom-0 z-30 w-72 bg-slate-950/90 backdrop-blur-md border-r border-slate-800 transition-transform duration-300 lg:translate-x-0 overflow-y-auto",
                    isOpen ? "translate-x-0" : "-translate-x-full"
                )}
                style={{ backgroundColor: 'var(--bg-secondary)', borderColor: 'var(--border-color)' }}
            >
                <div className="flex items-center justify-between h-16 px-6 border-b border-slate-800" style={{ borderColor: 'var(--border-color)' }}>
                    <span className="text-xl font-bold bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent" style={{ backgroundImage: 'linear-gradient(to right, var(--accent-primary), var(--accent-secondary))', color: 'transparent', WebkitBackgroundClip: 'text' }}>
                        VonixCore
                    </span>
                    <button onClick={() => setIsOpen(false)} className="lg:hidden text-slate-400">
                        <X size={24} />
                    </button>
                </div>

                <nav className="p-4 space-y-1">
                    {links.map((link) => (
                        <NavLink
                            key={link.path}
                            to={link.path}
                            onClick={() => setIsOpen(false)}
                            className={({ isActive }) => clsx(
                                "flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors",
                                isActive
                                    ? "bg-indigo-500/10 text-indigo-400 border border-indigo-500/20"
                                    : "text-slate-400 hover:text-slate-100 hover:bg-slate-900"
                            )}
                            style={({ isActive }) => isActive ? {
                                backgroundColor: 'var(--accent-glow)',
                                color: 'var(--accent-secondary)',
                                borderColor: 'var(--accent-glow)'
                            } : {}}
                        >
                            <link.icon size={18} />
                            {link.name}
                        </NavLink>
                    ))}
                </nav>

                <div className="p-4 mt-auto">
                    <div className="p-4 bg-slate-900 rounded-lg border border-slate-800" style={{ backgroundColor: 'var(--bg-tertiary)', borderColor: 'var(--border-color)' }}>
                        <p className="text-xs text-slate-500 text-center">Version 1.0.0</p>
                    </div>
                </div>
            </aside>
        </>
    );
};

export default Sidebar;
