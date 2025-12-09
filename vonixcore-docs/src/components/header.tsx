'use client';

import Link from 'next/link';
import { useState } from 'react';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
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
    Menu,
    X,
} from 'lucide-react';

const navLinks = [
    { title: 'Home', href: '/', icon: Home },
    { title: 'Configuration', href: '/docs/configuration', icon: HardDrive },
    { title: 'Commands', href: '/docs/commands', icon: Terminal },
    { title: 'Permissions', href: '/docs/permissions', icon: Key },
];

const mobileLinks = [
    { title: 'Home', href: '/', icon: Home },
    { title: 'Configuration', href: '/docs/configuration', icon: HardDrive },
    { title: 'Commands', href: '/docs/commands', icon: Terminal },
    { title: 'Permissions', href: '/docs/permissions', icon: Key },
    { title: 'Protection', href: '/docs/protection', icon: Shield },
    { title: 'Economy', href: '/docs/economy', icon: DollarSign },
    { title: 'Discord', href: '/docs/discord', icon: MessageCircle },
    { title: 'Authentication', href: '/docs/authentication', icon: Shield },
    { title: 'XP Sync', href: '/docs/xpsync', icon: RefreshCw },
];

export function Header() {
    const pathname = usePathname();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    return (
        <header className="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
            <div className="container flex h-16 items-center justify-between px-4">
                {/* Logo */}
                <Link href="/" className="flex items-center space-x-3 group">
                    <div className="relative h-10 w-10 flex items-center justify-center">
                        <div className="absolute inset-0 bg-neon-rainbow rounded-lg opacity-20 group-hover:opacity-40 transition-opacity" />
                        <span className="relative font-bold text-xl gradient-text">V</span>
                    </div>
                    <div className="flex flex-col">
                        <span className="font-bold text-lg gradient-text">VonixCore</span>
                        <span className="text-xs text-muted-foreground -mt-1">Documentation</span>
                    </div>
                </Link>

                {/* Desktop Navigation */}
                <nav className="hidden md:flex items-center space-x-1">
                    {navLinks.map((link) => {
                        const Icon = link.icon;
                        const isActive = pathname === link.href;
                        return (
                            <Link
                                key={link.href}
                                href={link.href}
                                className={cn(
                                    'flex items-center gap-2 px-3 py-2 text-sm rounded-lg transition-all duration-200',
                                    isActive
                                        ? 'bg-primary/10 text-primary'
                                        : 'text-muted-foreground hover:text-foreground hover:bg-accent/50'
                                )}
                            >
                                <Icon className="h-4 w-4" />
                                {link.title}
                            </Link>
                        );
                    })}
                </nav>

                {/* GitHub + Mobile Menu */}
                <div className="flex items-center gap-2">
                    <Button variant="ghost" size="icon" asChild className="hidden sm:flex">
                        <a
                            href="https://github.com/Vonix-Network/VonixCore"
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            <Github className="h-5 w-5" />
                            <span className="sr-only">GitHub</span>
                        </a>
                    </Button>

                    {/* Mobile Menu Button */}
                    <Button
                        variant="ghost"
                        size="icon"
                        className="md:hidden"
                        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                    >
                        {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
                        <span className="sr-only">Toggle menu</span>
                    </Button>
                </div>
            </div>

            {/* Mobile Menu */}
            {mobileMenuOpen && (
                <div className="md:hidden border-t border-border bg-background/95 backdrop-blur">
                    <nav className="container px-4 py-4 space-y-1">
                        {mobileLinks.map((link) => {
                            const Icon = link.icon;
                            const isActive = pathname === link.href;
                            return (
                                <Link
                                    key={link.href}
                                    href={link.href}
                                    onClick={() => setMobileMenuOpen(false)}
                                    className={cn(
                                        'flex items-center gap-3 px-3 py-2 text-sm rounded-lg transition-all duration-200',
                                        isActive
                                            ? 'bg-primary/10 text-primary'
                                            : 'text-muted-foreground hover:text-foreground hover:bg-accent/50'
                                    )}
                                >
                                    <Icon className="h-4 w-4" />
                                    {link.title}
                                </Link>
                            );
                        })}
                        <div className="border-t border-border pt-3 mt-3">
                            <a
                                href="https://github.com/Vonix-Network/VonixCore"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center gap-3 px-3 py-2 text-sm text-muted-foreground rounded-lg transition-all duration-200 hover:text-foreground hover:bg-accent/50"
                            >
                                <Github className="h-4 w-4" />
                                GitHub
                            </a>
                        </div>
                    </nav>
                </div>
            )}
        </header>
    );
}
