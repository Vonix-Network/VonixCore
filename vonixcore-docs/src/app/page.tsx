import Link from 'next/link';
import {
  ArrowRight,
  Database,
  Shield,
  Home as HomeIcon,
  DollarSign,
  MessageCircle,
  RefreshCw,
  Terminal,
  Key,
  Download,
  Server,
  Blocks,
  Puzzle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';

const features = [
  {
    icon: Database,
    title: 'Flexible Database',
    description: 'Support for SQLite, MySQL, PostgreSQL, Turso, and Supabase with a single unified schema.',
    color: 'neon-cyan',
  },
  {
    icon: Shield,
    title: 'Protection System',
    description: 'CoreProtect-style block logging with rollback and lookup capabilities.',
    color: 'neon-purple',
  },
  {
    icon: HomeIcon,
    title: 'Homes & Warps',
    description: 'Personal homes for players and server-wide warps with configurable limits.',
    color: 'neon-pink',
  },
  {
    icon: DollarSign,
    title: 'Economy',
    description: 'Full economy system with baltop, transactions, and admin controls.',
    color: 'neon-orange',
  },
  {
    icon: MessageCircle,
    title: 'Discord Integration',
    description: 'Chat relay, account linking, and event notifications between Discord and Minecraft.',
    color: 'neon-blue',
  },
  {
    icon: Key,
    title: 'Permissions',
    description: 'Built-in permission system or LuckPerms integration with groups and inheritance.',
    color: 'neon-teal',
  },
];

const platforms = [
  { name: 'NeoForge', versions: '1.20.2 - 1.21.x', java: 'Java 21', icon: Blocks },
  { name: 'Paper', versions: '1.18.2 - 1.21.x', java: 'Java 17+', icon: Server },
  { name: 'Bukkit/Spigot', versions: '1.18.2 - 1.21.x', java: 'Java 17+', icon: Puzzle },
];

const quickLinks = [
  { title: 'Configuration Guide', href: '/docs/configuration', icon: Database },
  { title: 'Commands Reference', href: '/docs/commands', icon: Terminal },
  { title: 'Permissions', href: '/docs/permissions', icon: Key },
  { title: 'Discord Setup', href: '/docs/discord', icon: MessageCircle },
];

export default function HomePage() {
  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="relative overflow-hidden border-b border-border">
        {/* Background Effects */}
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5" />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-neon-glow opacity-30 blur-3xl" />

        <div className="container relative px-4 py-20 md:py-32">
          <div className="mx-auto max-w-4xl text-center">
            {/* Badge */}
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary mb-8 neon-border">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
              </span>
              Open Source • Multi-Platform • Production Ready
            </div>

            {/* Title */}
            <h1 className="text-4xl font-bold tracking-tight sm:text-6xl lg:text-7xl mb-6">
              <span className="gradient-text-animated">VonixCore</span>
              <br />
              <span className="text-foreground">Documentation</span>
            </h1>

            {/* Description */}
            <p className="mx-auto max-w-2xl text-lg text-muted-foreground mb-10">
              A comprehensive, all-in-one essentials mod/plugin that brings the beloved features of
              <span className="text-primary"> EssentialsX</span>,
              <span className="text-accent"> LuckPerms</span>,
              <span className="text-neon-pink"> CoreProtect</span>, and more into a single, highly optimized package.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Button size="lg" asChild className="group">
                <Link href="/docs/configuration">
                  Get Started
                  <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
                </Link>
              </Button>
              <Button size="lg" variant="outline" asChild>
                <a
                  href="https://github.com/Vonix-Network/VonixCore/releases"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Download className="mr-2 h-4 w-4" />
                  Download Latest
                </a>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Platforms Section */}
      <section className="border-b border-border bg-card/30">
        <div className="container px-4 py-12">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {platforms.map((platform) => {
              const Icon = platform.icon;
              return (
                <div
                  key={platform.name}
                  className="flex items-center gap-4 p-4 rounded-xl glass-card hover-lift"
                >
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-primary/10">
                    <Icon className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">{platform.name}</h3>
                    <p className="text-sm text-muted-foreground">
                      {platform.versions} • {platform.java}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="border-b border-border">
        <div className="container px-4 py-20">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold mb-4 gradient-text">Everything You Need</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              VonixCore combines the functionality of multiple plugins into one cohesive, optimized package.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <div
                  key={feature.title}
                  className="group relative p-6 rounded-xl glass-card hover-lift transition-all duration-300"
                >
                  <div className={`inline-flex items-center justify-center w-12 h-12 rounded-lg bg-primary/10 mb-4 group-hover:neon-glow-cyan transition-all duration-300`}>
                    <Icon className="h-6 w-6 text-primary" />
                  </div>
                  <h3 className="text-lg font-semibold mb-2 text-foreground group-hover:text-primary transition-colors">
                    {feature.title}
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    {feature.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Quick Links Section */}
      <section className="border-b border-border bg-card/30">
        <div className="container px-4 py-20">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold mb-4 gradient-text">Quick Start</h2>
            <p className="text-muted-foreground">
              Jump right into the documentation with these popular topics.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 max-w-4xl mx-auto">
            {quickLinks.map((link) => {
              const Icon = link.icon;
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className="group flex items-center gap-3 p-4 rounded-xl glass-card hover-lift transition-all duration-300"
                >
                  <Icon className="h-5 w-5 text-primary group-hover:scale-110 transition-transform" />
                  <span className="font-medium text-foreground group-hover:text-primary transition-colors">
                    {link.title}
                  </span>
                  <ArrowRight className="ml-auto h-4 w-4 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all" />
                </Link>
              );
            })}
          </div>
        </div>
      </section>

      {/* Installation Section */}
      <section className="border-b border-border">
        <div className="container px-4 py-20">
          <div className="max-w-3xl mx-auto">
            <div className="text-center mb-12">
              <h2 className="text-3xl font-bold mb-4 gradient-text">Quick Installation</h2>
              <p className="text-muted-foreground">
                Get VonixCore running on your server in minutes.
              </p>
            </div>

            <div className="space-y-6">
              {/* NeoForge */}
              <div className="p-6 rounded-xl glass-card">
                <h3 className="font-semibold text-lg text-primary mb-3 flex items-center gap-2">
                  <Blocks className="h-5 w-5" />
                  NeoForge
                </h3>
                <ol className="space-y-2 text-muted-foreground">
                  <li className="flex items-start gap-3">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/20 text-primary text-sm flex items-center justify-center">1</span>
                    <span>Download <code className="text-primary">VonixCore-NeoForge-x.x.x.jar</code> from Releases</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/20 text-primary text-sm flex items-center justify-center">2</span>
                    <span>Place in your server&apos;s <code className="text-primary">mods/</code> folder</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/20 text-primary text-sm flex items-center justify-center">3</span>
                    <span>Start server to generate configs in <code className="text-primary">config/vonixcore-*.toml</code></span>
                  </li>
                </ol>
              </div>

              {/* Paper / Bukkit */}
              <div className="p-6 rounded-xl glass-card">
                <h3 className="font-semibold text-lg text-primary mb-3 flex items-center gap-2">
                  <Server className="h-5 w-5" />
                  Paper / Bukkit
                </h3>
                <ol className="space-y-2 text-muted-foreground">
                  <li className="flex items-start gap-3">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/20 text-primary text-sm flex items-center justify-center">1</span>
                    <span>Download <code className="text-primary">VonixCore-Paper-x.x.x.jar</code> or <code className="text-primary">VonixCore-Bukkit-x.x.x.jar</code></span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/20 text-primary text-sm flex items-center justify-center">2</span>
                    <span>Place in your server&apos;s <code className="text-primary">plugins/</code> folder</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/20 text-primary text-sm flex items-center justify-center">3</span>
                    <span>Start server to generate configs in <code className="text-primary">plugins/VonixCore/config.yml</code></span>
                  </li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-card/50">
        <div className="container px-4 py-12">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-3">
              <div className="relative h-8 w-8 flex items-center justify-center">
                <div className="absolute inset-0 bg-neon-rainbow rounded-lg opacity-20" />
                <span className="relative font-bold text-lg gradient-text">V</span>
              </div>
              <div className="text-sm text-muted-foreground">
                Made with ❤️ by the <span className="text-primary">Vonix Network</span> Team
              </div>
            </div>
            <div className="flex items-center gap-6 text-sm text-muted-foreground">
              <a href="https://github.com/Vonix-Network/VonixCore" target="_blank" rel="noopener noreferrer" className="hover:text-primary transition-colors">
                GitHub
              </a>
              <a href="https://discord.gg/vonix" target="_blank" rel="noopener noreferrer" className="hover:text-primary transition-colors">
                Discord
              </a>
              <a href="https://vonix.network" target="_blank" rel="noopener noreferrer" className="hover:text-primary transition-colors">
                Vonix Network
              </a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
