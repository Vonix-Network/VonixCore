'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import {
    Home,
    Book,
    Terminal,
    Shield,
    Key,
    MessageCircle,
    HardDrive,
    DollarSign,
    RefreshCw,
    Github,
    ExternalLink,
} from 'lucide-react';

const sidebarLinks = [
    {
        title: 'Getting Started',
        items: [
            { title: 'Introduction', href: '/', icon: Home },
            { title: 'Configuration', href: '/docs/configuration', icon: HardDrive },
        ],
    },
    {
        title: 'Features',
        items: [
            { title: 'Commands', href: '/docs/commands', icon: Terminal },
            { title: 'Permissions', href: '/docs/permissions', icon: Key },
            { title: 'Protection', href: '/docs/protection', icon: Shield },
            { title: 'Economy', href: '/docs/economy', icon: DollarSign },
        ],
    },
    {
        title: 'Integrations',
        items: [
            { title: 'Discord', href: '/docs/discord', icon: MessageCircle },
            { title: 'Authentication', href: '/docs/authentication', icon: Shield },
            { title: 'XP Sync', href: '/docs/xpsync', icon: RefreshCw },
        ],
    },
];

export function Sidebar() {
    const pathname = usePathname();

    return (
        <aside className="fixed left-0 top-16 z-30 hidden h-[calc(100vh-4rem)] w-72 shrink-0 overflow-y-auto border-r border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 md:sticky md:block">
            <nav className="space-y-6 p-6" aria-label="Sidebar">
                {sidebarLinks.map((section) => (
                    <div key={section.title}>
                        <h4 className="mb-3 text-sm font-semibold tracking-wider text-muted-foreground uppercase">
                            {section.title}
                        </h4>
                        <ul className="space-y-1">
                            {section.items.map((item) => {
                                const Icon = item.icon;
                                const isActive = pathname === item.href;
                                return (
                                    <li key={item.href}>
                                        <Link
                                            href={item.href}
                                            className={cn(
                                                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all duration-200',
                                                isActive
                                                    ? 'bg-primary/10 text-primary neon-glow-cyan'
                                                    : 'text-muted-foreground hover:bg-accent/50 hover:text-foreground'
                                            )}
                                        >
                                            <Icon className={cn('h-4 w-4', isActive && 'text-primary')} />
                                            {item.title}
                                        </Link>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                ))}

                {/* External Links */}
                <div className="border-t border-border pt-6">
                    <h4 className="mb-3 text-sm font-semibold tracking-wider text-muted-foreground uppercase">
                        Resources
                    </h4>
                    <ul className="space-y-1">
                        <li>
                            <a
                                href="https://github.com/Vonix-Network/VonixCore"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-muted-foreground transition-all duration-200 hover:bg-accent/50 hover:text-foreground"
                            >
                                <Github className="h-4 w-4" />
                                GitHub
                                <ExternalLink className="ml-auto h-3 w-3" />
                            </a>
                        </li>
                        <li>
                            <a
                                href="https://vonix.network"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-muted-foreground transition-all duration-200 hover:bg-accent/50 hover:text-foreground"
                            >
                                <Book className="h-4 w-4" />
                                Vonix Network
                                <ExternalLink className="ml-auto h-3 w-3" />
                            </a>
                        </li>
                    </ul>
                </div>
            </nav>
        </aside>
    );
}
