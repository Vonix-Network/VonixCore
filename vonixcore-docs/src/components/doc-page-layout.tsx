import { ReactNode } from 'react';
import Link from 'next/link';
import { ArrowLeft, ArrowRight, Clock, FileText } from 'lucide-react';

interface DocPageLayoutProps {
    title: string;
    description?: string;
    children: ReactNode;
    prevPage?: { title: string; href: string };
    nextPage?: { title: string; href: string };
    lastUpdated?: string;
}

export function DocPageLayout({
    title,
    description,
    children,
    prevPage,
    nextPage,
    lastUpdated,
}: DocPageLayoutProps) {
    return (
        <div className="container max-w-4xl px-4 py-12 md:py-16">
            {/* Header */}
            <header className="mb-10">
                <div className="flex items-center gap-2 text-sm text-muted-foreground mb-3">
                    <FileText className="h-4 w-4" />
                    <span>Documentation</span>
                </div>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl mb-4 gradient-text">
                    {title}
                </h1>
                {description && (
                    <p className="text-lg text-muted-foreground max-w-2xl">
                        {description}
                    </p>
                )}
                {lastUpdated && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground mt-4">
                        <Clock className="h-4 w-4" />
                        <span>Last updated: {lastUpdated}</span>
                    </div>
                )}
            </header>

            {/* Content */}
            <article className="prose prose-invert max-w-none">
                {children}
            </article>

            {/* Navigation */}
            <footer className="mt-16 pt-8 border-t border-border">
                <div className="flex flex-col sm:flex-row justify-between gap-4">
                    {prevPage ? (
                        <Link
                            href={prevPage.href}
                            className="group flex items-center gap-3 p-4 rounded-xl glass-card hover-lift transition-all duration-300 flex-1"
                        >
                            <ArrowLeft className="h-5 w-5 text-muted-foreground group-hover:text-primary group-hover:-translate-x-1 transition-all" />
                            <div className="text-left">
                                <div className="text-sm text-muted-foreground">Previous</div>
                                <div className="font-medium text-foreground group-hover:text-primary transition-colors">
                                    {prevPage.title}
                                </div>
                            </div>
                        </Link>
                    ) : (
                        <div className="flex-1" />
                    )}
                    {nextPage ? (
                        <Link
                            href={nextPage.href}
                            className="group flex items-center justify-end gap-3 p-4 rounded-xl glass-card hover-lift transition-all duration-300 flex-1 text-right"
                        >
                            <div>
                                <div className="text-sm text-muted-foreground">Next</div>
                                <div className="font-medium text-foreground group-hover:text-primary transition-colors">
                                    {nextPage.title}
                                </div>
                            </div>
                            <ArrowRight className="h-5 w-5 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all" />
                        </Link>
                    ) : (
                        <div className="flex-1" />
                    )}
                </div>
            </footer>
        </div>
    );
}
